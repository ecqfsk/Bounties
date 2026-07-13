package br.ecq.Bounties.managers;

import br.ecq.Bounties.BountiesPlugin;
import br.ecq.Bounties.model.BountyData;
import org.bukkit.configuration.ConfigurationSection;
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
    private final Map<UUID, BountyData> bounties = new LinkedHashMap<UUID, BountyData>();
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
        ConfigurationSection section = data.getConfigurationSection("bounties");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                if (section.isConfigurationSection(key)) {
                    BountyData bounty = loadModern(uuid, section.getConfigurationSection(key));
                    if (bounty != null && !bounty.isEmpty()) {
                        bounties.put(uuid, bounty);
                    }
                } else {
                    double amount = section.getDouble(key);
                    if (amount > 0) {
                        bounties.put(uuid, BountyData.legacy(uuid, amount));
                    }
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private BountyData loadModern(UUID target, ConfigurationSection section) {
        BountyData data = new BountyData(target);
        data.setCreatedAt(section.getLong("created", System.currentTimeMillis()));
        data.setExpiresAt(section.getLong("expires", 0L));
        data.setLastUpdated(section.getLong("updated", data.getCreatedAt()));

        ConfigurationSection contrib = section.getConfigurationSection("contributors");
        if (contrib != null) {
            for (String cKey : contrib.getKeys(false)) {
                try {
                    UUID cUuid = UUID.fromString(cKey);
                    double amount = contrib.getDouble(cKey);
                    if (amount > 0) {
                        data.setContribution(cUuid, amount);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // Compat: amount total sem contribuidores
        if (data.isEmpty()) {
            double amount = section.getDouble("amount", 0);
            if (amount > 0) {
                data.setContribution(BountyData.UNKNOWN_CONTRIBUTOR, amount);
            }
        }

        return data;
    }

    public void save() {
        if (dataFile == null) {
            dataFile = new File(plugin.getDataFolder(), "bounties.yml");
        }

        FileConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, BountyData> entry : bounties.entrySet()) {
            BountyData bounty = entry.getValue();
            if (bounty == null || bounty.isEmpty()) {
                continue;
            }
            String path = "bounties." + entry.getKey().toString();
            data.set(path + ".amount", bounty.getAmount());
            data.set(path + ".created", bounty.getCreatedAt());
            data.set(path + ".expires", bounty.getExpiresAt());
            data.set(path + ".updated", bounty.getLastUpdated());
            for (Map.Entry<UUID, Double> c : bounty.getContributors().entrySet()) {
                data.set(path + ".contributors." + c.getKey().toString(), c.getValue());
            }
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nao foi possivel salvar bounties.yml: " + e.getMessage());
        }
    }

    public BountyData getBountyData(UUID uuid) {
        return bounties.get(uuid);
    }

    public double getBounty(UUID uuid) {
        BountyData data = bounties.get(uuid);
        return data != null ? data.getAmount() : 0;
    }

    public boolean hasBounty(UUID uuid) {
        BountyData data = bounties.get(uuid);
        return data != null && !data.isEmpty();
    }

    public void putBounty(BountyData data) {
        if (data == null || data.isEmpty()) {
            bounties.remove(data != null ? data.getTarget() : null);
            return;
        }
        bounties.put(data.getTarget(), data);
    }

    public BountyData getOrCreate(UUID target) {
        BountyData data = bounties.get(target);
        if (data == null) {
            data = new BountyData(target);
            bounties.put(target, data);
        }
        return data;
    }

    /**
     * Define recompensa (substitui contribuidores).
     */
    public BountyData setBounty(UUID target, UUID contributor, double amount, long expiresAt) {
        BountyData data = new BountyData(target);
        data.replaceWith(contributor, amount);
        data.setExpiresAt(expiresAt);
        if (amount <= 0 || data.isEmpty()) {
            bounties.remove(target);
            return null;
        }
        bounties.put(target, data);
        return data;
    }

    public BountyData addBounty(UUID target, UUID contributor, double amount, long expiresAt, boolean refreshExpire) {
        BountyData data = getOrCreate(target);
        data.addContribution(contributor, amount);
        if (refreshExpire || !data.hasExpiration()) {
            data.setExpiresAt(expiresAt);
        }
        if (data.isEmpty()) {
            bounties.remove(target);
            return null;
        }
        return data;
    }

    public BountyData removeBountyData(UUID uuid) {
        return bounties.remove(uuid);
    }

    public double removeBounty(UUID uuid) {
        BountyData data = bounties.remove(uuid);
        return data != null ? data.getAmount() : 0;
    }

    public double removeContribution(UUID target, UUID contributor) {
        BountyData data = bounties.get(target);
        if (data == null) {
            return 0;
        }
        double removed = data.removeContribution(contributor);
        if (data.isEmpty()) {
            bounties.remove(target);
        }
        return removed;
    }

    public Map<UUID, Double> getAllBounties() {
        Map<UUID, Double> map = new LinkedHashMap<UUID, Double>();
        for (Map.Entry<UUID, BountyData> entry : bounties.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                map.put(entry.getKey(), entry.getValue().getAmount());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    public Map<UUID, BountyData> getAllBountyData() {
        return Collections.unmodifiableMap(bounties);
    }

    public int getTotalBounties() {
        int count = 0;
        for (BountyData data : bounties.values()) {
            if (data != null && !data.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public double getTotalAmount() {
        double total = 0;
        for (BountyData data : bounties.values()) {
            if (data != null) {
                total += data.getAmount();
            }
        }
        return total;
    }

    public int countPlacedBy(UUID contributor) {
        int count = 0;
        for (BountyData data : bounties.values()) {
            if (data != null && data.hasContributor(contributor)) {
                count++;
            }
        }
        return count;
    }

    public double sumPlacedBy(UUID contributor) {
        double total = 0;
        for (BountyData data : bounties.values()) {
            if (data != null) {
                total += data.getContribution(contributor);
            }
        }
        return total;
    }

    public List<Map.Entry<UUID, Double>> getTopBounties(int limit) {
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<Map.Entry<UUID, Double>>(getAllBounties().entrySet());
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

    public List<BountyData> getExpired(long now) {
        List<BountyData> expired = new ArrayList<BountyData>();
        for (BountyData data : bounties.values()) {
            if (data != null && data.isExpired(now)) {
                expired.add(data);
            }
        }
        return expired;
    }

    public List<BountyData> getExpiringSoon(long now, long warnMs) {
        List<BountyData> list = new ArrayList<BountyData>();
        if (warnMs <= 0) {
            return list;
        }
        for (BountyData data : bounties.values()) {
            if (data == null || !data.hasExpiration() || data.isExpired(now)) {
                continue;
            }
            long left = data.getMillisUntilExpire(now);
            if (left > 0 && left <= warnMs) {
                list.add(data);
            }
        }
        return list;
    }
}
