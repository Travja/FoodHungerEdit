package me.travja.foodedit.food;

import org.bukkit.ChatColor;

public enum FoodType {

    FRESH("&7[&bFresh&7]"),
    STALE("&7[&9Stale&7]"),
    MUSTY("&7[&1Musty&7]");

    private String lore;
    FoodType(String lore) {
        this.lore = ChatColor.translateAlternateColorCodes('&', lore);
    }

    public String getLore() {
        return this.lore;
    }

}
