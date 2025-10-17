package com.nowheberg.bottrainer.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiItems {
    public static ItemStack button(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta(); meta.setDisplayName(name); meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); it.setItemMeta(meta);
        return it;
    }
}
