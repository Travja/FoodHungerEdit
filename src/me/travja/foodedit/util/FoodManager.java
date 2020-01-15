package me.travja.foodedit.util;

import de.tr7zw.nbtapi.NBTItem;
import me.travja.foodedit.food.FoodType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;

public class FoodManager {

    private static final long DAY = 24000L;
    private static final String TIME_STRING = "creationDates";

    public static ItemStack setNBT(ItemStack item) {
        if (item == null)
            return null;
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey(TIME_STRING))
            nbt.setLong(TIME_STRING, Bukkit.getWorlds().get(0).getFullTime());
        return nbt.getItem();
    }

    public static boolean isFood(ItemStack item) {
        return item != null && item.getType().isEdible();
    }

    public static boolean isFood(Material mat) {
        return mat.isEdible();
    }

    public static boolean match(ItemStack base, ItemStack other) {
        if (base == null)
            return false;
        ItemMeta im = base.hasItemMeta() ? base.getItemMeta() : null;
        String title = im != null ? im.hasDisplayName() ? im.getDisplayName() : im.getLocalizedName() : null;
        Material type = base.getType();
        if (other != null && type == other.getType()) {
            boolean hasMeta = other.hasItemMeta();
            boolean hasName = other.getItemMeta().hasDisplayName();
            String name = hasMeta ? hasName ? im.getDisplayName() : im.getLocalizedName() : null;
            if (name.equals(title))
                return true;
        }
        return false;
    }

    public static ItemStack ageFood(ItemStack item) {
        if (item == null)
            return null;
        NBTItem nbt = new NBTItem(setNBT(item)); //Just in case it hasn't already been given NBT, lets add it before we do anything else
        if (nbt.hasKey("gone") && nbt.getBoolean("gone")) //If it's already aged past 3 days, there is no need to do extra logic
            return item;
        World world = Bukkit.getWorlds().get(0);
        item = nbt.getItem(); // Update our item to reflect updated NBT

        long created = nbt.getLong(TIME_STRING);
        long now = world.getFullTime();
        long age = now - created;
        double days = ((double) age / (double) DAY);

        ItemMeta im = item.getItemMeta();
        ArrayList<String> lore = new ArrayList<>();
        if (days > 2) {
            nbt.setBoolean("gone", true);
            lore.add(FoodType.MUSTY.getLore());
        } else if (days > 1)
            lore.add(FoodType.STALE.getLore());
        else
            lore.add(FoodType.FRESH.getLore());

        lore.add("Age: " + age);
        im.setLore(lore);
        item.setItemMeta(im);
        nbt = new NBTItem(item);

        return nbt.getItem();
    }

    public static Inventory ageFood(Inventory inv) {
        ItemStack[] contents = inv.getContents().clone();
        ItemStack[] newContents = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isFood(item))
                newContents[i] = FoodManager.ageFood(item);
        }
        inv.setContents(newContents);
        return inv;
    }

}
