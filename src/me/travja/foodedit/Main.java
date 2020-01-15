package me.travja.foodedit;

import me.travja.foodedit.listeners.ItemListener;
import me.travja.foodedit.util.FoodManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public class Main extends JavaPlugin {

    private FileConfiguration config;
    private Logger log;
    private static Main instance;

    public void onEnable() {
        config = this.getConfig();
        if (!new File(this.getDataFolder(), "config.yml").exists()) {
            this.saveDefaultConfig();
            config.options().copyDefaults(true);
        }
        instance = this;

        log = this.getLogger();

        registerEvents();

        log.info("Plugin loaded.");

        log.info("Edible items include: ");
        for (Material mat : Material.values()) {
            if (FoodManager.isFood(mat))
                log.info(mat.name());
        }
    }

    public void onDisable() {


        log.info("Plugin disabled.");
    }

    public static Main getInstance() {
        return instance;
    }

    public void registerEvents() {
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new ItemListener(), this);
    }

}
