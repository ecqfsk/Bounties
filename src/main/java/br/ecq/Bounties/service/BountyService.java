package br.ecq.Bounties.service;

import br.ecq.Bounties.BountiesPlugin;
import br.ecq.Bounties.managers.VisualEffectManager;
import br.ecq.Bounties.model.BountyData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

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

        BountyResult limits = validateLimits(player, target.getUniqueId(), amount, false);
        if (limits != BountyResult.SUCCESS) {
            return limits;
        }

        double totalCost = calculateTotalCost(amount);
        if (!plugin.getEconomyManager().has(player, totalCost)) {
            return BountyResult.INSUFFICIENT_FUNDS;
        }

        // Reembolsa contribuidores anteriores se for substituicao
        BountyData existing = plugin.getBountyManager().getBountyData(target.getUniqueId());
        if (existing != null && !existing.isEmpty() && plugin.getConfigManager().isRefundOnReplace()) {
            refundContributors(existing, target.getName(), player.getName() + " (substituicao)");
        }

        plugin.getEconomyManager().withdraw(player, totalCost);
        long expires = plugin.getConfigManager().computeExpiresAt();
        plugin.getBountyManager().setBounty(target.getUniqueId(), player.getUniqueId(), amount, expires);
        plugin.getCooldownManager().markPlacement(player.getUniqueId());

        plugin.getHistoryManager().recordPlacement(
                player.getUniqueId(), player.getName(),
                target.getUniqueId(), target.getName(),
                amount, false
        );

        String formatted = plugin.getEconomyManager().format(amount);
        player.sendMessage(plugin.getConfigManager().format(
                plugin.getConfigManager().getMessage("bounty-set"),
                "amount", formatted,
                "target", target.getName()
        ));

        if (expires > 0) {
            player.sendMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-expires-in"),
                    "time", VisualEffectManager.formatDuration(expires - System.currentTimeMillis())
            ));
        }

        if (plugin.getConfigManager().isAnnounceBounty()) {
            Bukkit.broadcastMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-announced-set"),
                    "player", player.getName(),
                    "amount", formatted,
                    "target", target.getName()
            ));
        }

        if (target.isOnline()) {
            plugin.getVisualEffectManager().playPlaceEffects(target.getPlayer());
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

        BountyResult limits = validateLimits(player, target.getUniqueId(), amount, true);
        if (limits != BountyResult.SUCCESS) {
            return limits;
        }

        double totalCost = calculateTotalCost(amount);
        if (!plugin.getEconomyManager().has(player, totalCost)) {
            return BountyResult.INSUFFICIENT_FUNDS;
        }

        plugin.getEconomyManager().withdraw(player, totalCost);
        long expires = plugin.getConfigManager().computeExpiresAt();
        BountyData data = plugin.getBountyManager().addBounty(
                target.getUniqueId(),
                player.getUniqueId(),
                amount,
                expires,
                plugin.getConfigManager().isRefreshExpireOnAdd()
        );
        plugin.getCooldownManager().markPlacement(player.getUniqueId());

        double total = data != null ? data.getAmount() : amount;
        plugin.getHistoryManager().recordPlacement(
                player.getUniqueId(), player.getName(),
                target.getUniqueId(), target.getName(),
                amount, true
        );

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

        if (target.isOnline()) {
            plugin.getVisualEffectManager().playPlaceEffects(target.getPlayer());
        }

        saveAsync();
        return BountyResult.SUCCESS;
    }

    /**
     * Remove recompensa completa (admin/remove) ou apenas a contribuicao do jogador.
     */
    public BountyResult removeBounty(Player player, OfflinePlayer target) {
        if (target == null || target.getUniqueId() == null) {
            return BountyResult.PLAYER_NOT_FOUND;
        }

        BountyData data = plugin.getBountyManager().getBountyData(target.getUniqueId());
        if (data == null || data.isEmpty()) {
            return BountyResult.BOUNTY_NOT_FOUND;
        }

        boolean isAdmin = player.hasPermission("bounties.remove") || player.hasPermission("bounties.admin");
        boolean isContributor = data.hasContributor(player.getUniqueId());
        boolean allowOwn = plugin.getConfigManager().isAllowOwnRemove()
                && player.hasPermission("bounties.remove.own")
                && isContributor;

        if (!isAdmin && !allowOwn) {
            return BountyResult.NO_PERMISSION;
        }

        if (isAdmin) {
            double amount = data.getAmount();
            Map<UUID, Double> contributors = data.getContributors();
            plugin.getBountyManager().removeBountyData(target.getUniqueId());

            if (plugin.getConfigManager().isRefundOnRemove()) {
                refundMap(contributors, target.getName(), player.getName());
            }

            plugin.getHistoryManager().recordRemove(
                    player.getUniqueId(), player.getName(),
                    target.getUniqueId(), target.getName(),
                    amount
            );

            player.sendMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-removed"),
                    "target", target.getName(),
                    "amount", plugin.getEconomyManager().format(amount)
            ));
        } else {
            double amount = plugin.getBountyManager().removeContribution(target.getUniqueId(), player.getUniqueId());
            if (amount <= 0) {
                return BountyResult.NOT_CONTRIBUTOR;
            }

            double refund = applyRefundPercent(amount);
            if (refund > 0 && plugin.getConfigManager().isRefundOnRemove()) {
                plugin.getEconomyManager().deposit(player, refund);
                plugin.getHistoryManager().recordRefund(
                        player.getUniqueId(), player.getName(),
                        target.getUniqueId(), target.getName(),
                        refund
                );
            }

            plugin.getHistoryManager().recordRemove(
                    player.getUniqueId(), player.getName(),
                    target.getUniqueId(), target.getName(),
                    amount
            );

            player.sendMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-removed-own"),
                    "target", target.getName(),
                    "amount", plugin.getEconomyManager().format(refund > 0 ? refund : amount)
            ));
        }

        saveAsync();
        return BountyResult.SUCCESS;
    }

    /**
     * Remove por console/admin sem player online (sem reembolso de "ator" específico).
     */
    public BountyResult removeBountyAdmin(OfflinePlayer target, String actorName) {
        BountyData data = plugin.getBountyManager().getBountyData(target.getUniqueId());
        if (data == null || data.isEmpty()) {
            return BountyResult.BOUNTY_NOT_FOUND;
        }

        double amount = data.getAmount();
        Map<UUID, Double> contributors = data.getContributors();
        plugin.getBountyManager().removeBountyData(target.getUniqueId());

        if (plugin.getConfigManager().isRefundOnRemove()) {
            refundMap(contributors, target.getName(), actorName);
        }

        plugin.getHistoryManager().recordRemove(
                null, actorName != null ? actorName : "Console",
                target.getUniqueId(), target.getName(),
                amount
        );
        saveAsync();
        return BountyResult.SUCCESS;
    }

    public void claimBounty(Player killer, Player victim) {
        BountyData data = plugin.getBountyManager().getBountyData(victim.getUniqueId());
        if (data == null || data.isEmpty()) {
            return;
        }

        if (plugin.getConfigManager().getMinCombatSeconds() > 0) {
            if (!plugin.getCooldownManager().isInCombat(killer.getUniqueId())
                    && !plugin.getCooldownManager().isInCombat(victim.getUniqueId())) {
                killer.sendMessage(plugin.getConfigManager().getMessage("claim-not-in-combat"));
                return;
            }
        }

        double bounty = data.getAmount();
        String contributorsText = plugin.getVisualEffectManager().formatContributors(data, 5);

        plugin.getBountyManager().removeBountyData(victim.getUniqueId());
        plugin.getEconomyManager().deposit(killer, bounty);

        plugin.getHistoryManager().recordClaim(
                killer.getUniqueId(), killer.getName(),
                victim.getUniqueId(), victim.getName(),
                bounty
        );

        String formatted = plugin.getEconomyManager().format(bounty);

        killer.sendMessage(plugin.getConfigManager().format(
                plugin.getConfigManager().getMessage("bounty-claimed-killer"),
                "amount", formatted,
                "victim", victim.getName()
        ));

        if (!contributorsText.isEmpty()) {
            killer.sendMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-claimed-contributors"),
                    "contributors", contributorsText
            ));
        }

        if (victim.isOnline()) {
            victim.sendMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-claimed-victim"),
                    "amount", formatted,
                    "killer", killer.getName()
            ));
        }

        if (plugin.getConfigManager().isAnnounceClaim()) {
            Bukkit.broadcastMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-announced-claim"),
                    "amount", formatted,
                    "killer", killer.getName(),
                    "victim", victim.getName()
            ));
        }

        plugin.getVisualEffectManager().playClaimEffects(killer, victim);
        saveAsync();
    }

    public int processExpirations() {
        long now = System.currentTimeMillis();
        int count = 0;
        for (BountyData data : plugin.getBountyManager().getExpired(now)) {
            UUID target = data.getTarget();
            double amount = data.getAmount();
            Map<UUID, Double> contributors = data.getContributors();
            String targetName = Bukkit.getOfflinePlayer(target).getName();
            if (targetName == null) {
                targetName = target.toString();
            }

            plugin.getBountyManager().removeBountyData(target);

            if (plugin.getConfigManager().isExpireRefund()) {
                refundMap(contributors, targetName, "Expiracao");
            }

            plugin.getHistoryManager().recordExpire(target, targetName, amount);

            if (plugin.getConfigManager().isAnnounceExpire()) {
                Bukkit.broadcastMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("bounty-expired"),
                        "target", targetName,
                        "amount", plugin.getEconomyManager().format(amount)
                ));
            }

            OfflinePlayer offline = Bukkit.getOfflinePlayer(target);
            if (offline.isOnline()) {
                offline.getPlayer().sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("bounty-expired-self"),
                        "amount", plugin.getEconomyManager().format(amount)
                ));
            }

            count++;
        }

        if (count > 0) {
            saveAsync();
        }
        return count;
    }

    public void processExpireWarnings() {
        int warnSec = plugin.getConfigManager().getExpireWarnSeconds();
        if (warnSec <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long warnMs = warnSec * 1000L;

        for (BountyData data : plugin.getBountyManager().getExpiringSoon(now, warnMs)) {
            // Avisa uma vez por "janela" aproximada: se restam menos que warn e mais que warn-60s
            long left = data.getMillisUntilExpire(now);
            if (left > warnMs - 60000L && left <= warnMs) {
                String targetName = Bukkit.getOfflinePlayer(data.getTarget()).getName();
                if (targetName == null) {
                    targetName = "?";
                }
                String time = VisualEffectManager.formatDuration(left);
                for (UUID contrib : data.getContributors().keySet()) {
                    if (BountyData.isUnknownContributor(contrib)) {
                        continue;
                    }
                    Player p = Bukkit.getPlayer(contrib);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(plugin.getConfigManager().format(
                                plugin.getConfigManager().getMessage("bounty-expire-warn"),
                                "target", targetName,
                                "time", time,
                                "amount", plugin.getEconomyManager().format(data.getAmount())
                        ));
                    }
                }
                Player targetPlayer = Bukkit.getPlayer(data.getTarget());
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    targetPlayer.sendMessage(plugin.getConfigManager().format(
                            plugin.getConfigManager().getMessage("bounty-expire-warn-target"),
                            "time", time,
                            "amount", plugin.getEconomyManager().format(data.getAmount())
                    ));
                }
            }
        }
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

    public BountyResult validateLimits(Player player, UUID targetUuid, double amount, boolean isAdd) {
        if (!player.hasPermission("bounties.admin") && !player.hasPermission("bounties.bypass.cooldown")) {
            if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId())) {
                return BountyResult.ON_COOLDOWN;
            }
        }

        if (!player.hasPermission("bounties.admin") && !player.hasPermission("bounties.bypass.limits")) {
            int maxActive = plugin.getConfigManager().getMaxActivePlaced();
            if (maxActive > 0) {
                boolean alreadyOnTarget = plugin.getBountyManager().hasBounty(targetUuid)
                        && plugin.getBountyManager().getBountyData(targetUuid) != null
                        && plugin.getBountyManager().getBountyData(targetUuid).hasContributor(player.getUniqueId());
                int current = plugin.getBountyManager().countPlacedBy(player.getUniqueId());
                if (!alreadyOnTarget && current >= maxActive) {
                    return BountyResult.MAX_ACTIVE_PLACED;
                }
            }

            double maxPlaced = plugin.getConfigManager().getMaxAmountPlaced();
            if (maxPlaced > 0) {
                double currentSum = plugin.getBountyManager().sumPlacedBy(player.getUniqueId());
                // Em set no mesmo alvo, a contribuicao atual sera substituida
                BountyData existing = plugin.getBountyManager().getBountyData(targetUuid);
                if (!isAdd && existing != null) {
                    currentSum -= existing.getContribution(player.getUniqueId());
                }
                if (currentSum + amount > maxPlaced) {
                    return BountyResult.MAX_AMOUNT_PLACED;
                }
            }
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

    public double applyRefundPercent(double amount) {
        double percent = plugin.getConfigManager().getRefundPercent();
        if (percent >= 100) {
            return amount;
        }
        if (percent <= 0) {
            return 0;
        }
        return amount * (percent / 100.0);
    }

    private void refundContributors(BountyData data, String targetName, String reason) {
        refundMap(data.getContributors(), targetName, reason);
    }

    private void refundMap(Map<UUID, Double> contributors, String targetName, String reason) {
        if (contributors == null || contributors.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Double> entry : contributors.entrySet()) {
            UUID uuid = entry.getKey();
            if (BountyData.isUnknownContributor(uuid)) {
                continue;
            }
            double refund = applyRefundPercent(entry.getValue());
            if (refund <= 0) {
                continue;
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            plugin.getEconomyManager().deposit(offline, refund);
            plugin.getHistoryManager().recordRefund(
                    uuid,
                    offline.getName() != null ? offline.getName() : uuid.toString(),
                    null,
                    targetName,
                    refund
            );
            if (offline.isOnline()) {
                offline.getPlayer().sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("bounty-refunded"),
                        "amount", plugin.getEconomyManager().format(refund),
                        "target", targetName != null ? targetName : "?",
                        "reason", reason != null ? reason : ""
                ));
            }
        }
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
            case ON_COOLDOWN:
                long remain = plugin.getCooldownManager().getRemainingCooldownMs(player.getUniqueId());
                player.sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("on-cooldown"),
                        "time", VisualEffectManager.formatDuration(remain)
                ));
                break;
            case MAX_ACTIVE_PLACED:
                player.sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("max-active-placed"),
                        "max", String.valueOf(plugin.getConfigManager().getMaxActivePlaced())
                ));
                break;
            case MAX_AMOUNT_PLACED:
                player.sendMessage(plugin.getConfigManager().format(
                        plugin.getConfigManager().getMessage("max-amount-placed"),
                        "max", plugin.getEconomyManager().format(plugin.getConfigManager().getMaxAmountPlaced())
                ));
                break;
            case NOT_IN_COMBAT:
                player.sendMessage(plugin.getConfigManager().getMessage("claim-not-in-combat"));
                break;
            case BOUNTY_NOT_FOUND:
                player.sendMessage(plugin.getConfigManager().getMessage("bounty-not-found").replace("{target}", ""));
                break;
            case NOT_CONTRIBUTOR:
                player.sendMessage(plugin.getConfigManager().getMessage("not-contributor"));
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
                plugin.getHistoryManager().save();
            }
        });
    }
}
