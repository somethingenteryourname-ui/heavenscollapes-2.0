package com.heavenscollapse.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks each player's Heaven's Collapse hit count independently.
 *
 * <p>Design notes (documented per the spec's request to make the reset
 * behaviour explicit):</p>
 * <ul>
 *   <li>Each player has their own counter, keyed by UUID. Counters never
 *       interact between players.</li>
 *   <li>Every non-cancelled hit landed with the mace increments the
 *       counter. When the counter reaches {@code hitsRequired}, the
 *       counter resets to 0 <b>regardless</b> of whether the special
 *       attack actually activated (i.e. even if the target turned out to
 *       be standing on the ground and the attack fizzled). This keeps the
 *       "every Nth hit is a checked attempt" behaviour simple and
 *       predictable for players.</li>
 *   <li>If a player goes longer than {@code idleResetMillis} without
 *       landing a hit, their counter is wiped back to 0 the next time they
 *       do hit something. Set {@code idleResetMillis} to 0 to disable this.</li>
 *   <li>{@link #resetPlayer(UUID)} is called explicitly when a player logs
 *       out or switches away from the mace, so the counter doesn't linger
 *       and silently "charge" while unrelated combat happens.</li>
 * </ul>
 */
public class AbilityManager {

    private final Map<UUID, Integer> hitCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastHitMillis = new ConcurrentHashMap<>();

    private final int hitsRequired;
    private final long idleResetMillis;

    public AbilityManager(int hitsRequired, long idleResetMillis) {
        this.hitsRequired = Math.max(1, hitsRequired);
        this.idleResetMillis = Math.max(0, idleResetMillis);
    }

    /**
     * Registers one successful mace hit for the given player.
     *
     * @return true if this hit is the Nth hit and should be checked as a
     *         potential special attack; false for a normal hit.
     */
    public boolean registerHit(UUID playerId) {
        return registerHitVerbose(playerId).charged();
    }

    /**
     * Same as {@link #registerHit(UUID)}, but also returns the hit count
     * that was just reached, so callers (e.g. debug feedback) can show the
     * player their progress toward the next charged hit.
     */
    public HitResult registerHitVerbose(UUID playerId) {
        long now = System.currentTimeMillis();

        Long last = lastHitMillis.get(playerId);
        if (last != null && idleResetMillis > 0 && (now - last) > idleResetMillis) {
            hitCounts.put(playerId, 0);
        }
        lastHitMillis.put(playerId, now);

        int newCount = hitCounts.merge(playerId, 1, Integer::sum);

        if (newCount >= hitsRequired) {
            hitCounts.put(playerId, 0);
            return new HitResult(newCount, true);
        }
        return new HitResult(newCount, false);
    }

    /**
     * @param count   the hit count just reached (1..hitsRequired)
     * @param charged true if this hit reached hitsRequired and should be
     *                checked as a potential special attack
     */
    public record HitResult(int count, boolean charged) {
    }

    /**
     * Clears all tracked state for a player - used on logout and when the
     * player switches away from the Heaven's Collapse mace.
     */
    public void resetPlayer(UUID playerId) {
        hitCounts.remove(playerId);
        lastHitMillis.remove(playerId);
    }

    public int getHitsRequired() {
        return hitsRequired;
    }
}
