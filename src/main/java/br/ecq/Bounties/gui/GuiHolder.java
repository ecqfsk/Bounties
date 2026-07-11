package br.ecq.Bounties.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class GuiHolder implements InventoryHolder {

    private final GuiType type;
    private final int page;
    private final UUID targetUuid;
    private final String targetName;
    private final boolean addMode;

    private Inventory inventory;

    public GuiHolder(GuiType type) {
        this(type, 0, null, null, false);
    }

    public GuiHolder(GuiType type, int page) {
        this(type, page, null, null, false);
    }

    public GuiHolder(GuiType type, UUID targetUuid, String targetName, boolean addMode) {
        this(type, 0, targetUuid, targetName, addMode);
    }

    public GuiHolder(GuiType type, int page, UUID targetUuid, String targetName, boolean addMode) {
        this.type = type;
        this.page = page;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.addMode = addMode;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public GuiType getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public boolean isAddMode() {
        return addMode;
    }
}