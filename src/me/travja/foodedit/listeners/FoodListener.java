package me.travja.foodedit.listeners;

import me.travja.foodedit.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class FoodListener implements Listener {

    private final boolean disableCampfire = Main.config().getBoolean("disableCampfire");
    private static HashMap<UUID, Integer> foodTime = new HashMap<>();
    private static boolean running = false;
    private Random rand = new Random();

    @EventHandler
    public void join(PlayerJoinEvent event) {
        //Start timer
        if (!foodTime.containsKey(event.getPlayer().getUniqueId())) {
            foodTime.put(event.getPlayer().getUniqueId(), 120);
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        foodTime.remove(event.getPlayer().getUniqueId());
    }

    public static void updateFood() {
        if (!running) {
            running = true;
            new BukkitRunnable() {
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!foodTime.containsKey(player.getUniqueId()))
                            foodTime.put(player.getUniqueId(), 120);
                    }
                    for (UUID id : foodTime.keySet()) {
                        Player player = Bukkit.getPlayer(id);
                        if (player != null && player.isOnline()) {
                            int newTime = foodTime.get(id) - 1;
                            if (newTime <= 0) {
                                int newFood = player.getFoodLevel() - 2;
                                newFood = newFood < 0 ? 0 : newFood;
                                player.setFoodLevel(newFood);
                                newTime = 120;
                            }
                            if (newTime % 15 == 0)
                                Main.getFoodManager().ageFood(player.getInventory());
                            foodTime.put(id, newTime);
                        } else {
                            foodTime.remove(id);
                        }
                    }
                }
            }.runTaskTimer(Main.getInstance(), 20L, 20L);
        }
    }

    private ArrayList<UUID> zero = new ArrayList<>();

    @EventHandler
    public void foodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = ((Player) event.getEntity());
        if (event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
            return;
        }

        if (event.getItem() == null)
            return;

        if (player.getFoodLevel() <= 0) {
            zero.add(player.getUniqueId());
            new BukkitRunnable() {
                public void run() {
                    zero.remove(player.getUniqueId());
                }
            }.runTaskLater(Main.getInstance(), 20L * 30L);
        }

        ItemStack item = event.getItem();

        int mod = Main.getFoodManager().getFoodMod(item);
        PotionEffect effect;

        if (item.getType() != Material.GLISTERING_MELON_SLICE) {
            effect = Main.getFoodManager().getEffect(item);
        } else {
            ArrayList<PotionEffect> effs = Main.getFoodManager().getGlisteringEffects();
            effect = effs.get(rand.nextInt(effs.size()));
        }

        applyEffect(player, effect);
        int newFood = player.getFoodLevel() + mod;
        newFood = newFood > 20 ? 20 : newFood;
        event.setFoodLevel(newFood);//Apply hunger modifier

        if (zero.contains(player.getUniqueId()) && event.getFoodLevel() >= 20) { //If the player eats to fill in less than 30 seconds
            applyEffect(player, Main.getFoodManager().getQuickFill());
        }

        if (event.getFoodLevel() >= 20) {
            for (PotionEffect pe : Main.getFoodManager().getFullEffects()) { //Apply effects for "full"
                applyEffect(player, pe);
            }
        }


        if (item.getType() == Material.SPIDER_EYE) {
            new BukkitRunnable() {
                public void run() {
                    player.removePotionEffect(PotionEffectType.POISON);
                }
            }.runTaskLater(Main.getInstance(), 3L);
        }
    }

    @EventHandler
    public void blockCook(BlockCookEvent event) {
        Block b = event.getBlock();
        if (b.getType() == Material.CAMPFIRE && disableCampfire)
            event.setCancelled(true);
    }

    private void applyEffect(Player player, PotionEffect pe) {
        if (pe == null)
            return;

        if (player.hasPotionEffect(pe.getType()))
            if (player.getPotionEffect(pe.getType()).getDuration() < pe.getDuration())
                player.removePotionEffect(pe.getType());
        player.addPotionEffect(pe);
    }

}
