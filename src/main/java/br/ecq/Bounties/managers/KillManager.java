package br.ecq.Bounties.managers;

import br.ecq.Bounties.BountiesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KillManager {

    private final BountiesPlugin plugin;
    private final File dataFile;
    private final Map<UUID, Integer> kills = new ConcurrentHashMap<>();
    private UUID currentTopKiller;

    public KillManager(BountiesPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "kills.yml");
    }

    public void load() {
        kills.clear();
        currentTopKiller = null;

        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        if (!data.isConfigurationSection("kills")) {
            return;
        }

        for (String key : data.getConfigurationSection("kills").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int amount = data.getInt("kills." + key, 0);
                if (amount > 0) {
                    kills.put(uuid, amount);
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("UUID invalido em kills.yml: " + key);
            }
        }

        currentTopKiller = resolveTopKiller();
    }

    public void save() {
        FileConfiguration data = new YamlConfiguration();

        for (Map.Entry<UUID, Integer> entry : kills.entrySet()) {
            data.set("kills." + entry.getKey().toString(), entry.getValue());
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar kills.yml: " + e.getMessage());
        }
    }

    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    public void addKill(UUID killerUuid) {
        kills.put(killerUuid, getKills(killerUuid) + 1);
    }

    public UUID getTopKiller() {
        return currentTopKiller;
    }

    public boolean isTopKiller(UUID uuid) {
        return uuid != null && uuid.equals(currentTopKiller);
    }

    public UUID updateTopKiller() {
        UUID previous = currentTopKiller;
        currentTopKiller = resolveTopKiller();

        if (currentTopKiller != null && !currentTopKiller.equals(previous)) {
            return currentTopKiller;
        }
        return null;
    }

    public List<Map.Entry<UUID, Integer>> getTopKills(int limit) {
        List<Map.Entry<UUID, Integer>> top = new ArrayList<>(kills.entrySet());
        top.sort(Comparator
                .comparingInt((Map.Entry<UUID, Integer> e) -> e.getValue()).reversed()
                .thenComparing(e -> getPlayerName(e.getKey())));
        if (limit > 0 && top.size() > limit) {
            return new ArrayList<>(top.subList(0, limit));
        }
        return top;
    }

    public int getRank(UUID uuid) {
        List<Map.Entry<UUID, Integer>> top = getTopKills(0);
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).getKey().equals(uuid)) {
                return i + 1;
            }
        }
        return 0;
    }

    private UUID resolveTopKiller() {
        List<Map.Entry<UUID, Integer>> top = getTopKills(1);
        if (top.isEmpty()) {
            return null;
        }

        Map.Entry<UUID, Integer> first = top.get(0);
        if (first.getValue() < plugin.getConfigManager().getMinKillsForTop()) {
            return null;
        }

        return first.getKey();
    }

    private String getPlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        return name != null ? name : uuid.toString();
    }
}