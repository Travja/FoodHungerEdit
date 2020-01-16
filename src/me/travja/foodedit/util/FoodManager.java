package me.travja.foodedit.util;

import de.tr7zw.nbtapi.NBTItem;
import me.travja.foodedit.Main;
import me.travja.foodedit.food.FoodType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;

public class FoodManager {

    private final long DAY = 24000L;
    private final String TIME_STRING = "creationDates";

    private HashMap<Material, Double> healthMod = new HashMap<>();
    private HashMap<Material, PotionEffect> effects = new HashMap<>();

    public ItemStack setNBT(ItemStack item) {
        if (item == null)
            return null;
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey(TIME_STRING))
            nbt.setLong(TIME_STRING, Bukkit.getWorlds().get(0).getFullTime());
        return nbt.getItem();
    }

    public boolean isFood(ItemStack item) {
        return item != null && item.getType().isEdible();
    }

    public boolean isFood(Material mat) {
        return mat.isEdible();
    }

    public boolean match(ItemStack base, ItemStack other) {
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

    public ItemStack ageFood(ItemStack item) {
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
        if(days > 2) {

        }
        if (days > 2) {
            nbt.setBoolean("gone", true);
            lore.add(FoodType.MUSTY.getLore());
        } else if (days > 1)
            lore.add(FoodType.STALE.getLore());
        else
            lore.add(FoodType.FRESH.getLore());

        lore.add("Age: " + age);//DEBUG
        im.setLore(lore);
        item.setItemMeta(im);
        nbt = new NBTItem(item);

        return nbt.getItem();
    }

    public Inventory ageFood(Inventory inv) {
        ItemStack[] contents = inv.getContents().clone();
        ItemStack[] newContents = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isFood(item))
                newContents[i] = ageFood(item);
        }
        inv.setContents(newContents);
        return inv;
    }

    public void setHealthMod(Material mat, double mod) {
        healthMod.put(mat, mod);
    }

    public double getHealthMod(Material mat) {
        if (healthMod.containsKey(mat))
            return healthMod.get(mat);
        else
            return healthMod.get(Material.AIR);
    }

    public double getHealthMod(ItemStack item) {
        return getHealthMod(item.getType());
    }

    public void setEffect(Material mat, PotionEffect eff) {
        effects.put(mat, eff);
    }

    public PotionEffect getEffect(Material mat) {
        if (effects.containsKey(mat))
            return effects.get(mat);
        else
            return effects.get(Material.AIR);
    }

    public PotionEffect getEffect(ItemStack item) {
        return getEffect(item.getType());
    }


    public void loadConfig() {
        ConfigurationSection healthSec = Main.config().getConfigurationSection("food");
        for (String key : healthSec.getValues(false).keySet()) {
            Material mat = Material.getMaterial(key);
            if (mat == null) {
                Main.log("Found a health modifier put in for '" + key + "' but couldn't tie it to a Material.");
                continue;
            }

            double health = Main.config().getDouble("food." + key);

            setHealthMod(mat, health);
        }

        if (Main.config().contains("defaultModifier")) {
            double defMod = Main.config().getDouble("defaultModifier");
            setHealthMod(Material.AIR, defMod);
        } else
            setHealthMod(Material.AIR, 2d);


        //Load Effects
        ConfigurationSection effSec = Main.config().getConfigurationSection("effects");
        for (String key : effSec.getValues(false).keySet()) {
            Material mat = Material.getMaterial(key);
            if (mat == null) {
                Main.log("Found an effect put in for '" + key + "' but couldn't tie it to a Material.");
                continue;
            }

            String[] effString = Main.config().getString("effects." + key).split(".");

            setEffect(mat, extractEffect(effString));
        }

        String defEff = Main.config().getString("defaultEffect");
        if (defEff != null && !defEff.equals(""))
            setEffect(Material.AIR, extractEffect(defEff.split(".")));
        else
            setEffect(Material.AIR, new PotionEffect(PotionEffectType.SLOW, 5, 0));
    }

    private PotionEffect extractEffect(String[] effString) {
        int dur = Integer.valueOf(effString[2]) * 20;
        int amp = Integer.valueOf(effString[1]) - 1; //By default it's already 1.

        return new PotionEffect(PotionEffectType.getByName(effString[0]), dur, amp);
    }

}
