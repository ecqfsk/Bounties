package br.ecq.Bounties.gui;

import br.ecq.Bounties.BountiesPlugin;
import br.ecq.Bounties.model.BountyData;
import br.ecq.Bounties.model.PlayerStats;
import br.ecq.Bounties.utils.ItemBuilder;
import br.ecq.Bounties.utils.Materials;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiManager {

    public static final int LIST_SIZE = 54;
    public static final int MAIN_SIZE = 27;
    public static final int AMOUNT_SIZE = 27;
    public static final int CONTENT_SLOTS = 28;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_BACK = 49;
    public static final int SLOT_NEXT = 53;

    private final BountiesPlugin plugin;

    public GuiManager(BountiesPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        GuiHolder holder = new GuiHolder(GuiType.MAIN);
        String title = plugin.getConfigManager().getGuiTitle("main");
        Inventory inv = Bukkit.createInventory(holder, MAIN_SIZE, title);
        holder.setInventory(inv);

        fillBorder(inv, MAIN_SIZE);

        inv.setItem(10, new ItemBuilder(Materials.get("BOOK", "WRITABLE_BOOK"))
                .name(plugin.getConfigManager().getGuiItemName("list"))
                .lore(plugin.getConfigManager().getGuiItemLore("list"))
                .build());

        inv.setItem(12, new ItemBuilder(Materials.get("IRON_SWORD", "DIAMOND_SWORD"))
                .name(plugin.getConfigManager().getGuiItemName("place"))
                .lore(plugin.getConfigManager().getGuiItemLore("place"))
                .build());

        inv.setItem(14, new ItemBuilder(Material.GOLD_INGOT)
                .name(plugin.getConfigManager().getGuiItemName("top"))
                .lore(plugin.getConfigManager().getGuiItemLore("top"))
                .build());

        double ownBounty = plugin.getBountyManager().getBounty(player.getUniqueId());
        PlayerStats stats = plugin.getHistoryManager().getStats(player.getUniqueId());
        List<String> infoLore = new ArrayList<String>(plugin.getConfigManager().getGuiItemLore("info"));
        for (int i = 0; i < infoLore.size(); i++) {
            infoLore.set(i, infoLore.get(i)
                    .replace("{amount}", plugin.getEconomyManager().format(ownBounty))
                    .replace("{balance}", plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player)))
                    .replace("{earned}", plugin.getEconomyManager().format(stats.getEarned()))
                    .replace("{spent}", plugin.getEconomyManager().format(stats.getSpent()))
                    .replace("{claims}", String.valueOf(stats.getClaims())));
        }

        inv.setItem(16, ItemBuilder.playerHead(
                player.getName(),
                plugin.getConfigManager().getGuiItemName("info"),
                infoLore.toArray(new String[0])
        ));

        player.openInventory(inv);
    }

    public void openList(Player player, int page) {
        openPagedBounties(player, GuiType.LIST, page, plugin.getBountyManager().getAllBounties());
    }

    public void openTop(Player player, int page) {
        List<Map.Entry<UUID, Double>> top = plugin.getBountyManager().getTopBounties(0);
        openPagedEntries(player, GuiType.TOP, page, top);
    }

    private void openPagedBounties(Player player, GuiType type, int page, Map<UUID, Double> bounties) {
        List<Map.Entry<UUID, Double>> entries = new ArrayList<Map.Entry<UUID, Double>>(bounties.entrySet());
        openPagedEntries(player, type, page, entries);
    }

    private void openPagedEntries(Player player, GuiType type, int page, List<Map.Entry<UUID, Double>> entries) {
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) CONTENT_SLOTS));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));

        GuiHolder holder = new GuiHolder(type, safePage);
        String titleKey = type == GuiType.TOP ? "top" : "list";
        String title = plugin.getConfigManager().formatGuiTitle(titleKey, safePage + 1, totalPages);
        Inventory inv = Bukkit.createInventory(holder, LIST_SIZE, title);
        holder.setInventory(inv);

        fillNavigation(inv);

        int start = safePage * CONTENT_SLOTS;
        int end = Math.min(start + CONTENT_SLOTS, entries.size());

        for (int i = start; i < end; i++) {
            Map.Entry<UUID, Double> entry = entries.get(i);
            OfflinePlayer target = Bukkit.getOfflinePlayer(entry.getKey());
            String name = target.getName() != null ? target.getName() : entry.getKey().toString();
            String formatted = plugin.getEconomyManager().format(entry.getValue());
            BountyData bountyData = plugin.getBountyManager().getBountyData(entry.getKey());
            String expires = plugin.getVisualEffectManager().formatTimeLeft(bountyData);
            String contributors = plugin.getVisualEffectManager().formatContributors(bountyData, 3);

            List<String> lore = new ArrayList<String>();
            if (type == GuiType.TOP) {
                int rank = i + 1;
                for (String line : plugin.getConfigManager().getGuiItemLore("top-entry")) {
                    lore.add(line
                            .replace("{rank}", String.valueOf(rank))
                            .replace("{amount}", formatted)
                            .replace("{time}", expires)
                            .replace("{contributors}", contributors));
                }
            } else {
                for (String line : plugin.getConfigManager().getGuiItemLore("list-entry")) {
                    lore.add(line
                            .replace("{amount}", formatted)
                            .replace("{time}", expires)
                            .replace("{contributors}", contributors));
                }
            }
            lore.add("");
            lore.addAll(plugin.getConfigManager().getGuiItemLore("click-detail"));

            inv.setItem(i - start, ItemBuilder.playerHead(
                    name,
                    plugin.getConfigManager().format(
                            plugin.getConfigManager().getGuiItemName(type == GuiType.TOP ? "top-entry" : "list-entry"),
                            "player", name,
                            "rank", String.valueOf(i + 1),
                            "amount", formatted
                    ),
                    lore.toArray(new String[0])
            ));
        }

        if (safePage > 0) {
            inv.setItem(SLOT_PREV, navigationItem("prev", safePage));
        }
        if (safePage < totalPages - 1) {
            inv.setItem(SLOT_NEXT, navigationItem("next", safePage + 2));
        }

        player.openInventory(inv);
    }

    public void openPlayerSelect(Player player, int page) {
        List<Player> online = new ArrayList<Player>(Bukkit.getOnlinePlayers());
        online.remove(player);

        int totalPages = Math.max(1, (int) Math.ceil(online.size() / (double) CONTENT_SLOTS));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));

        GuiHolder holder = new GuiHolder(GuiType.SELECT_PLAYER, safePage);
        String title = plugin.getConfigManager().formatGuiTitle("select", safePage + 1, totalPages);
        Inventory inv = Bukkit.createInventory(holder, LIST_SIZE, title);
        holder.setInventory(inv);

        fillNavigation(inv);

        int start = safePage * CONTENT_SLOTS;
        int end = Math.min(start + CONTENT_SLOTS, online.size());

        for (int i = start; i < end; i++) {
            Player target = online.get(i);
            double bounty = plugin.getBountyManager().getBounty(target.getUniqueId());
            List<String> lore = new ArrayList<String>();
            for (String line : plugin.getConfigManager().getGuiItemLore("player-entry")) {
                lore.add(line.replace("{amount}", plugin.getEconomyManager().format(bounty)));
            }
            lore.add("");
            lore.addAll(plugin.getConfigManager().getGuiItemLore("click-select"));

            inv.setItem(i - start, ItemBuilder.playerHead(
                    target.getName(),
                    plugin.getConfigManager().format(
                            plugin.getConfigManager().getGuiItemName("player-entry"),
                            "player", target.getName()
                    ),
                    lore.toArray(new String[0])
            ));
        }

        if (online.isEmpty()) {
            inv.setItem(22, new ItemBuilder(Materials.get("BARRIER", "REDSTONE_BLOCK"))
                    .name(plugin.getConfigManager().getGuiItemName("empty"))
                    .lore(plugin.getConfigManager().getGuiItemLore("empty"))
                    .build());
        }

        if (safePage > 0) {
            inv.setItem(SLOT_PREV, navigationItem("prev", safePage));
        }
        if (safePage < totalPages - 1) {
            inv.setItem(SLOT_NEXT, navigationItem("next", safePage + 2));
        }

        player.openInventory(inv);
    }

    public void openDetail(Player viewer, UUID targetUuid, String targetName) {
        GuiHolder holder = new GuiHolder(GuiType.DETAIL, targetUuid, targetName, false);
        String title = plugin.getConfigManager().formatGuiTitle("detail", targetName);
        Inventory inv = Bukkit.createInventory(holder, AMOUNT_SIZE, title);
        holder.setInventory(inv);

        fillBorder(inv, AMOUNT_SIZE);

        double bounty = plugin.getBountyManager().getBounty(targetUuid);
        int rank = plugin.getBountyManager().getRank(targetUuid);
        BountyData bountyData = plugin.getBountyManager().getBountyData(targetUuid);
        String expires = plugin.getVisualEffectManager().formatTimeLeft(bountyData);
        String contributors = plugin.getVisualEffectManager().formatContributors(bountyData, 5);

        List<String> headLore = new ArrayList<String>();
        for (String line : plugin.getConfigManager().getGuiItemLore("detail-head")) {
            headLore.add(line
                    .replace("{amount}", plugin.getEconomyManager().format(bounty))
                    .replace("{rank}", rank > 0 ? String.valueOf(rank) : "-")
                    .replace("{time}", expires)
                    .replace("{contributors}", contributors));
        }

        inv.setItem(4, ItemBuilder.playerHead(
                targetName,
                plugin.getConfigManager().format(
                        plugin.getConfigManager().getGuiItemName("detail-head"),
                        "player", targetName
                ),
                headLore.toArray(new String[0])
        ));

        if (viewer.hasPermission("bounties.set")) {
            inv.setItem(11, new ItemBuilder(Materials.get("EMERALD", "EMERALD_BLOCK"))
                    .name(plugin.getConfigManager().getGuiItemName("set"))
                    .lore(plugin.getConfigManager().getGuiItemLore("set"))
                    .build());
        }

        if (viewer.hasPermission("bounties.add")) {
            inv.setItem(13, new ItemBuilder(Material.GOLD_INGOT)
                    .name(plugin.getConfigManager().getGuiItemName("add"))
                    .lore(plugin.getConfigManager().getGuiItemLore("add"))
                    .build());
        }

        boolean canRemove = viewer.hasPermission("bounties.remove") || viewer.hasPermission("bounties.admin");
        boolean canRemoveOwn = plugin.getConfigManager().isAllowOwnRemove()
                && viewer.hasPermission("bounties.remove.own")
                && bountyData != null
                && bountyData.hasContributor(viewer.getUniqueId());
        if ((canRemove || canRemoveOwn) && plugin.getBountyManager().hasBounty(targetUuid)) {
            inv.setItem(15, new ItemBuilder(Materials.get("BARRIER", "REDSTONE_BLOCK"))
                    .name(plugin.getConfigManager().getGuiItemName("remove"))
                    .lore(plugin.getConfigManager().getGuiItemLore("remove"))
                    .build());
        }

        inv.setItem(22, navigationItem("back", 0));
        viewer.openInventory(inv);
    }

    public void openAmount(Player player, UUID targetUuid, String targetName, boolean addMode) {
        GuiHolder holder = new GuiHolder(GuiType.AMOUNT, targetUuid, targetName, addMode);
        String title = plugin.getConfigManager().formatGuiTitle("amount", targetName);
        Inventory inv = Bukkit.createInventory(holder, AMOUNT_SIZE, title);
        holder.setInventory(inv);

        fillBorder(inv, AMOUNT_SIZE);

        List<String> headLore = new ArrayList<String>();
        double current = plugin.getBountyManager().getBounty(targetUuid);
        String mode = addMode
                ? plugin.getConfigManager().getGuiText("mode-add")
                : plugin.getConfigManager().getGuiText("mode-set");

        for (String line : plugin.getConfigManager().getGuiItemLore("amount-head")) {
            headLore.add(line
                    .replace("{player}", targetName)
                    .replace("{current}", plugin.getEconomyManager().format(current))
                    .replace("{mode}", mode)
                    .replace("{balance}", plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player))));
        }

        inv.setItem(4, ItemBuilder.playerHead(
                targetName,
                plugin.getConfigManager().getGuiItemName("amount-head"),
                headLore.toArray(new String[0])
        ));

        List<Double> presets = plugin.getConfigManager().getPresetAmounts();
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < presets.size() && i < slots.length; i++) {
            double amount = presets.get(i);
            inv.setItem(slots[i], new ItemBuilder(Material.PAPER)
                    .name(plugin.getConfigManager().format(
                            plugin.getConfigManager().getGuiItemName("preset"),
                            "amount", plugin.getEconomyManager().format(amount)
                    ))
                    .lore(plugin.getConfigManager().getGuiItemLore("preset"))
                    .amount(Math.max(1, Math.min(64, (int) amount)))
                    .build());
        }

        inv.setItem(22, new ItemBuilder(Materials.get("ANVIL", "NAME_TAG"))
                .name(plugin.getConfigManager().getGuiItemName("custom"))
                .lore(plugin.getConfigManager().getGuiItemLore("custom"))
                .build());

        inv.setItem(18, navigationItem("back", 0));
        player.openInventory(inv);
    }

    private void fillNavigation(Inventory inv) {
        ItemStack filler = ItemBuilder.filler();
        for (int i = CONTENT_SLOTS; i < LIST_SIZE; i++) {
            if (i != SLOT_PREV && i != SLOT_BACK && i != SLOT_NEXT) {
                inv.setItem(i, filler);
            }
        }
        inv.setItem(SLOT_BACK, navigationItem("back", 0));
    }

    private void fillBorder(Inventory inv, int size) {
        ItemStack filler = ItemBuilder.filler();
        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }
    }

    private ItemStack navigationItem(String key, int page) {
        Material material;
        if (key.equals("prev") || key.equals("next")) {
            material = Materials.get("ARROW");
        } else if (key.equals("back")) {
            material = Materials.get("BARRIER", "REDSTONE_BLOCK");
        } else {
            material = Material.STONE;
        }

        List<String> lore = new ArrayList<String>();
        for (String line : plugin.getConfigManager().getGuiItemLore(key)) {
            lore.add(line.replace("{page}", String.valueOf(page)));
        }

        return new ItemBuilder(material)
                .name(plugin.getConfigManager().getGuiItemName(key))
                .lore(lore)
                .build();
    }
}