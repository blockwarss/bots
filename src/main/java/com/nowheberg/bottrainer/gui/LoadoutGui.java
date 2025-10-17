package com.nowheberg.bottrainer.gui;

import com.nowheberg.bottrainer.session.SessionManager;
import com.nowheberg.bottrainer.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class LoadoutGui implements Listener {
    private final SessionManager manager; private final Player player; private final Consumer<SessionManager.Loadout> confirm;
    private Inventory inv; private SessionManager.Loadout loadout;

    public LoadoutGui(SessionManager manager, Player player, Consumer<SessionManager.Loadout> confirm) {
        this.manager = manager; this.player = player; this.confirm = confirm;
        Bukkit.getPluginManager().registerEvents(this, com.nowheberg.bottrainer.BotTrainerPlugin.get());
    }

    public void open() {
        this.inv = Bukkit.createInventory(null, 6*9, "§8Choix de l'équipement");
        this.loadout = manager.createDefaultLoadout();
        redraw();
        player.openInventory(inv);
    }

    private void redraw() {
        inv.clear();
        for (int i=0;i<9;i++) inv.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(13, ItemUtil.named(Material.NETHERITE_SWORD, "§bÉpée Netherite"));
        inv.setItem(14, ItemUtil.named(Material.MACE, "§dMasse"));
        inv.setItem(15, ItemUtil.named(Material.BOW, "§aArc"));
        inv.setItem(22, ItemUtil.named(Material.END_CRYSTAL, "§6Cristaux (x64)"));
        inv.setItem(23, ItemUtil.named(Material.OBSIDIAN, "§8Obsidienne (x64)"));
        inv.setItem(24, ItemUtil.named(Material.GOLDEN_APPLE, "§ePommes dorées (x16)"));
        inv.setItem(31, ItemUtil.named(Material.NETHERITE_CHESTPLATE, "§fArmure Netherite"));
        inv.setItem(49, ItemUtil.named(Material.LIME_WOOL, "§aDémarrer"));
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (!e.getWhoClicked().getUniqueId().equals(player.getUniqueId())) return;
        if (e.getClickedInventory() == null || !e.getView().getTitle().contains("Choix de l'équipement")) return;
        e.setCancelled(true);
        ItemStack it = e.getCurrentItem(); if (it == null) return;
        Material m = it.getType();
        switch (m) {
            case NETHERITE_SWORD -> loadout.contents[0] = new ItemStack(Material.NETHERITE_SWORD);
            case MACE -> loadout.contents[1] = new ItemStack(Material.MACE);
            case BOW -> { loadout.contents[4] = new ItemStack(Material.BOW); loadout.contents[5] = new ItemStack(Material.ARROW, 64); }
            case END_CRYSTAL -> loadout.contents[2] = new ItemStack(Material.END_CRYSTAL, 64);
            case OBSIDIAN -> loadout.contents[3] = new ItemStack(Material.OBSIDIAN, 64);
            case GOLDEN_APPLE -> loadout.contents[8] = new ItemStack(Material.GOLDEN_APPLE, 16);
            case NETHERITE_CHESTPLATE -> {
                loadout.armor[3] = new ItemStack(Material.NETHERITE_HELMET);
                loadout.armor[2] = new ItemStack(Material.NETHERITE_CHESTPLATE);
                loadout.armor[1] = new ItemStack(Material.NETHERITE_LEGGINGS);
                loadout.armor[0] = new ItemStack(Material.NETHERITE_BOOTS);
            }
            case LIME_WOOL -> { player.closeInventory(); InventoryClickEvent.getHandlerList().unregister(this); confirm.accept(loadout); return; }
            default -> {}
        }
        player.sendMessage("§7Équipement mis à jour.");
    }
}
