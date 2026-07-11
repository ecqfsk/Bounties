package br.ecq.Bounties.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(new ItemStack(material));
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        meta.setDisplayName(colorize(name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        List<String> lore = new ArrayList<String>();
        for (String line : lines) {
            lore.add(colorize(line));
        }
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        List<String> lore = new ArrayList<String>();
        for (String line : lines) {
            lore.add(colorize(line));
        }
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder durability(short durability) {
        try {
            item.setDurability(durability);
        } catch (NoSuchMethodError ignored) {
        }
        return this;
    }

    public ItemBuilder skullOwner(String owner) {
        if (meta instanceof SkullMeta && owner != null) {
            SkullMeta skull = (SkullMeta) meta;
            try {
                skull.setOwner(owner);
            } catch (Exception ignored) {
            }
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack filler() {
        Material pane = Materials.get("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
        ItemBuilder builder = new ItemBuilder(pane).name(" ");
        if (pane.name().equals("STAINED_GLASS_PANE")) {
            builder.durability((short) 7);
        }
        return builder.build();
    }

    public static ItemStack playerHead(String name, String displayName, String... lore) {
        Material skull = Materials.get("PLAYER_HEAD", "SKULL_ITEM");
        ItemBuilder builder = new ItemBuilder(skull).name(displayName).lore(lore).skullOwner(name);
        if (skull.name().equals("SKULL_ITEM")) {
            builder.durability((short) 3);
        }
        return builder.build();
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}