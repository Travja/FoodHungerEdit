package me.travja.foodedit.util;

import de.tr7zw.nbtapi.NBTItem;
import me.travja.foodedit.Main;
import me.travja.foodedit.food.FoodType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FoodManager {

    public static boolean debug = Main.config().getBoolean("debug");

    private final long TIME_UNIT = Main.config().getLong("stageTime");
    private final boolean REMOVE_DECAY = Main.config().getBoolean("removeDecay");
    public static final String TIME_STRING = "creationDates";

    private HashMap<Material, Integer> foodMod = new HashMap<>();
    private HashMap<Material, PotionEffect> effects = new HashMap<>();
    private ArrayList<PotionEffect> fullEffects = new ArrayList<>(),
            glisteringEffects = new ArrayList<>();
    private PotionEffect quickFill;
    private ArrayList<Material> foods = new ArrayList<>();

    public ItemStack setNBT(ItemStack item) {
        if (item == null)
            return null;
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey(TIME_STRING))
            nbt.setLong(TIME_STRING, Bukkit.getWorlds().get(0).getFullTime());
        return nbt.getItem();
    }

    public boolean isFood(ItemStack item) {
        return item != null && (item.getType().isEdible() || foods.contains(item.getType()));
    }

    public boolean isFood(Material mat) {
        return mat.isEdible() || foods.contains(mat);
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
            ItemMeta im2 = hasMeta ? other.getItemMeta() : null;
            String name = hasMeta ? hasName ? im2.getDisplayName() : im2.getLocalizedName() : null;
            if (name.equals(title)) {
                NBTItem nbt1 = new NBTItem(base);
                NBTItem nbt2 = new NBTItem(other);
                if (nbt1.hasKey(TIME_STRING) && nbt2.hasKey(TIME_STRING)) {

                    return getStage(base) == getStage(other);
                }
            }
        }
        return false;
    }

    public int getStage(ItemStack item) {
        if (item == null)
            return 0;
        NBTItem nbt = new NBTItem(item); //Just in case it hasn't already been given NBT, lets add it before we do anything else
        if (!nbt.hasKey(TIME_STRING))
            return -1;
        if (nbt.hasKey("gone") && nbt.getBoolean("gone")) //If it's already aged past 3 stages, there is no need to do extra logic
            return 4;
        World world = Bukkit.getWorlds().get(0);

        long created = nbt.getLong(TIME_STRING);
        long now = world.getFullTime();
        long age = now - created;
        double units = ((double) age / (double) TIME_UNIT);

        return (int) units;
    }

    public long getAge(ItemStack item) {
        if (item == null)
            return 0;
        NBTItem nbt = new NBTItem(setNBT(item)); //Just in case it hasn't already been given NBT, lets add it before we do anything else

        return Bukkit.getWorlds().get(0).getFullTime() - nbt.getLong(TIME_STRING);
    }

    public long getOldestStamp(ItemStack one, ItemStack two) {
        if (getAge(one) >= getAge(two))
            return getTimeStamp(one);
        else
            return getTimeStamp(two);
    }

    public long getTimeStamp(ItemStack item) {
        NBTItem nbt = new NBTItem(setNBT(item));

        return nbt.getLong(TIME_STRING);
    }

    public ItemStack ageFood(ItemStack item) {
        if (item == null)
            return null;
        NBTItem nbt = new NBTItem(setNBT(item)); //Just in case it hasn't already been given NBT, lets add it before we do anything else
        if (nbt.hasKey("gone") && nbt.getBoolean("gone") && !debug) //If it's already aged past 3 stages, there is no need to do extra logic
            return item;
        item = nbt.getItem(); // Update our item to reflect updated NBT


        int stage = getStage(item);

        if (stage >= 4 && REMOVE_DECAY) {
            item.setAmount(0);
            return item;
        }

        ItemMeta im = item.getItemMeta();
        ArrayList<String> lore = im.hasLore() ? (ArrayList<String>) im.getLore() : new ArrayList<>();
        if (lore.contains(getLore(stage)) && !debug)//If the lore is already set to reflect the current state, there's no need to update it.
            return item;

        for (FoodType type : FoodType.values()) { //Clear out the lore, just in case it's already there so we don't get gigantic lore
            lore.remove(type.getLore());
        }
        if (debug) {
            for (String l : (ArrayList<String>) lore.clone()) { //DEBUG
                if (l.startsWith("Age: "))
                    lore.remove(l);
            }

            World world = Bukkit.getWorlds().get(0); //DEBUG
            long created = nbt.getLong(TIME_STRING);
            long now = world.getFullTime();
            long age = now - created;
            lore.add("Age: " + age);//DEBUG
        }
        lore.add(getLore(stage));

        if (stage >= 3) {
            item.setType(Material.ROTTEN_FLESH);
            im = item.getItemMeta();
            im.setDisplayName(ChatColor.DARK_RED + "Crow Food");
        }


        im.setLore(lore);
        item.setItemMeta(im);
        nbt = new NBTItem(item);
        if (stage > 3)
            nbt.setBoolean("gone", true);

        return nbt.getItem();
    }

    private String getLore(int days) {
        if (days >= 3)
            return FoodType.ROTTEN.getLore();
        else if (days == 2)
            return FoodType.MUSTY.getLore();
        else if (days == 1)
            return FoodType.STALE.getLore();
        else
            return FoodType.FRESH.getLore();
    }

    private ArrayList<Inventory> aged = new ArrayList<>();

    public Inventory ageFood(Inventory inv) {
        if (aged.contains(inv))
            return inv;
        ItemStack[] contents = inv.getContents().clone();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isFood(item)) {
                ItemStack initial = item.clone();
                ItemStack it = ageFood(item);
                if (!initial.equals(it) || debug) //Only update the items if the day difference has changed.
                    inv.setItem(i, it);
            }
        }

        aged.add(inv); //Limit inventory updates to every 10 seconds
        new BukkitRunnable() {
            public void run() {
                aged.remove(inv);
            }
        }.runTaskLater(Main.getInstance(), 200L);

        return inv;
    }

    public void setFoodMod(Material mat, int mod) {
        foodMod.put(mat, mod);
    }

    public int getFoodMod(Material mat) {
        if (foodMod.containsKey(mat))
            return foodMod.get(mat);
        else
            return foodMod.get(Material.AIR);
    }

    public int getFoodMod(ItemStack item) {
        return getFoodMod(item.getType());
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

    public ArrayList<PotionEffect> getGlisteringEffects() {
        return (ArrayList<PotionEffect>) glisteringEffects.clone();
    }

    public ArrayList<PotionEffect> getFullEffects() {
        return (ArrayList<PotionEffect>) fullEffects.clone();
    }

    public PotionEffect getQuickFill() {
        return quickFill;
    }

    public void setQuickFill(PotionEffect quickFill) {
        this.quickFill = quickFill;
    }

    public void loadConfig() {
        ConfigurationSection healthSec = Main.config().getConfigurationSection("food");
        for (String key : healthSec.getValues(false).keySet()) {
            Material mat = Material.getMaterial(key);
            if (mat == null) {
                Main.log("Found a health modifier put in for '" + key + "' but couldn't tie it to a Material.");
                continue;
            }

            int health = Main.config().getInt("food." + key);
            foods.add(mat);

            setFoodMod(mat, health);
        }

        if (Main.config().contains("defaultModifier")) {
            int defMod = Main.config().getInt("defaultModifier");
            setFoodMod(Material.AIR, defMod);
        } else
            setFoodMod(Material.AIR, 2);


        //Load Effects
        ConfigurationSection effSec = Main.config().getConfigurationSection("effects");
        for (String key : effSec.getValues(false).keySet()) {
            Material mat = Material.getMaterial(key);
            if (mat == null) {
                Main.log("Found an effect put in for '" + key + "' but couldn't tie it to a Material.");
                continue;
            }

            String[] effString = Main.config().getString("effects." + key).split("\\.");

            try {
                setEffect(mat, extractEffect(effString));
            } catch (Exception e) {
                Main.log("Tried to parse effect '" + Arrays.toString(effString) + "' for " + key + " but couldn't translate it to an effect.");
            }
        }

        String defEff = Main.config().getString("defaultEffect");
        if (defEff != null && !defEff.equals("")) {
            try {
                setEffect(Material.AIR, extractEffect(defEff.split("\\.")));
            } catch (Exception e) {
                Main.log("Tried to parse effect '" + Arrays.toString(defEff.split("\\.")) + "' for DEFAULT but couldn't translate it to an effect.");
            }
        } else
            setEffect(Material.AIR, new PotionEffect(PotionEffectType.SLOW, 5, 0));

        List<String> fullEff = Main.config().getStringList("fullEffects");
        if (!fullEff.isEmpty()) {
            for (String eff : fullEff) {
                try {
                    fullEffects.add(extractEffect(eff.split("\\.")));
                } catch (Exception e) {
                    Main.log("Tried to parse effect '" + Arrays.toString(defEff.split("\\.")) + "' for FULL EFFECTS but couldn't translate it to an effect.");
                }
            }
        }
        List<String> melon = Main.config().getStringList("effects.GLISTERING_MELON_SLICE");
        if (!melon.isEmpty()) {
            for (String eff : melon) {
                try {
                    glisteringEffects.add(extractEffect(eff.split("\\.")));
                } catch (Exception e) {
                    Main.log("Tried to parse effect '" + Arrays.toString(defEff.split("\\.")) + "' for GLISTERING_MELON_SLICE but couldn't translate it to an effect.");
                }
            }
        }

        String quickEff = Main.config().getString("quickFill");
        try {
            setQuickFill(extractEffect(quickEff.split("\\.")));
        } catch (Exception e) {
            Main.log("Tried to parse effect '" + Arrays.toString(defEff.split("\\.")) + "' for FULL EFFECTS but couldn't translate it to an effect.");
        }
    }

    private PotionEffect extractEffect(String[] effString) {
        int dur = Integer.valueOf(effString[2]) * 20; //Translate to ticks
        int amp = Integer.valueOf(effString[1]) - 1; //By default it's already 1.

        return new PotionEffect(PotionEffectType.getByName(effString[0]), dur, amp);
    }

}
