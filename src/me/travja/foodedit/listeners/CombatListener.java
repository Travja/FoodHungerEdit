package me.travja.foodedit.listeners;

import me.travja.foodedit.Main;
import me.travja.foodedit.util.FoodManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CombatListener implements Listener {

    private HashMap<UUID, Long> startInCombat = new HashMap<>();
    private HashMap<UUID, Long> lastCombat = new HashMap<>();
    private HashMap<UUID, Integer> tasks = new HashMap<>();

    @EventHandler
    public void damage(EntityDamageByEntityEvent event) {
        Entity d = event.getDamager();
        Entity e = event.getEntity();
        if (FoodManager.debug)
            System.out.println("Damage: " + event.getDamage());
        if (d instanceof Player) {
            checkCombat((Player) d);
        }
        if (e instanceof Player) {
            checkCombat((Player) e);
        }
    }

    private void checkCombat(Player p) {
        long timeSinceLastCombat = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - (lastCombat.containsKey(p.getUniqueId()) ? lastCombat.get(p.getUniqueId()) : 0));
        if (timeSinceLastCombat >= 4) {
            startInCombat.remove(p.getUniqueId());
        }

        if (!startInCombat.containsKey(p.getUniqueId()))
            startInCombat.put(p.getUniqueId(), System.currentTimeMillis());

        lastCombat.put(p.getUniqueId(), System.currentTimeMillis());
        long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startInCombat.get(p.getUniqueId()));
        if (seconds >= 8) {
            if (FoodManager.debug)
                System.out.println("Been in combat for 8 seconds. Change hunger.");
            int newFood = p.getFoodLevel() - 4;
            newFood = newFood < 0 ? 0 : newFood;
            p.setFoodLevel(newFood);
            startInCombat.put(p.getUniqueId(), System.currentTimeMillis());
        }


        if (tasks.containsKey(p.getUniqueId()))
            Bukkit.getScheduler().cancelTask(tasks.get(p.getUniqueId()));

        int task = new BukkitRunnable() {
            public void run() {
                lastCombat.remove(p.getUniqueId());
                startInCombat.remove(p.getUniqueId());
                tasks.remove(p.getUniqueId());
            }
        }.runTaskLater(Main.getInstance(), 20L * 30L).getTaskId();
        tasks.put(p.getUniqueId(), task);
    }

}
