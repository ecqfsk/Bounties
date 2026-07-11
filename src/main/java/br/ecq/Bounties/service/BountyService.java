package br.ecq.Bounties.service;

import br.ecq.Bounties.BountiesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class BountyService {

    private final BountiesPlugin plugin;

    public BountyService(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public BountyResult setBounty(Player player, OfflinePlayer target, double amount) {
        if (!player.hasPermission("bounties.set")) {
            return BountyResult.NO_PERMISSION;
        }

        BountyResult validation = validatePlacement(player, target, amount, "bounties.set.other");
        if (validation != BountyResult.SUCCESS) {
            return validation;
        }

        double totalCost = calculateTotalCost(amount);
        if (!plugin.getEconomyManager().has(player, totalCost)) {
            return BountyResult.INSUFFICIENT_FUNDS;
        }

        plugin.getEconomyManager().withdraw(player, totalCost);
        plugin.getBountyManager().setBounty(target.getUniqueId(), amount);

        String formatted = plugin.getEconomyManager().format(amount);
        player.sendMessage(plugin.getConfigManager().format(
                plugin.getConfigManager().getMessage("bounty-set"),
                "amount", formatted,
                "target", target.getName()
        ));

        if (plugin.getConfigManager().isAnnounceBounty()) {
            Bukkit.broadcastMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-announced-set"),
                    "player", player.getName(),
                    "amount", formatted,
                    "target", target.getName()
            ));
        }

        saveAsync();
        return BountyResult.SUCCESS;
    }

    public BountyResult addBounty(Player player, OfflinePlayer target, double amount) {
        if (!player.hasPermission("bounties.add")) {
            return BountyResult.NO_PERMISSION;
        }

        BountyResult validation = validatePlacement(player, target, amount, "bounties.add.other");
        if (validation != BountyResult.SUCCESS) {
            return validation;
        }

        double totalCost = calculateTotalCost(amount);
        if (!plugin.getEconomyManager().has(player, totalCost)) {
            return BountyResult.INSUFFICIENT_FUNDS;
        }

        plugin.getEconomyManager().withdraw(player, totalCost);
        double total = plugin.getBountyManager().addBounty(target.getUniqueId(), amount);

        String formatted = plugin.getEconomyManager().format(amount);
        String formattedTotal = plugin.getEconomyManager().format(total);
        player.sendMessage(plugin.getConfigManager().format(
                plugin.getConfigManager().getMessage("bounty-added"),
                "amount", formatted,
                "target", target.getName(),
                "total", formattedTotal
        ));

        if (plugin.getConfigManager().isAnnounceBounty()) {
            Bukkit.broadcastMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-announced-set"),
                    "player", player.getName(),
                    "amount", formattedTotal,
                    "target", target.getName()
            ));
        }

        saveAsync();
        return BountyResult.SUCCESS;
    }

    public BountyResult validateAmount(double amount) {
        if (amount <= 0) {
            return BountyResult.INVALID_AMOUNT;
        }
        if (amount < plugin.getConfigManager().getMinAmount()) {
            return BountyResult.TOO_LOW;
        }
        double max = plugin.getConfigManager().getMaxAmount();
        if (max > 0 && amount > max) {
            return BountyResult.TOO_HIGH;
        }
        return BountyResult.SUCCESS;
    }

    public BountyResult validatePlacement(Player player, OfflinePlayer target, double amount, String otherPermission) {
        if (target == null || target.getUniqueId() == null || !hasPlayedBefore(target)) {
            return BountyResult.PLAYER_NOT_FOUND;
        }

        if (!target.getUniqueId().equals(player.getUniqueId())
                && !player.hasPermission(otherPermission)
                && !player.hasPermission("bounties.admin")) {
            return BountyResult.NO_PERMISSION;
        }

        BountyResult amountResult = validateAmount(amount);
        if (amountResult != BountyResult.SUCCESS) {
            return amountResult;
        }

        if (plugin.getConfigManager().isBlockSelfBounty()
                && target.getUniqueId().equals(player.getUniqueId())) {
            return BountyResult.CANNOT_SELF;
        }

        if (target.isOnline() && target.getPlayer().hasPermission("bounties.bypass")
                && !player.hasPermission("bounties.admin")) {
            return BountyResult.BYPASS;
        }

        return BountyResult.SUCCESS;
    }

    public double calculateTotalCost(double amount) {
        double fee = plugin.getConfigManager().getPlacementFeePercent();
        if (fee <= 0) {
            return amount;
        }
        return amount + (amount * fee / 100.0);
    }

    public void sendResultMessage(Player player, BountyResult result, double amount) {
        switch (result) {
            case NO_PERMISSION:
                player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                break;
            case CANNOT_SELF:
                player.sendMessage(plugin.getConfigManager().getMessage("cannot-bounty-self"));
                break;
            case BYPASS:
                player.sendMessage(plugin.getConfigManager().getMessage("player-bypass"));
                break;
            case INSUFFICIENT_FUNDS:
                player.sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("insufficient-funds"),
                        "amount", plugin.getEconomyManager().format(calculateTotalCost(amount))
                ));
                break;
            case INVALID_AMOUNT:
                player.sendMessage(plugin.getConfigManager().getMessage("invalid-amount"));
                break;
            case TOO_LOW:
                player.sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("amount-too-low"),
                        "min", plugin.getEconomyManager().format(plugin.getConfigManager().getMinAmount())
                ));
                break;
            case TOO_HIGH:
                player.sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("amount-too-high"),
                        "max", plugin.getEconomyManager().format(plugin.getConfigManager().getMaxAmount())
                ));
                break;
            case PLAYER_NOT_FOUND:
                player.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
                break;
            default:
                break;
        }
    }

    private boolean hasPlayedBefore(OfflinePlayer player) {
        try {
            return player.hasPlayedBefore() || player.isOnline();
        } catch (NoSuchMethodError e) {
            return player.getName() != null;
        }
    }

    private void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getBountyManager().save();
            }
        });
    }
}