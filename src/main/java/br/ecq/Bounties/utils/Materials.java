package br.ecq.Bounties.utils;

import org.bukkit.Material;

public final class Materials {

    private Materials() {
    }

    public static Material get(String... names) {
        for (String name : names) {
            Material material = match(name);
            if (material != null) {
                return material;
            }
        }
        return Material.STONE;
    }

    private static Material match(String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException ignored) {
        }

        try {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                return material;
            }
        } catch (NoSuchMethodError ignored) {
        }

        return null;
    }

    public static boolean isSkull(Material material) {
        String name = material.name();
        return name.equals("SKULL_ITEM") || name.equals("PLAYER_HEAD");
    }
}