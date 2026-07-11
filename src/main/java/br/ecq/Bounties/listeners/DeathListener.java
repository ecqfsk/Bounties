package br.ecq.Bounties.listeners;

import br.ecq.Bounties.BountiesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class DeathListener implements Listener {

    private final BountiesPlugin plugin;

    public DeathListener(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) {
            return;
        }

        if (killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        handleKillTracking(killer);

        if (!plugin.getBountyManager().hasBounty(victim.getUniqueId())) {
            return;
        }

        double bounty = plugin.getBountyManager().removeBounty(victim.getUniqueId());
        plugin.getEconomyManager().deposit(killer, bounty);

        String formatted = plugin.getEconomyManager().format(bounty);

        killer.sendMessage(plugin.getConfigManager().format(
                plugin.getConfigManager().getMessage("bounty-claimed-killer"),
                "amount", formatted,
                "victim", victim.getName()
        ));

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

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getBountyManager().save();
            }
        });
    }

    private void handleKillTracking(Player killer) {
        UUID previousTop = plugin.getKillManager().getTopKiller();
        plugin.getKillManager().addKill(killer.getUniqueId());
        UUID newTop = plugin.getKillManager().updateTopKiller();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getKillManager().save();
            }
        });

        if (newTop == null || !plugin.getConfigManager().isBroadcastNewTopKiller()) {
            return;
        }

        OfflinePlayer topPlayer = Bukkit.getOfflinePlayer(newTop);
        String topName = topPlayer.getName() != null ? topPlayer.getName() : "Desconhecido";

        Bukkit.broadcastMessage(plugin.getConfigManager().format(
                plugin.getConfigManager().getMessage("new-top-killer"),
                "player", topName,
                "kills", String.valueOf(plugin.getKillManager().getKills(newTop))
        ));

        if (previousTop != null && !previousTop.equals(newTop)) {
            Player previousPlayer = Bukkit.getPlayer(previousTop);
            if (previousPlayer != null && previousPlayer.isOnline()) {
                previousPlayer.sendMessage(plugin.getConfigManager().getMessage("lost-top-killer"));
            }
        }
    }
}