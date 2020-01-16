package me.travja.foodedit.listeners;

import me.travja.foodedit.Main;
import me.travja.foodedit.util.FoodManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ItemListener implements Listener {


    @EventHandler
    public void degrade(PlayerInteractEvent event) {
        if (!event.hasItem())
            return;

        ItemStack item = event.getItem();
        if (Main.getFoodManager().isFood(item))
            event.getPlayer().getInventory().setItemInMainHand(Main.getFoodManager().ageFood(item));

        //For debug purposes
        /*boolean left = event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK;
        ItemStack item = event.getItem();
        NBTItem nbt = new NBTItem(item);
        nbt.setLong("creationDates", nbt.getLong("creationDates") + (left ? 24000L : -24000L));
        event.getPlayer().getInventory().setItemInMainHand(FoodManager.ageFood(nbt.getItem()));*/
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

        for (ItemStack toItem : to.getContents()) {
            if (Main.getFoodManager().match(item, toItem) && toItem.getAmount() < toItem.getMaxStackSize()) {
                match = toItem;
                break;
            }
        }
        if (match != null) {
            ItemStack finalMatch = match;
            event.setCancelled(true);

            new BukkitRunnable() {
                public void run() {
                    finalMatch.setAmount(finalMatch.getAmount() + item.getAmount());
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
        player.sendMessage("InventoryAction: " + act);
        if (act == InventoryAction.COLLECT_TO_CURSOR) {

        } else if (act == InventoryAction.MOVE_TO_OTHER_INVENTORY) {

        } else if (act == InventoryAction.PLACE_ONE || act == InventoryAction.PLACE_SOME) {

        } else if (act == InventoryAction.SWAP_WITH_CURSOR) {

        }

    }

    @EventHandler
    public void inventory(InventoryEvent event) {
        Bukkit.broadcastMessage("InventoryEvent!");
        Main.getFoodManager().ageFood(event.getInventory());
    }

}
