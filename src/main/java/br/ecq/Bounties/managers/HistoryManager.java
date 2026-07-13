package br.ecq.Bounties.managers;

import br.ecq.Bounties.BountiesPlugin;
import br.ecq.Bounties.model.HistoryEntry;
import br.ecq.Bounties.model.PlayerStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HistoryManager {

    private final BountiesPlugin plugin;
    private final List<HistoryEntry> history = Collections.synchronizedList(new ArrayList<HistoryEntry>());
    private final Map<UUID, PlayerStats> stats = new ConcurrentHashMap<UUID, PlayerStats>();
    private File historyFile;
    private File statsFile;

    public HistoryManager(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        historyFile = new File(plugin.getDataFolder(), "history.yml");
        statsFile = new File(plugin.getDataFolder(), "stats.yml");
        history.clear();
        stats.clear();

        if (historyFile.exists()) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(historyFile);
            List<?> raw = data.getList("entries");
            if (raw != null) {
                for (Object obj : raw) {
                    if (!(obj instanceof Map)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) obj;
                    HistoryEntry entry = fromMap(map);
                    if (entry != null) {
                        history.add(entry);
                    }
                }
            }
        }

        if (statsFile.exists()) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(statsFile);
            ConfigurationSection section = data.getConfigurationSection("stats");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        PlayerStats s = new PlayerStats();
                        s.setEarned(section.getDouble(key + ".earned", 0));
                        s.setSpent(section.getDouble(key + ".spent", 0));
                        s.setClaims(section.getInt(key + ".claims", 0));
                        s.setPlacements(section.getInt(key + ".placements", 0));
                        s.setKillsWithBounty(section.getInt(key + ".kills-with-bounty", 0));
                        stats.put(uuid, s);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
    }

    public void save() {
        saveHistory();
        saveStats();
    }

    private void saveHistory() {
        if (historyFile == null) {
            historyFile = new File(plugin.getDataFolder(), "history.yml");
        }
        FileConfiguration data = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        synchronized (history) {
            int max = plugin.getConfigManager().getHistoryMaxEntries();
            int start = 0;
            if (max > 0 && history.size() > max) {
                start = history.size() - max;
            }
            for (int i = start; i < history.size(); i++) {
                list.add(toMap(history.get(i)));
            }
        }
        data.set("entries", list);
        try {
            plugin.getDataFolder().mkdirs();
            data.save(historyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nao foi possivel salvar history.yml: " + e.getMessage());
        }
    }

    private void saveStats() {
        if (statsFile == null) {
            statsFile = new File(plugin.getDataFolder(), "stats.yml");
        }
        FileConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            String path = "stats." + entry.getKey().toString();
            PlayerStats s = entry.getValue();
            data.set(path + ".earned", s.getEarned());
            data.set(path + ".spent", s.getSpent());
            data.set(path + ".claims", s.getClaims());
            data.set(path + ".placements", s.getPlacements());
            data.set(path + ".kills-with-bounty", s.getKillsWithBounty());
        }
        try {
            plugin.getDataFolder().mkdirs();
            data.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nao foi possivel salvar stats.yml: " + e.getMessage());
        }
    }

    public void add(HistoryEntry entry) {
        if (entry == null) {
            return;
        }
        history.add(entry);
        int max = plugin.getConfigManager().getHistoryMaxEntries();
        if (max > 0) {
            synchronized (history) {
                while (history.size() > max) {
                    history.remove(0);
                }
            }
        }
    }

    public List<HistoryEntry> getRecent(int limit) {
        synchronized (history) {
            if (history.isEmpty()) {
                return Collections.emptyList();
            }
            int size = history.size();
            int from = limit > 0 ? Math.max(0, size - limit) : 0;
            return new ArrayList<HistoryEntry>(history.subList(from, size));
        }
    }

    public List<HistoryEntry> getRecentFor(UUID uuid, int limit) {
        List<HistoryEntry> result = new ArrayList<HistoryEntry>();
        if (uuid == null) {
            return result;
        }
        synchronized (history) {
            for (int i = history.size() - 1; i >= 0; i--) {
                HistoryEntry entry = history.get(i);
                if (entry.involves(uuid)) {
                    result.add(entry);
                    if (limit > 0 && result.size() >= limit) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public PlayerStats getStats(UUID uuid) {
        PlayerStats s = stats.get(uuid);
        if (s == null) {
            s = new PlayerStats();
            stats.put(uuid, s);
        }
        return s;
    }

    public void recordPlacement(UUID actor, String actorName, UUID target, String targetName, double amount, boolean add) {
        getStats(actor).addSpent(amount);
        getStats(actor).addPlacement();
        add(new HistoryEntry(
                add ? HistoryEntry.Type.ADD : HistoryEntry.Type.SET,
                System.currentTimeMillis(),
                actor, actorName,
                target, targetName,
                null, null,
                amount
        ));
    }

    public void recordRemove(UUID actor, String actorName, UUID target, String targetName, double amount) {
        add(new HistoryEntry(
                HistoryEntry.Type.REMOVE,
                System.currentTimeMillis(),
                actor, actorName,
                target, targetName,
                null, null,
                amount
        ));
    }

    public void recordClaim(UUID killer, String killerName, UUID victim, String victimName, double amount) {
        getStats(killer).addEarned(amount);
        getStats(killer).addClaim();
        getStats(killer).addKillWithBounty();
        add(new HistoryEntry(
                HistoryEntry.Type.CLAIM,
                System.currentTimeMillis(),
                killer, killerName,
                victim, victimName,
                null, null,
                amount
        ));
    }

    public void recordExpire(UUID target, String targetName, double amount) {
        add(new HistoryEntry(
                HistoryEntry.Type.EXPIRE,
                System.currentTimeMillis(),
                null, "Sistema",
                target, targetName,
                null, null,
                amount
        ));
    }

    public void recordRefund(UUID player, String playerName, UUID target, String targetName, double amount) {
        add(new HistoryEntry(
                HistoryEntry.Type.REFUND,
                System.currentTimeMillis(),
                player, playerName,
                target, targetName,
                null, null,
                amount
        ));
    }

    private Map<String, Object> toMap(HistoryEntry entry) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("type", entry.getType().name());
        map.put("time", entry.getTimestamp());
        map.put("actor", entry.getActor() != null ? entry.getActor().toString() : "");
        map.put("actorName", entry.getActorName());
        map.put("target", entry.getTarget() != null ? entry.getTarget().toString() : "");
        map.put("targetName", entry.getTargetName());
        map.put("extra", entry.getExtra() != null ? entry.getExtra().toString() : "");
        map.put("extraName", entry.getExtraName());
        map.put("amount", entry.getAmount());
        return map;
    }

    private HistoryEntry fromMap(Map<String, Object> map) {
        try {
            String typeStr = String.valueOf(map.get("type"));
            HistoryEntry.Type type = HistoryEntry.Type.valueOf(typeStr);
            long time = toLong(map.get("time"));
            UUID actor = parseUuid(map.get("actor"));
            String actorName = String.valueOf(map.get("actorName"));
            UUID target = parseUuid(map.get("target"));
            String targetName = String.valueOf(map.get("targetName"));
            UUID extra = parseUuid(map.get("extra"));
            String extraName = map.get("extraName") != null ? String.valueOf(map.get("extraName")) : "";
            double amount = toDouble(map.get("amount"));
            return new HistoryEntry(type, time, actor, actorName, target, targetName, extra, extraName, amount);
        } catch (Exception e) {
            return null;
        }
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        if (s.isEmpty() || s.equals("null")) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
