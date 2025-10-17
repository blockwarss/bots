package com.nowheberg.bottrainer.session;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record PlayerSnapshot(Location location, ItemStack[] contents, ItemStack[] armor, float exp, int level) {
    public static PlayerSnapshot of(Player p) {
        return new PlayerSnapshot(p.getLocation(), p.getInventory().getContents().clone(), p.getInventory().getArmorContents().clone(), p.getExp(), p.getLevel());
    }
    public void restore(Player p) {
        p.getInventory().setContents(contents);
        p.getInventory().setArmorContents(armor);
        p.setExp(exp);
        p.setLevel(level);
        p.teleport(location);
    }
}
