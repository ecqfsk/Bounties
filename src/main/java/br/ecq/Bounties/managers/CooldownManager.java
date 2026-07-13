package br.ecq.Bounties.managers;

import br.ecq.Bounties.BountiesPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final BountiesPlugin plugin;
    private final Map<UUID, Long> lastPlacement = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> lastCombat = new ConcurrentHashMap<UUID, Long>();

    public CooldownManager(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void markPlacement(UUID player) {
        lastPlacement.put(player, System.currentTimeMillis());
    }

    public long getRemainingCooldownMs(UUID player) {
        int cooldownSec = plugin.getConfigManager().getCooldownSeconds();
        if (cooldownSec <= 0) {
            return 0;
        }
        Long last = lastPlacement.get(player);
        if (last == null) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - last;
        long needed = cooldownSec * 1000L;
        return Math.max(0, needed - elapsed);
    }

    public boolean isOnCooldown(UUID player) {
        return getRemainingCooldownMs(player) > 0;
    }

    public void markCombat(UUID player) {
        lastCombat.put(player, System.currentTimeMillis());
    }

    public boolean isInCombat(UUID player) {
        int minCombat = plugin.getConfigManager().getMinCombatSeconds();
        if (minCombat <= 0) {
            return true;
        }
        Long last = lastCombat.get(player);
        if (last == null) {
            return false;
        }
        return (System.currentTimeMillis() - last) <= (minCombat * 1000L);
    }

    public long getMillisSinceCombat(UUID player) {
        Long last = lastCombat.get(player);
        if (last == null) {
            return Long.MAX_VALUE;
        }
        return System.currentTimeMillis() - last;
    }

    public void clear(UUID player) {
        lastPlacement.remove(player);
        lastCombat.remove(player);
    }
}
