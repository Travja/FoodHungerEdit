package me.travja.foodedit.listeners;

import de.tr7zw.nbtapi.NBTItem;
import me.travja.foodedit.Main;
import me.travja.foodedit.util.FoodManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ItemListener implements Listener {


    @EventHandler
    public void degrade(PlayerInteractEvent event) {
        if (!event.hasItem())
            return;

        Block bl = event.getClickedBlock();
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (!item.getType().isEdible() && Main.getFoodManager().isFood(item)) {
            if (bl != null && (bl.getState() instanceof BlockInventoryHolder || bl.getType() == Material.ANVIL))
                event.setUseItemInHand(Event.Result.DENY);

            if (player.getFoodLevel() < 20 && event.useItemInHand() == Event.Result.DEFAULT) {
                int newFood = player.getFoodLevel() + Main.getFoodManager().getFoodMod(item);
                if (newFood > 20) newFood = 20;
                FoodLevelChangeEvent food = new FoodLevelChangeEvent(player, newFood, item);
                Bukkit.getPluginManager().callEvent(food);

                if (!food.isCancelled()) {
                    if (item.getAmount() > 1)
                        item.setAmount(item.getAmount() - 1);
                    else
                        player.getInventory().remove(item);
                    player.setFoodLevel(newFood);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
                    Vector dir = player.getLocation().getDirection().normalize().multiply(0.5);
                    player.getWorld().spawnParticle(Particle.ITEM_CRACK, player.getLocation().clone().add(dir.getX(), 1.4 + dir.getY(), dir.getZ()), 10, 0.1, 0.1, 0.1, 0.06, item);
                    event.setUseItemInHand(Event.Result.DENY);
                }
            }
        }

        if (Main.getFoodManager().isFood(item) && event.useItemInHand() == Event.Result.DEFAULT)
            player.getInventory().setItemInMainHand(Main.getFoodManager().ageFood(item));
    }

    @EventHandler
    public void join(PlayerJoinEvent event) {
        Main.getFoodManager().ageFood(event.getPlayer().getInventory());
    }

    @EventHandler
    public void openInv(InventoryOpenEvent event) {
        Main.getFoodManager().ageFood(event.getInventory());
    }

    @EventHandler
    public void closeInv(InventoryCloseEvent event) {
        Main.getFoodManager().ageFood(event.getInventory());
    }

    @EventHandler
    public void hopper(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (!Main.getFoodManager().isFood(item))
            return;

        ItemStack match = null;
        Inventory to = event.getDestination(), source = event.getSource();
        Main.getFoodManager().ageFood(to);
        Main.getFoodManager().ageFood(source);

        int index = -1;
        for (ItemStack toItem : to.getContents()) {
            index++;
            if (!Main.getFoodManager().isFood(toItem))
                continue;
            if (Main.getFoodManager().match(item, toItem) && toItem.getAmount() < toItem.getMaxStackSize()) {
                match = toItem;
                break;
            }
        }
        if (match != null) {
            ItemStack finalMatch = match;
            int finalIndex = index;
            long oldestStamp = Main.getFoodManager().getOldestStamp(item, match);
            if (FoodManager.debug)
                Main.log("Between the two items, the oldest was made at " + oldestStamp); //DEBUG
            event.setCancelled(true);

            new BukkitRunnable() {
                public void run() {
                    finalMatch.setAmount(finalMatch.getAmount() + item.getAmount());
                    NBTItem nbt = new NBTItem(finalMatch);
                    nbt.setLong(FoodManager.TIME_STRING, oldestStamp);
                    to.setItem(finalIndex, nbt.getItem());
                    source.removeItem(item);
                }
            }.runTaskLater(Main.getInstance(), 1L);
        }
    }

    @EventHandler
    public void pickup(EntityPickupItemEvent event) {
        Entity e = event.getEntity();

        if (!(e instanceof Player))
            return;

        Item item = event.getItem();
        if (!Main.getFoodManager().isFood(item.getItemStack()))
            return;
        ItemStack itemStack = item.getItemStack();
        new BukkitRunnable() {
            public void run() {
                Inventory inv = ((Player) e).getInventory();
                if (inv.contains(itemStack)) { //TODO Stack items with OLDEST item taking priority.
                    inv.removeItem(itemStack);
                    inv.addItem(Main.getFoodManager().ageFood(itemStack));
                }
            }
        }.runTaskLater(Main.getInstance(), 1L);
    }

    @EventHandler //TODO update item if shift clicked. Also allow stacking items. (Oldest overwrites.)
    public void invClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        HumanEntity player = event.getWhoClicked();
        if (current == null)
            return;

        if (Main.getFoodManager().isFood(current))
            event.setCurrentItem(Main.getFoodManager().ageFood(current));

        InventoryAction act = event.getAction();
        //Merge logic
        if (FoodManager.debug)
            player.sendMessage("InventoryAction: " + act); //DEBUG
        if (act == InventoryAction.COLLECT_TO_CURSOR) {

        } else if (act == InventoryAction.MOVE_TO_OTHER_INVENTORY) {

        } else if (act == InventoryAction.PLACE_ONE || act == InventoryAction.PLACE_SOME) {

        } else if (act == InventoryAction.SWAP_WITH_CURSOR) {

        }

    }

    @EventHandler
    public void inventory(InventoryEvent event) {
        if (FoodManager.debug)
            Bukkit.broadcastMessage("InventoryEvent!"); //DEBUG
        Main.getFoodManager().ageFood(event.getInventory());
    }

}
