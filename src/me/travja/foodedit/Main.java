package me.travja.foodedit;

import me.travja.foodedit.listeners.FoodListener;
import me.travja.foodedit.listeners.ItemListener;
import me.travja.foodedit.util.FoodManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

public class Main extends JavaPlugin {

    private static FileConfiguration config;
    private static Logger log;
    private static Main instance;
    private static FoodManager fm;

    public void onEnable() {
        config = this.getConfig();
        if (!new File(this.getDataFolder(), "config.yml").exists()) {
            this.saveDefaultConfig();
            config.options().copyDefaults(true);
        }
        instance = this;
        fm = new FoodManager();

        log = this.getLogger();

        registerEvents();

        fm.loadConfig();

        log.info("Plugin loaded.");

        log.info("Edible items include: ");
        for (Material mat : Material.values()) {
            if (getFoodManager().isFood(mat))
                log.info(mat.name());
        }
    }

    public void onDisable() {


        log.info("Plugin disabled.");
    }

    public static Main getInstance() {
        return instance;
    }

    public static FoodManager getFoodManager() {
        return fm;
    }

    public static void log(String toLog) {
        log.info(toLog);
    }

    public static FileConfiguration config() {
        return config;
    }

    public void registerEvents() {
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new ItemListener(), this);
        pm.registerEvents(new FoodListener(), this);
    }


}
