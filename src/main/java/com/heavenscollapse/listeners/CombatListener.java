package com.heavenscollapse.listeners;

import com.heavenscollapse.HeavensCollapseItem;
import com.heavenscollapse.HeavensCollapsePlugin;
import com.heavenscollapse.util.AbilityManager;
import com.heavenscollapse.util.EffectsUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles every hit landed with the Heaven's Collapse mace: counts it per
 * player, and on the configured Nth hit checks whether the target is
 * airborne. If so, the hit is boosted to a guaranteed kill and the
 * cinematic effects play. If the target is on the ground, the special
 * fizzles and the hit is treated as a completely normal mace hit.
 *
 * <p>When {@code debug.actionbar} is enabled in config.yml, the attacker
 * gets a live action-bar readout of their hit count and, on the checked
 * Nth hit, whether it activated or fizzled because the target was
 * grounded - this makes the (otherwise invisible) counter and ground
 * check easy to verify while testing.</p>
 */
public class CombatListener implements Listener {

    /**
     * A single base-damage value large enough to guarantee a kill after
     * armor, enchantment protection, resistance and absorption are all
     * applied (which reduce damage multiplicatively/additively, but never
     * enough to bring a value this large down to survivable levels).
     */
    private static final double GUARANTEED_KILL_DAMAGE = 1_000_000.0D;

    private final HeavensCollapsePlugin plugin;
    private final HeavensCollapseItem itemManager;
    private final AbilityManager abilityManager;
    private final EffectsUtil effectsUtil;

    public CombatListener(HeavensCollapsePlugin plugin, HeavensCollapseItem itemManager,
                           AbilityManager abilityManager, EffectsUtil effectsUtil) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.abilityManager = abilityManager;
        this.effectsUtil = effectsUtil;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            // Never trigger against the attacker themselves.
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!itemManager.isHeavensCollapse(weapon)) {
            return;
        }

        AbilityManager.HitResult result = abilityManager.registerHitVerbose(player.getUniqueId());

        if (!result.charged()) {
            sendDebugActionbar(player, hitCounterComponent(result.count(), abilityManager.getHitsRequired()));
            return;
        }

        if (target.isOnGround()) {
            // Ground restriction: the special never activates while the
            // target is standing on solid ground, even on the Nth hit.
            // The counter has already been reset by registerHitVerbose(),
            // and this hit proceeds as an entirely normal mace attack.
            sendDebugActionbar(player, Component.text("Heaven's Collapse fizzled - target was on the ground.")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        // Target is airborne on the Nth hit: trigger Heaven's Collapse.
        event.setDamage(GUARANTEED_KILL_DAMAGE);
        effectsUtil.playSpecialAttack(player, target);
        sendDebugActionbar(player, Component.text("HEAVEN'S COLLAPSE!")
                .color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true));
    }

    private Component hitCounterComponent(int count, int required) {
        return Component.text("Heaven's Collapse: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(count + "/" + required).color(NamedTextColor.WHITE));
    }

    private void sendDebugActionbar(Player player, Component message) {
        if (plugin.isDebugActionbarEnabled()) {
            player.sendActionBar(message);
        }
    }
}
