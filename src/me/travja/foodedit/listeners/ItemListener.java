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

        Inventory to = event.getDestination(), source = event.getSource();

        if (stackMatch(item, to, source) == null) {
            event.setCancelled(true);
            new BukkitRunnable() {
                public void run() {
                    source.removeItem(item);
                }
            }.runTaskLater(Main.getInstance(), 1L);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void pickup(EntityPickupItemEvent event) {
        Entity e = event.getEntity();

        if (!(e instanceof Player))
            return;


        Player player = (Player) e;
        Item item = event.getItem();
        if (!Main.getFoodManager().isFood(item.getItemStack()))
            return;
        ItemStack itemStack = Main.getFoodManager().ageFood(item.getItemStack());
        item.setItemStack(itemStack);
        if (FoodManager.debug)
            System.out.println("Picked up item");//DEBUG


        Inventory inv = ((Player) e).getInventory();
        ItemStack left = stackMatch(itemStack, inv, null);
        if (left == null) {
            event.setCancelled(true);
            event.getItem().remove();
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        }
    }

    @EventHandler //TODO update item if shift clicked. Also allow stacking items. (Oldest overwrites.)
    public void invClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        HumanEntity player = event.getWhoClicked();
        if (current == null)
            return;

        InventoryAction act = event.getAction();

        if (FoodManager.debug)
            player.sendMessage("InventoryAction: " + act + " Item: " + current.getType()); //DEBUG

        //Merge logic
        if (Main.getFoodManager().isFood(current)) {
            if (act == InventoryAction.COLLECT_TO_CURSOR) {

            } else if (act == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                Inventory from = event.getClickedInventory();
                Inventory inv1 = event.getView().getTopInventory();
                Inventory to = from.equals(inv1) ? event.getView().getBottomInventory() : inv1;
                ItemStack left = stackMatch(current, to, from);
                if (left != null && to.getType() == InventoryType.CRAFTING) { //TODO Fix this.. Items disappear when shift clicking just in the inventory
                    //event.setCurrentItem(left);
                    /*event.setCancelled(true);*/
                    left = stackMatch(left, from, from, event.getSlot());
                    if (left == null) {
                        if (FoodManager.debug) System.out.println("We good man....");
                        //event.setCurrentItem(left);
                    } else {
                        if (FoodManager.debug)
                            System.out.println("Still items left!");
                        event.setCancelled(true);
                        from.setItem(getFirstEmpty(from, event.getSlot() <= 8 ? 9 : 0), left);
                        event.setCurrentItem(null);
                    }
                } else {
                    event.setCancelled(true);
                    event.setCurrentItem(left);
                }

                new BukkitRunnable() {
                    public void run() {
                        ((Player) player).updateInventory();
                    }
                }.runTaskLater(Main.getInstance(), 2L);
            } else if (act == InventoryAction.SWAP_WITH_CURSOR) {
                ItemStack cursor = event.getCursor();
                if (Main.getFoodManager().match(cursor, current)) {
                    event.setCancelled(true);
                    long oldest = Main.getFoodManager().getOldestStamp(current, cursor);
                    NBTItem nbt = new NBTItem(current);
                    nbt.setLong(Main.getFoodManager().TIME_STRING, oldest);
                    current = nbt.getItem();
                    if (event.isRightClick()) {
                        current.setAmount(current.getAmount() + 1);
                        cursor.setAmount(cursor.getAmount() - 1);
                    } else if (event.isLeftClick()) {
                        int extra = current.getMaxStackSize() - current.getAmount();
                        int leftCursor = cursor.getAmount() - extra;
                        current.setAmount(current.getAmount() + cursor.getAmount() - (leftCursor > 0 ? leftCursor : 0));
                        cursor.setAmount(leftCursor);
                    }
                    event.setCurrentItem(current);
                    ((Player) player).updateInventory();
                }
            }
        }
    }

    private ItemStack stackMatch(ItemStack item, Inventory inv, Inventory from) {
        return stackMatch(item, inv, from, -1);
    }

    private ItemStack stackMatch(ItemStack item, Inventory inv, Inventory from, int slot) {
        ItemStack match = null;

        Main.getFoodManager().ageFood(inv);
        if (from != null)
            Main.getFoodManager().ageFood(from);
        else
            item = Main.getFoodManager().ageFood(item);
        if (FoodManager.debug)
            Main.log("From inv... " + from + " -- To inv.... " + inv + " with items " + inv.getContents().toString()); //DEBUG

        int index = -1;
        for (ItemStack toItem : inv.getContents()) {
            index++;
            if (!Main.getFoodManager().isFood(toItem) || index == slot)
                continue;
            if ((slot <= 8 && slot >= 0) && index <= 8)
                continue;
            else if (slot > 8 && index > 8)
                break;

            if (Main.getFoodManager().match(item, toItem) && toItem.getAmount() < toItem.getMaxStackSize()) {
                match = toItem;
                break;
            }
        }
        if (match != null) {
            long oldestStamp = Main.getFoodManager().getOldestStamp(item, match);
            if (FoodManager.debug)
                Main.log("Between the two items, the oldest was made at " + oldestStamp); //DEBUG

            int newAmount = match.getAmount() + item.getAmount();
            int itAmount = newAmount - match.getMaxStackSize();
            newAmount = newAmount > match.getMaxStackSize() ? match.getMaxStackSize() : newAmount;
            match.setAmount(newAmount);

            if (FoodManager.debug) {
                System.out.println("We have a new stack size of " + newAmount + " and " + itAmount + " excess.");
            }


            NBTItem nbt = new NBTItem(match);
            nbt.setLong(FoodManager.TIME_STRING, oldestStamp);
            inv.setItem(index, nbt.getItem());


            if (itAmount > 0) {
                item.setAmount(itAmount);
                //If we have leftover items, we need a place to put them.
                return stackMatch(item, inv, from);
            }
            return null;
        } else {
            if (inv.firstEmpty() != -1) {
                if (from.equals(inv)) {
                    return item;
                } else {
                    inv.addItem(item);
                    return null;
                }
            } else {
                return item;
            }
        }
    }

    private int getFirstEmpty(Inventory inv, int start) {
        if(start == 0)
            return inv.firstEmpty();
        else {
            ItemStack[] contents = inv.getContents();
            for(int i = start; i < contents.length; i++) {
                if(contents[i] == null) {
                    return i;
                }
            }
            return -1;
        }
    }

}
