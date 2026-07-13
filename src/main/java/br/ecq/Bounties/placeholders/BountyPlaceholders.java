package br.ecq.Bounties.placeholders;

import br.ecq.Bounties.BountiesPlugin;
import br.ecq.Bounties.managers.VisualEffectManager;
import br.ecq.Bounties.model.BountyData;
import br.ecq.Bounties.model.PlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyPlaceholders extends PlaceholderExpansion {

    private final BountiesPlugin plugin;

    public BountyPlaceholders(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "bounties";
    }

    @Override
    public String getAuthor() {
        return "ecq";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null || params.isEmpty()) {
            return null;
        }

        String lower = params.toLowerCase();

        if (lower.equals("total_count")) {
            return String.valueOf(plugin.getBountyManager().getTotalBounties());
        }

        if (lower.equals("total_amount")) {
            return plugin.getEconomyManager().format(plugin.getBountyManager().getTotalAmount());
        }

        if (lower.equals("total_amount_raw")) {
            return String.valueOf(plugin.getBountyManager().getTotalAmount());
        }

        if (lower.startsWith("top_name_")) {
            return getTopName(lower.replace("top_name_", ""));
        }

        if (lower.startsWith("top_amount_")) {
            return getTopAmount(lower.replace("top_amount_", ""));
        }

        if (lower.startsWith("top_amount_raw_")) {
            return getTopAmountRaw(lower.replace("top_amount_raw_", ""));
        }

        if (lower.startsWith("amount_")) {
            String targetName = params.substring("amount_".length());
            return getAmountForName(targetName);
        }

        if (lower.startsWith("amount_raw_")) {
            String targetName = params.substring("amount_raw_".length());
            return getAmountRawForName(targetName);
        }

        if (lower.startsWith("has_")) {
            String targetName = params.substring("has_".length());
            return getHasForName(targetName);
        }

        if (lower.startsWith("rank_")) {
            String targetName = params.substring("rank_".length());
            return getRankForName(targetName);
        }

        if (lower.startsWith("killer_top_name_")) {
            return getKillerTopName(lower.replace("killer_top_name_", ""));
        }

        if (lower.startsWith("killer_top_kills_")) {
            return getKillerTopKills(lower.replace("killer_top_kills_", ""));
        }

        if (lower.equals("killer_top_name")) {
            return getKillerTopName("1");
        }

        if (lower.equals("killer_top_kills")) {
            return getKillerTopKills("1");
        }

        if (lower.equals("killer_tag")) {
            if (player == null) {
                return "";
            }
            return plugin.getKillManager().isTopKiller(player.getUniqueId())
                    ? plugin.getConfigManager().getKillerTagText()
                    : "";
        }

        if (lower.equals("is_top_killer")) {
            if (player == null) {
                return "false";
            }
            return plugin.getKillManager().isTopKiller(player.getUniqueId()) ? "true" : "false";
        }

        if (player == null) {
            return null;
        }

        PlayerStats stats = plugin.getHistoryManager().getStats(player.getUniqueId());
        BountyData data = plugin.getBountyManager().getBountyData(player.getUniqueId());

        switch (lower) {
            case "amount":
                return plugin.getEconomyManager().format(
                        plugin.getBountyManager().getBounty(player.getUniqueId())
                );
            case "amount_raw":
                return String.valueOf(plugin.getBountyManager().getBounty(player.getUniqueId()));
            case "has":
                return plugin.getBountyManager().hasBounty(player.getUniqueId()) ? "true" : "false";
            case "rank":
                return String.valueOf(plugin.getBountyManager().getRank(player.getUniqueId()));
            case "kills":
                return String.valueOf(plugin.getKillManager().getKills(player.getUniqueId()));
            case "kill_rank":
                return String.valueOf(plugin.getKillManager().getRank(player.getUniqueId()));
            case "expires":
            case "time_left":
                return plugin.getVisualEffectManager().formatTimeLeft(data);
            case "expires_raw":
                return data != null && data.hasExpiration()
                        ? String.valueOf(data.getMillisUntilExpire(System.currentTimeMillis()) / 1000L)
                        : "0";
            case "contributors":
                return stripColor(plugin.getVisualEffectManager().formatContributors(data, 5));
            case "contributor_count":
                return data != null ? String.valueOf(data.getContributorCount()) : "0";
            case "earned":
                return plugin.getEconomyManager().format(stats.getEarned());
            case "earned_raw":
                return String.valueOf(stats.getEarned());
            case "spent":
                return plugin.getEconomyManager().format(stats.getSpent());
            case "spent_raw":
                return String.valueOf(stats.getSpent());
            case "claims":
                return String.valueOf(stats.getClaims());
            case "placements":
                return String.valueOf(stats.getPlacements());
            case "bounty_kills":
                return String.valueOf(stats.getKillsWithBounty());
            case "active_placed":
                return String.valueOf(plugin.getBountyManager().countPlacedBy(player.getUniqueId()));
            case "active_placed_amount":
                return plugin.getEconomyManager().format(plugin.getBountyManager().sumPlacedBy(player.getUniqueId()));
            case "cooldown":
                long cd = plugin.getCooldownManager().getRemainingCooldownMs(player.getUniqueId());
                return cd > 0 ? VisualEffectManager.formatDuration(cd) : "0";
            case "cooldown_raw":
                return String.valueOf(plugin.getCooldownManager().getRemainingCooldownMs(player.getUniqueId()) / 1000L);
            default:
                return null;
        }
    }

    private String stripColor(String text) {
        if (text == null) {
            return "";
        }
        return org.bukkit.ChatColor.stripColor(text);
    }

    private String getKillerTopName(String indexStr) {
        int index = parseIndex(indexStr);
        if (index <= 0) {
            return "";
        }
        List<Map.Entry<UUID, Integer>> top = plugin.getKillManager().getTopKills(index);
        if (top.size() < index) {
            return "";
        }
        OfflinePlayer topPlayer = Bukkit.getOfflinePlayer(top.get(index - 1).getKey());
        return topPlayer.getName() != null ? topPlayer.getName() : "";
    }

    private String getKillerTopKills(String indexStr) {
        int index = parseIndex(indexStr);
        if (index <= 0) {
            return "0";
        }
        List<Map.Entry<UUID, Integer>> top = plugin.getKillManager().getTopKills(index);
        if (top.size() < index) {
            return "0";
        }
        return String.valueOf(top.get(index - 1).getValue());
    }

    private String getAmountForName(String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (target.getUniqueId() == null) {
            return "0";
        }
        return plugin.getEconomyManager().format(plugin.getBountyManager().getBounty(target.getUniqueId()));
    }

    private String getAmountRawForName(String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (target.getUniqueId() == null) {
            return "0";
        }
        return String.valueOf(plugin.getBountyManager().getBounty(target.getUniqueId()));
    }

    private String getHasForName(String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (target.getUniqueId() == null) {
            return "false";
        }
        return plugin.getBountyManager().hasBounty(target.getUniqueId()) ? "true" : "false";
    }

    private String getRankForName(String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (target.getUniqueId() == null) {
            return "0";
        }
        return String.valueOf(plugin.getBountyManager().getRank(target.getUniqueId()));
    }

    private String getTopName(String indexStr) {
        int index = parseIndex(indexStr);
        if (index <= 0) {
            return "";
        }
        List<Map.Entry<UUID, Double>> top = plugin.getBountyManager().getTopBounties(index);
        if (top.size() < index) {
            return "";
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(top.get(index - 1).getKey());
        return player.getName() != null ? player.getName() : "";
    }

    private String getTopAmount(String indexStr) {
        int index = parseIndex(indexStr);
        if (index <= 0) {
            return "0";
        }
        List<Map.Entry<UUID, Double>> top = plugin.getBountyManager().getTopBounties(index);
        if (top.size() < index) {
            return "0";
        }
        return plugin.getEconomyManager().format(top.get(index - 1).getValue());
    }

    private String getTopAmountRaw(String indexStr) {
        int index = parseIndex(indexStr);
        if (index <= 0) {
            return "0";
        }
        List<Map.Entry<UUID, Double>> top = plugin.getBountyManager().getTopBounties(index);
        if (top.size() < index) {
            return "0";
        }
        return String.valueOf(top.get(index - 1).getValue());
    }

    private int parseIndex(String indexStr) {
        try {
            return Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
