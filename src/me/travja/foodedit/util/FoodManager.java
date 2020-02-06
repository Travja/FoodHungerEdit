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

    private final long DAY = 24000L;
    public static final String TIME_STRING = "creationDates";

    private HashMap<Material, Integer> foodMod = new HashMap<>();
    private HashMap<Material, PotionEffect> effects = new HashMap<>();
    private ArrayList<PotionEffect> fullEffects = new ArrayList<>();
    private PotionEffect quickFill;

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
            if (name.equals(title)) {
                NBTItem nbt1 = new NBTItem(base);
                NBTItem nbt2 = new NBTItem(other);
                if (nbt1.hasKey(TIME_STRING) && nbt2.hasKey(TIME_STRING)) {

                    return getDaysOld(base) == getDaysOld(other);
                }
            }
        }
        return false;
    }

    public int getDaysOld(ItemStack item) {
        if (item == null)
            return 0;
        NBTItem nbt = new NBTItem(setNBT(item)); //Just in case it hasn't already been given NBT, lets add it before we do anything else
        if (nbt.hasKey("gone") && nbt.getBoolean("gone")) //If it's already aged past 3 days, there is no need to do extra logic
            return 3;
        World world = Bukkit.getWorlds().get(0);

        long created = nbt.getLong(TIME_STRING);
        long now = world.getFullTime();
        long age = now - created;
        double days = ((double) age / (double) DAY);

        return (int) days;
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
        if (nbt.hasKey("gone") && nbt.getBoolean("gone")) //If it's already aged past 3 days, there is no need to do extra logic
            return item;
        item = nbt.getItem(); // Update our item to reflect updated NBT

        World world = Bukkit.getWorlds().get(0); //DEBUG
        long created = nbt.getLong(TIME_STRING);
        long now = world.getFullTime();
        long age = now - created; //END DEBUG

        int days = getDaysOld(item);

        ItemMeta im = item.getItemMeta();
        ArrayList<String> lore = im.hasLore() ? (ArrayList<String>) im.getLore() : new ArrayList<>();
        for (FoodType type : FoodType.values()) { //Clear out the lore, just in case it's already there so we don't get gigantic lore
            lore.remove(type.getLore());
        }
        for (String l : (ArrayList<String>) lore.clone()) { //DEBUG
            if (l.startsWith("Age: "))
                lore.remove(l);
        }

        if (days >= 3) {
            item.setType(Material.ROTTEN_FLESH);
            im = item.getItemMeta();
            im.setDisplayName(ChatColor.DARK_RED + "Crow Food");
            lore.add(FoodType.ROTTEN.getLore());
        } else if (days == 2) {
            lore.add(FoodType.MUSTY.getLore());
        } else if (days == 1)
            lore.add(FoodType.STALE.getLore());
        else
            lore.add(FoodType.FRESH.getLore());

        lore.add("Age: " + age);//DEBUG
        im.setLore(lore);
        item.setItemMeta(im);
        nbt = new NBTItem(item);
        if (days > 3)
            nbt.setBoolean("gone", true);

        return nbt.getItem();
    }

    private ArrayList<Inventory> aged = new ArrayList<>();

    public Inventory ageFood(Inventory inv) {
        if (aged.contains(inv))
            return inv;
        ItemStack[] contents = inv.getContents().clone();
        ItemStack[] newContents = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isFood(item))
                newContents[i] = ageFood(item);
        }
        inv.setContents(newContents);

        aged.add(inv); //Limit inventory updates to every 10 seconds
        new BukkitRunnable() {
            public void run() {
                aged.remove(inv);
            }
        }.runTaskLater(Main.getInstance(), 200L);

        return inv;
    }

    public void setHealthMod(Material mat, int mod) {
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

            setHealthMod(mat, health);
        }

        if (Main.config().contains("defaultModifier")) {
            int defMod = Main.config().getInt("defaultModifier");
            setHealthMod(Material.AIR, defMod);
        } else
            setHealthMod(Material.AIR, 2);


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
