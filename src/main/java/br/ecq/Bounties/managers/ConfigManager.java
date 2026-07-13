package br.ecq.Bounties.managers;

import br.ecq.Bounties.BountiesPlugin;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final BountiesPlugin plugin;

    private double minAmount;
    private double maxAmount;
    private double placementFeePercent;
    private int autoSaveInterval;
    private boolean blockSelfBounty;
    private boolean announceBounty;
    private boolean announceClaim;
    private boolean guiEnabled;
    private List<Double> presetAmounts;
    private int minKillsForTop;
    private boolean broadcastNewTopKiller;
    private String killerTagText;

    // Expiration
    private int expireAfterSeconds;
    private boolean expireRefund;
    private int expireWarnSeconds;
    private boolean refreshExpireOnAdd;
    private boolean announceExpire;

    // Cooldown / limits
    private int cooldownSeconds;
    private int maxActivePlaced;
    private double maxAmountPlaced;
    private int minCombatSeconds;

    // Refund
    private boolean refundOnRemove;
    private boolean refundOnReplace;
    private double refundPercent;
    private boolean allowOwnRemove;

    // Visuals
    private boolean visualsEnabled;
    private boolean particlesEnabled;
    private boolean actionBarEnabled;
    private boolean glowEnabled;
    private boolean soundEnabled;
    private String particleType;
    private int particleCount;
    private int visualIntervalTicks;
    private String actionBarFormat;
    private String soundPlace;
    private String soundClaim;
    private String neverExpireText;

    // History
    private int historyMaxEntries;
    private int historyDisplayLimit;

    public ConfigManager(BountiesPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        minAmount = plugin.getConfig().getDouble("settings.min-amount", 100.0);
        maxAmount = plugin.getConfig().getDouble("settings.max-amount", 0);
        placementFeePercent = plugin.getConfig().getDouble("settings.placement-fee-percent", 0);
        autoSaveInterval = plugin.getConfig().getInt("settings.auto-save-interval", 300);
        blockSelfBounty = plugin.getConfig().getBoolean("settings.block-self-bounty", true);
        announceBounty = plugin.getConfig().getBoolean("settings.announce-bounty", true);
        announceClaim = plugin.getConfig().getBoolean("settings.announce-claim", true);

        expireAfterSeconds = plugin.getConfig().getInt("settings.expire-after-seconds", 86400);
        expireRefund = plugin.getConfig().getBoolean("settings.expire-refund", true);
        expireWarnSeconds = plugin.getConfig().getInt("settings.expire-warn-seconds", 300);
        refreshExpireOnAdd = plugin.getConfig().getBoolean("settings.refresh-expire-on-add", true);
        announceExpire = plugin.getConfig().getBoolean("settings.announce-expire", true);

        cooldownSeconds = plugin.getConfig().getInt("settings.cooldown-seconds", 30);
        maxActivePlaced = plugin.getConfig().getInt("settings.max-active-placed", 5);
        maxAmountPlaced = plugin.getConfig().getDouble("settings.max-amount-placed", 0);
        minCombatSeconds = plugin.getConfig().getInt("settings.min-combat-seconds", 0);

        refundOnRemove = plugin.getConfig().getBoolean("settings.refund-on-remove", true);
        refundOnReplace = plugin.getConfig().getBoolean("settings.refund-on-replace", true);
        refundPercent = plugin.getConfig().getDouble("settings.refund-percent", 100.0);
        allowOwnRemove = plugin.getConfig().getBoolean("settings.allow-own-remove", true);

        guiEnabled = plugin.getConfig().getBoolean("gui.enabled", true);
        presetAmounts = new ArrayList<Double>();
        for (Double value : plugin.getConfig().getDoubleList("gui.preset-amounts")) {
            presetAmounts.add(value);
        }
        if (presetAmounts.isEmpty()) {
            presetAmounts.add(100.0);
            presetAmounts.add(500.0);
            presetAmounts.add(1000.0);
            presetAmounts.add(5000.0);
            presetAmounts.add(10000.0);
        }

        minKillsForTop = plugin.getConfig().getInt("killer-top.min-kills", 1);
        broadcastNewTopKiller = plugin.getConfig().getBoolean("killer-top.broadcast-new-top", true);
        killerTagText = colorize(plugin.getConfig().getString("killer-top.tag.text", "&4&lKILLER"));

        visualsEnabled = plugin.getConfig().getBoolean("visuals.enabled", true);
        particlesEnabled = plugin.getConfig().getBoolean("visuals.particles", true);
        actionBarEnabled = plugin.getConfig().getBoolean("visuals.action-bar", true);
        glowEnabled = plugin.getConfig().getBoolean("visuals.glow", false);
        soundEnabled = plugin.getConfig().getBoolean("visuals.sounds", true);
        particleType = plugin.getConfig().getString("visuals.particle-type", "FLAME");
        particleCount = plugin.getConfig().getInt("visuals.particle-count", 8);
        visualIntervalTicks = plugin.getConfig().getInt("visuals.interval-ticks", 20);
        actionBarFormat = colorize(plugin.getConfig().getString("visuals.action-bar-format",
                "&c☠ Recompensa: &f{amount}"));
        soundPlace = plugin.getConfig().getString("visuals.sound-place", "ENTITY_EXPERIENCE_ORB_PICKUP");
        soundClaim = plugin.getConfig().getString("visuals.sound-claim", "ENTITY_PLAYER_LEVELUP");
        neverExpireText = colorize(plugin.getConfig().getString("visuals.never-expire-text", "&7Nunca"));

        historyMaxEntries = plugin.getConfig().getInt("history.max-entries", 500);
        historyDisplayLimit = plugin.getConfig().getInt("history.display-limit", 10);
    }

    public String getMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, "&cMensagem nao encontrada: " + key);
        return colorize(plugin.getConfig().getString("messages.prefix", "") + message);
    }

    public String getRawMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, "");
        return colorize(message);
    }

    public List<String> getHelpMessages() {
        return colorizeList(plugin.getConfig().getStringList("messages.help"));
    }

    public String format(String message, String... replacements) {
        String result = message;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            result = result.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return result;
    }

    public String colorize(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private List<String> colorizeList(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            list.set(i, colorize(list.get(i)));
        }
        return list;
    }

    public double getMinAmount() {
        return minAmount;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public double getPlacementFeePercent() {
        return placementFeePercent;
    }

    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    public boolean isBlockSelfBounty() {
        return blockSelfBounty;
    }

    public boolean isAnnounceBounty() {
        return announceBounty;
    }

    public boolean isAnnounceClaim() {
        return announceClaim;
    }

    public int getExpireAfterSeconds() {
        return expireAfterSeconds;
    }

    public boolean isExpireRefund() {
        return expireRefund;
    }

    public int getExpireWarnSeconds() {
        return expireWarnSeconds;
    }

    public boolean isRefreshExpireOnAdd() {
        return refreshExpireOnAdd;
    }

    public boolean isAnnounceExpire() {
        return announceExpire;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public int getMaxActivePlaced() {
        return maxActivePlaced;
    }

    public double getMaxAmountPlaced() {
        return maxAmountPlaced;
    }

    public int getMinCombatSeconds() {
        return minCombatSeconds;
    }

    public boolean isRefundOnRemove() {
        return refundOnRemove;
    }

    public boolean isRefundOnReplace() {
        return refundOnReplace;
    }

    public double getRefundPercent() {
        return refundPercent;
    }

    public boolean isAllowOwnRemove() {
        return allowOwnRemove;
    }

    public boolean isGuiEnabled() {
        return guiEnabled;
    }

    public List<Double> getPresetAmounts() {
        return presetAmounts;
    }

    public String getGuiTitle(String key) {
        return colorize(plugin.getConfig().getString("gui.titles." + key, "&8Bounties"));
    }

    public String formatGuiTitle(String key, int page, int pages) {
        String title = plugin.getConfig().getString("gui.titles." + key, "&8Bounties");
        return colorize(title.replace("{page}", String.valueOf(page)).replace("{pages}", String.valueOf(pages)));
    }

    public String formatGuiTitle(String key, String player) {
        String title = plugin.getConfig().getString("gui.titles." + key, "&8Bounties");
        return colorize(title.replace("{player}", player));
    }

    public String getGuiItemName(String key) {
        return colorize(plugin.getConfig().getString("gui.items." + key + ".name", "&7Item"));
    }

    public List<String> getGuiItemLore(String key) {
        return colorizeList(new ArrayList<String>(plugin.getConfig().getStringList("gui.items." + key + ".lore")));
    }

    public String getGuiText(String key) {
        return colorize(plugin.getConfig().getString("gui.text." + key, ""));
    }

    public int getMinKillsForTop() {
        return minKillsForTop;
    }

    public boolean isBroadcastNewTopKiller() {
        return broadcastNewTopKiller;
    }

    public String getKillerTagText() {
        return killerTagText;
    }

    public boolean isVisualsEnabled() {
        return visualsEnabled;
    }

    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }

    public boolean isActionBarEnabled() {
        return actionBarEnabled;
    }

    public boolean isGlowEnabled() {
        return glowEnabled;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public String getParticleType() {
        return particleType;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public int getVisualIntervalTicks() {
        return visualIntervalTicks;
    }

    public String getActionBarFormat() {
        return actionBarFormat;
    }

    public String getSoundPlace() {
        return soundPlace;
    }

    public String getSoundClaim() {
        return soundClaim;
    }

    public String getNeverExpireText() {
        return neverExpireText;
    }

    public int getHistoryMaxEntries() {
        return historyMaxEntries;
    }

    public int getHistoryDisplayLimit() {
        return historyDisplayLimit;
    }

    public long computeExpiresAt() {
        if (expireAfterSeconds <= 0) {
            return 0L;
        }
        return System.currentTimeMillis() + (expireAfterSeconds * 1000L);
    }
}
