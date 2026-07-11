package br.ecq.Bounties.listeners;

import br.ecq.Bounties.BountiesPlugin;
import br.ecq.Bounties.gui.GuiHolder;
import br.ecq.Bounties.gui.GuiManager;
import br.ecq.Bounties.gui.GuiType;
import br.ecq.Bounties.gui.PendingInput;
import br.ecq.Bounties.service.BountyResult;
import br.ecq.Bounties.service.BountyService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyGuiListener implements Listener {

    private final BountiesPlugin plugin;
    private final GuiManager guiManager;
    private final BountyService bountyService;
    private final Map<UUID, PendingInput> pendingInputs = new HashMap<UUID, PendingInput>();

    public BountyGuiListener(BountiesPlugin plugin) {
        this.plugin = plugin;
        this.guiManager = plugin.getGuiManager();
        this.bountyService = plugin.getBountyService();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof GuiHolder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() >= top.getSize()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        GuiHolder holder = (GuiHolder) top.getHolder();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == null) {
            return;
        }

        int slot = event.getRawSlot();

        switch (holder.getType()) {
            case MAIN:
                handleMainClick(player, slot);
                break;
            case LIST:
                handleListClick(player, holder, slot, clicked);
                break;
            case TOP:
                handleTopClick(player, holder, slot, clicked);
                break;
            case SELECT_PLAYER:
                handleSelectClick(player, holder, slot, clicked);
                break;
            case DETAIL:
                handleDetailClick(player, holder, slot);
                break;
            case AMOUNT:
                handleAmountClick(player, holder, slot, clicked);
                break;
            default:
                break;
        }
    }

    private void handleMainClick(Player player, int slot) {
        if (slot == 10 && player.hasPermission("bounties.list")) {
            guiManager.openList(player, 0);
        } else if (slot == 12 && player.hasPermission("bounties.set")) {
            guiManager.openPlayerSelect(player, 0);
        } else if (slot == 14 && player.hasPermission("bounties.top")) {
            guiManager.openTop(player, 0);
        }
    }

    private void handleListClick(Player player, GuiHolder holder, int slot, ItemStack clicked) {
        if (slot == GuiManager.SLOT_PREV) {
            guiManager.openList(player, holder.getPage() - 1);
        } else if (slot == GuiManager.SLOT_NEXT) {
            guiManager.openList(player, holder.getPage() + 1);
        } else if (slot == GuiManager.SLOT_BACK) {
            guiManager.openMain(player);
        } else if (slot < GuiManager.CONTENT_SLOTS) {
            String name = getSkullOwner(clicked);
            if (name != null) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(name);
                guiManager.openDetail(player, target.getUniqueId(), name);
            }
        }
    }

    private void handleTopClick(Player player, GuiHolder holder, int slot, ItemStack clicked) {
        if (slot == GuiManager.SLOT_PREV) {
            guiManager.openTop(player, holder.getPage() - 1);
        } else if (slot == GuiManager.SLOT_NEXT) {
            guiManager.openTop(player, holder.getPage() + 1);
        } else if (slot == GuiManager.SLOT_BACK) {
            guiManager.openMain(player);
        } else if (slot < GuiManager.CONTENT_SLOTS) {
            String name = getSkullOwner(clicked);
            if (name != null) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(name);
                guiManager.openDetail(player, target.getUniqueId(), name);
            }
        }
    }

    private void handleSelectClick(Player player, GuiHolder holder, int slot, ItemStack clicked) {
        if (slot == GuiManager.SLOT_PREV) {
            guiManager.openPlayerSelect(player, holder.getPage() - 1);
        } else if (slot == GuiManager.SLOT_NEXT) {
            guiManager.openPlayerSelect(player, holder.getPage() + 1);
        } else if (slot == GuiManager.SLOT_BACK) {
            guiManager.openMain(player);
        } else if (slot < GuiManager.CONTENT_SLOTS) {
            String name = getSkullOwner(clicked);
            if (name != null) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(name);
                guiManager.openDetail(player, target.getUniqueId(), name);
            }
        }
    }

    private void handleDetailClick(Player player, GuiHolder holder, int slot) {
        UUID targetUuid = holder.getTargetUuid();
        String targetName = holder.getTargetName();

        if (slot == 11 && player.hasPermission("bounties.set")) {
            guiManager.openAmount(player, targetUuid, targetName, false);
        } else if (slot == 13 && player.hasPermission("bounties.add")) {
            guiManager.openAmount(player, targetUuid, targetName, true);
        } else if (slot == 15 && (player.hasPermission("bounties.remove") || player.hasPermission("bounties.admin"))) {
            removeBounty(player, targetUuid, targetName);
        } else if (slot == 22) {
            guiManager.openMain(player);
        }
    }

    private void handleAmountClick(Player player, GuiHolder holder, int slot, ItemStack clicked) {
        UUID targetUuid = holder.getTargetUuid();
        String targetName = holder.getTargetName();
        boolean addMode = holder.isAddMode();

        if (slot == 18) {
            guiManager.openDetail(player, targetUuid, targetName);
            return;
        }

        if (slot == 22) {
            player.closeInventory();
            pendingInputs.put(player.getUniqueId(), new PendingInput(targetUuid, targetName, addMode));
            player.sendMessage(plugin.getConfigManager().getMessage("gui-custom-prompt"));
            return;
        }

        int[] presetSlots = {10, 11, 12, 13, 14, 15, 16};
        List<Double> presets = plugin.getConfigManager().getPresetAmounts();
        for (int i = 0; i < presetSlots.length && i < presets.size(); i++) {
            if (slot == presetSlots[i]) {
                processBounty(player, targetUuid, targetName, presets.get(i), addMode);
                return;
            }
        }
    }

    private void processBounty(Player player, UUID targetUuid, String targetName, double amount, boolean addMode) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        if (target.getName() == null && targetName != null) {
            target = Bukkit.getOfflinePlayer(targetName);
        }

        BountyResult result;
        if (addMode) {
            result = bountyService.addBounty(player, target, amount);
        } else {
            result = bountyService.setBounty(player, target, amount);
        }

        if (result == BountyResult.SUCCESS) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    guiManager.openDetail(player, targetUuid, targetName);
                }
            }, 1L);
        } else {
            bountyService.sendResultMessage(player, result, amount);
        }
    }

    private void removeBounty(Player player, UUID targetUuid, String targetName) {
        if (!plugin.getBountyManager().hasBounty(targetUuid)) {
            player.sendMessage(plugin.getConfigManager().format(
                    plugin.getConfigManager().getMessage("bounty-not-found"),
                    "target", targetName
            ));
            return;
        }

        double amount = plugin.getBountyManager().removeBounty(targetUuid);
        player.sendMessage(plugin.getConfigManager().format(
                plugin.getConfigManager().getMessage("bounty-removed"),
                "target", targetName,
                "amount", plugin.getEconomyManager().format(amount)
        ));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getBountyManager().save();
            }
        });

        guiManager.openList(player, 0);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingInput pending = pendingInputs.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }

        event.setCancelled(true);
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancelar") || input.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.sendMessage(plugin.getConfigManager().getMessage("gui-custom-cancel"));
                    guiManager.openAmount(player, pending.getTargetUuid(), pending.getTargetName(), pending.isAddMode());
                }
            });
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(input.replace(",", "."));
        } catch (NumberFormatException e) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.sendMessage(plugin.getConfigManager().getMessage("invalid-amount"));
                    pendingInputs.put(player.getUniqueId(), pending);
                }
            });
            return;
        }

        final double finalAmount = amount;
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                processBounty(player, pending.getTargetUuid(), pending.getTargetName(), finalAmount, pending.isAddMode());
            }
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        if (pendingInputs.containsKey(player.getUniqueId())) {
            return;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }

    private String getSkullOwner(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        if (item.getItemMeta() instanceof SkullMeta) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta.getOwner() != null) {
                return meta.getOwner();
            }
        }
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return stripColor(item.getItemMeta().getDisplayName());
        }
        return null;
    }

    private String stripColor(String text) {
        return org.bukkit.ChatColor.stripColor(text);
    }
}