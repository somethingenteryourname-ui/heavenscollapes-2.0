package com.heavenscollapse.listeners;

import com.heavenscollapse.HeavensCollapseItem;
import com.heavenscollapse.util.AbilityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Resets a player's Heaven's Collapse hit counter as soon as they switch
 * their held hotbar slot away from the mace, per the spec's requirement
 * that the counter "reset appropriately if the player stops using the
 * weapon." (The idle-timeout in {@link AbilityManager} is a secondary
 * safety net for cases this event doesn't cover, e.g. dropping the item.)
 */
public class ItemSwitchListener implements Listener {

    private final HeavensCollapseItem itemManager;
    private final AbilityManager abilityManager;

    public ItemSwitchListener(HeavensCollapseItem itemManager, AbilityManager abilityManager) {
        this.itemManager = itemManager;
        this.abilityManager = abilityManager;
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (!itemManager.isHeavensCollapse(newItem)) {
            abilityManager.resetPlayer(player.getUniqueId());
        }
    }
}
