package br.ecq.Bounties.managers;

import br.ecq.Bounties.BountiesPlugin;
import br.ecq.Bounties.model.BountyData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VisualEffectManager {

    private final BountiesPlugin plugin;
    private BukkitTask task;
    private final Set<UUID> glowing = new HashSet<UUID>();

    public VisualEffectManager(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.getConfigManager().isVisualsEnabled()) {
            clearAllGlow();
            return;
        }

        int interval = Math.max(5, plugin.getConfigManager().getVisualIntervalTicks());
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        clearAllGlow();
    }

    public void reload() {
        start();
    }

    private void tick() {
        if (!plugin.getConfigManager().isVisualsEnabled()) {
            clearAllGlow();
            return;
        }

        Set<UUID> withBounty = new HashSet<UUID>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getBountyManager().hasBounty(player.getUniqueId())) {
                setGlowing(player, false);
                continue;
            }

            withBounty.add(player.getUniqueId());
            BountyData data = plugin.getBountyManager().getBountyData(player.getUniqueId());
            double amount = data != null ? data.getAmount() : 0;
            String formatted = plugin.getEconomyManager().format(amount);

            if (plugin.getConfigManager().isActionBarEnabled()) {
                String msg = plugin.getConfigManager().format(
                        plugin.getConfigManager().getActionBarFormat(),
                        "amount", formatted,
                        "player", player.getName()
                );
                sendActionBar(player, msg);
            }

            if (plugin.getConfigManager().isParticlesEnabled()) {
                spawnParticles(player);
            }

            if (plugin.getConfigManager().isGlowEnabled()) {
                setGlowing(player, true);
            } else {
                setGlowing(player, false);
            }
        }

        glowing.retainAll(withBounty);
        for (UUID uuid : new HashSet<UUID>(glowing)) {
            if (!withBounty.contains(uuid)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    setGlowing(p, false);
                } else {
                    glowing.remove(uuid);
                }
            }
        }
    }

    public void playPlaceEffects(Player target) {
        if (!plugin.getConfigManager().isVisualsEnabled()) {
            return;
        }
        if (plugin.getConfigManager().isSoundEnabled()) {
            playSound(target, true);
        }
        if (plugin.getConfigManager().isParticlesEnabled()) {
            spawnParticles(target);
            spawnParticles(target);
        }
    }

    public void playClaimEffects(Player killer, Player victim) {
        if (!plugin.getConfigManager().isVisualsEnabled()) {
            return;
        }
        if (plugin.getConfigManager().isSoundEnabled()) {
            playSound(killer, false);
            if (victim != null) {
                playSound(victim, false);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void spawnParticles(Player player) {
        Location loc = player.getLocation().add(0, 1.0, 0);
        try {
            Class<?> particleClass = Class.forName("org.bukkit.Particle");
            Object particle = Enum.valueOf((Class) particleClass, plugin.getConfigManager().getParticleType());
            Method spawn = player.getWorld().getClass().getMethod(
                    "spawnParticle", particleClass, Location.class, int.class,
                    double.class, double.class, double.class, double.class
            );
            spawn.invoke(player.getWorld(), particle, loc, plugin.getConfigManager().getParticleCount(),
                    0.35, 0.5, 0.35, 0.01);
        } catch (Throwable t) {
            try {
                player.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
            } catch (Throwable ignored) {
            }
        }
    }

    private void playSound(Player player, boolean place) {
        try {
            String name = place
                    ? plugin.getConfigManager().getSoundPlace()
                    : plugin.getConfigManager().getSoundClaim();
            Sound sound = Sound.valueOf(name);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void sendActionBar(Player player, String message) {
        try {
            Class<?> chatMessageType = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Object actionBar = Enum.valueOf((Class) chatMessageType, "ACTION_BAR");
            Class<?> baseComponent = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Class<?> textComponent = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Object component = textComponent.getConstructor(String.class).newInstance(message);
            Object array = java.lang.reflect.Array.newInstance(baseComponent, 1);
            java.lang.reflect.Array.set(array, 0, component);
            Method spigot = player.getClass().getMethod("spigot");
            Object spigotPlayer = spigot.invoke(player);
            Method sendMessage = spigotPlayer.getClass().getMethod("sendMessage", chatMessageType, array.getClass());
            sendMessage.invoke(spigotPlayer, actionBar, array);
        } catch (Throwable t) {
            // fallback silencioso
        }
    }

    private void setGlowing(Player player, boolean glow) {
        try {
            Method method = player.getClass().getMethod("setGlowing", boolean.class);
            method.invoke(player, glow);
            if (glow) {
                glowing.add(player.getUniqueId());
            } else {
                glowing.remove(player.getUniqueId());
            }
        } catch (Throwable ignored) {
        }
    }

    private void clearAllGlow() {
        for (UUID uuid : new HashSet<UUID>(glowing)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                setGlowing(p, false);
            }
        }
        glowing.clear();
    }

    public String formatTimeLeft(BountyData data) {
        if (data == null || !data.hasExpiration()) {
            return plugin.getConfigManager().getNeverExpireText();
        }
        long ms = data.getMillisUntilExpire(System.currentTimeMillis());
        return formatDuration(ms);
    }

    public static String formatDuration(long ms) {
        if (ms < 0) {
            return "-";
        }
        long totalSec = ms / 1000L;
        long hours = totalSec / 3600L;
        long minutes = (totalSec % 3600L) / 60L;
        long seconds = totalSec % 60L;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    public String formatContributors(BountyData data, int max) {
        if (data == null || data.getContributors().isEmpty()) {
            return ChatColor.GRAY + "-";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<UUID, Double> entry : data.getContributors().entrySet()) {
            if (i >= max) {
                sb.append(ChatColor.GRAY).append("...");
                break;
            }
            if (i > 0) {
                sb.append(ChatColor.DARK_GRAY).append(", ");
            }
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) {
                name = "?";
            }
            sb.append(ChatColor.WHITE).append(name)
                    .append(ChatColor.GRAY).append(" (")
                    .append(plugin.getEconomyManager().format(entry.getValue()))
                    .append(")");
            i++;
        }
        return sb.toString();
    }
}
