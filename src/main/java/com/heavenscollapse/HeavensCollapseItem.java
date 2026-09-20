package com.heavenscollapse;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and identifies the custom "Heaven's Collapse" mace.
 *
 * <p>Identification never relies on the display name - a
 * {@link PersistentDataType#BYTE} tag on the item's
 * {@link org.bukkit.persistence.PersistentDataContainer} is the single
 * source of truth, so renaming the item in an anvil (or any other
 * display-name change) can never break or spoof the ability.</p>
 */
public class HeavensCollapseItem {

    private static final String PDC_KEY = "heavens_collapse";

    private final NamespacedKey key;

    public HeavensCollapseItem(HeavensCollapsePlugin plugin) {
        this.key = new NamespacedKey(plugin, PDC_KEY);
    }

    /**
     * Creates a brand new Heaven's Collapse mace item stack.
     */
    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            // Should never happen for a valid Material, but keep the plugin
            // from throwing an NPE into the caller if it ever does.
            item.setItemMeta(item.getItemMeta());
            return item;
        }

        meta.displayName(
                Component.text("Heaven's Collapse")
                        .color(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false)
        );

        List<Component> lore = new ArrayList<>();
        lore.add(plain("A mace forged from the wrath of the sky."));
        lore.add(plain("Every third true strike calls down judgment"));
        lore.add(plain("upon those who have left the earth."));
        lore.add(Component.empty());
        lore.add(
                Component.text("STRUCK FROM ABOVE")
                        .color(NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.OBFUSCATED, true)
                        .decoration(TextDecoration.ITALIC, false)
        );
        meta.lore(lore);

        // Visual-only glint (Paper API) - makes the item shimmer like an
        // enchanted item without adding any real enchantment that could
        // interfere with damage calculation or the ability logic.
        meta.setEnchantmentGlintOverride(true);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);

        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Reliable identity check via PersistentDataContainer - the only thing
     * that should ever gate the special ability, the command, and the
     * lightning-obtain transformation.
     */
    public boolean isHeavensCollapse(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /**
     * True for a completely ordinary vanilla Mace (not Heaven's Collapse) -
     * used by the lightning-obtain mechanic to find an eligible item to
     * transform.
     */
    public boolean isPlainMace(ItemStack item) {
        return item != null && item.getType() == Material.MACE && !isHeavensCollapse(item);
    }

    private Component plain(String text) {
        return Component.text(text).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    public NamespacedKey getKey() {
        return key;
    }
}
