package com.nowheberg.bottrainer.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtil {
    public static ItemStack named(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta(); meta.setDisplayName(name); meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); it.setItemMeta(meta);
        return it;
    }
}
