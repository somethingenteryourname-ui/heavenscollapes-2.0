package com.heavenscollapse.util;

import com.heavenscollapse.HeavensCollapsePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Plays the "divine lightning strike" cinematic: sounds, particles and an
 * optional lightning bolt. Everything here is purely presentational - it
 * never touches health or damage.
 *
 * <p>Every world interaction takes explicit {@link Location}/{@link World}
 * parameters captured by the caller <em>before</em> the killing blow is
 * applied, so this class never depends on the target entity still being
 * alive or even still existing.</p>
 */
public class EffectsUtil {

    private final HeavensCollapsePlugin plugin;

    public EffectsUtil(HeavensCollapsePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Plays the full special-attack cinematic centered on the target's
     * current location, with a beam of particles reaching back toward the
     * attacker.
     */
    public void playSpecialAttack(Player attacker, LivingEntity target) {
        World world = target.getWorld();
        Location targetLoc = target.getLocation().add(0, 1.0, 0);
        Location attackerLoc = attacker.getEyeLocation();

        playSounds(world, targetLoc);
        strikeLightning(world, targetLoc);
        playParticles(world, targetLoc, attackerLoc);
    }

    private void playSounds(World world, Location targetLoc) {
        if (!plugin.isSoundsEnabled()) {
            return;
        }
        world.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 2.5f, 0.9f);
        world.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 2.0f, 1.0f);
        world.playSound(targetLoc, Sound.ITEM_MACE_SMASH_GROUND_HEAVY, SoundCategory.PLAYERS, 1.6f, 0.8f);
        world.playSound(targetLoc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.7f);
    }

    private void strikeLightning(World world, Location targetLoc) {
        if (!plugin.isLightningEnabled()) {
            return;
        }
        if (plugin.isLightningDamage()) {
            world.strikeLightning(targetLoc);
            if (!plugin.isLightningFire()) {
                extinguishNearbyFire(targetLoc);
            }
        } else {
            // Visual-only strike: no block damage, no fire, no extra
            // entity damage - just the bolt, flash and thunder.
            world.strikeLightningEffect(targetLoc);
        }
    }

    private void playParticles(World world, Location targetLoc, Location attackerLoc) {
        if (!plugin.isParticlesEnabled()) {
            return;
        }

        world.spawnParticle(Particle.FLASH, targetLoc, 2, 0, 0, 0, 0);
        world.spawnParticle(Particle.END_ROD, targetLoc, 45, 0.4, 0.9, 0.4, 0.05);
        world.spawnParticle(Particle.ELECTRIC_SPARK, targetLoc, 35, 0.5, 1.0, 0.5, 0.15);
        world.spawnParticle(Particle.CLOUD, targetLoc, 18, 0.3, 0.3, 0.3, 0.02);

        spawnBeam(world, attackerLoc, targetLoc);
    }

    /**
     * Spawns a short trail of spark particles from the attacker toward the
     * target so the strike reads as connected to the player rather than
     * appearing out of nowhere.
     */
    private void spawnBeam(World world, Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        if (length < 0.5) {
            return;
        }
        direction.normalize();

        int points = (int) Math.min(20, Math.max(4, length * 2));
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            Location point = from.clone().add(direction.clone().multiply(length * t));
            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 2, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private void extinguishNearbyFire(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    Material type = block.getType();
                    if (type == Material.FIRE || type == Material.SOUL_FIRE) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }
}
