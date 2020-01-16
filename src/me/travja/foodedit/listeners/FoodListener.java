package me.travja.foodedit.listeners;

import me.travja.foodedit.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;

public class FoodListener implements Listener {

    @EventHandler
    public void foodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = ((Player) event.getEntity());
        if (event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
            return;
        }

        if(event.getItem() == null)
            return;

        ItemStack item = event.getItem();

        Main.getFoodManager().getHealthMod(item);

        //TODO check food and give appropriate health/effects


    }

}
