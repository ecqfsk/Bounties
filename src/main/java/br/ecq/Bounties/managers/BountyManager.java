package br.ecq.Bounties.managers;

import br.ecq.Bounties.BountiesPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyManager {

    private final BountiesPlugin plugin;
    private final Map<UUID, Double> bounties = new LinkedHashMap<>();
    private File dataFile;

    public BountyManager(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "bounties.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nao foi possivel criar bounties.yml: " + e.getMessage());
            }
        }

        bounties.clear();
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        if (data.getConfigurationSection("bounties") != null) {
            for (String key : data.getConfigurationSection("bounties").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    double amount = data.getDouble("bounties." + key);
                    if (amount > 0) {
                        bounties.put(uuid, amount);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save() {
        if (dataFile == null) {
            dataFile = new File(plugin.getDataFolder(), "bounties.yml");
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        data.set("bounties", null);
        for (Map.Entry<UUID, Double> entry : bounties.entrySet()) {
            data.set("bounties." + entry.getKey().toString(), entry.getValue());
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nao foi possivel salvar bounties.yml: " + e.getMessage());
        }
    }

    public double getBounty(UUID uuid) {
        Double amount = bounties.get(uuid);
        return amount != null ? amount : 0;
    }

    public boolean hasBounty(UUID uuid) {
        return bounties.containsKey(uuid) && bounties.get(uuid) > 0;
    }

    public void setBounty(UUID uuid, double amount) {
        if (amount <= 0) {
            bounties.remove(uuid);
        } else {
            bounties.put(uuid, amount);
        }
    }

    public double addBounty(UUID uuid, double amount) {
        double current = getBounty(uuid);
        double total = current + amount;
        setBounty(uuid, total);
        return total;
    }

    public double removeBounty(UUID uuid) {
        double amount = getBounty(uuid);
        bounties.remove(uuid);
        return amount;
    }

    public Map<UUID, Double> getAllBounties() {
        return Collections.unmodifiableMap(bounties);
    }

    public int getTotalBounties() {
        return bounties.size();
    }

    public double getTotalAmount() {
        double total = 0;
        for (double amount : bounties.values()) {
            total += amount;
        }
        return total;
    }

    public List<Map.Entry<UUID, Double>> getTopBounties(int limit) {
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(bounties.entrySet());
        sorted.sort(new Comparator<Map.Entry<UUID, Double>>() {
            @Override
            public int compare(Map.Entry<UUID, Double> a, Map.Entry<UUID, Double> b) {
                return Double.compare(b.getValue(), a.getValue());
            }
        });

        if (limit > 0 && sorted.size() > limit) {
            return sorted.subList(0, limit);
        }
        return sorted;
    }

    public int getRank(UUID uuid) {
        if (!hasBounty(uuid)) {
            return 0;
        }
        List<Map.Entry<UUID, Double>> top = getTopBounties(0);
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).getKey().equals(uuid)) {
                return i + 1;
            }
        }
        return 0;
    }
}