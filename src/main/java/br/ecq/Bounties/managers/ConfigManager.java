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
}