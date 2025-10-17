package com.nowheberg.bottrainer.session;

import com.nowheberg.bottrainer.BotTrainerPlugin;
import com.nowheberg.bottrainer.arena.Arena;
import com.nowheberg.bottrainer.bot.BotMode;
import com.nowheberg.bottrainer.bot.Difficulty;
import com.nowheberg.bottrainer.bot.impl.SimpleBot;
import com.nowheberg.bottrainer.gui.LoadoutGui;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager implements Listener {
    public static class Session {
        public final java.util.UUID playerId; public final PlayerSnapshot snapshot; public final long endTick;
        public final BotMode mode; public final Difficulty diff; public final long startedTick;
        public final Loadout loadout; public final SimpleBot bot;
        public Session(java.util.UUID playerId, PlayerSnapshot snapshot, long endTick, BotMode mode, Difficulty diff, Loadout loadout, SimpleBot bot) {
            this.playerId = playerId; this.snapshot = snapshot; this.endTick = endTick; this.mode = mode; this.diff = diff; this.loadout = loadout; this.bot = bot; this.startedTick = Bukkit.getCurrentTick();
        }
    }
    public static class Loadout { public ItemStack[] contents; public ItemStack[] armor; }

    private final java.util.Map<java.util.UUID, Session> sessions = new ConcurrentHashMap<>();
    private final BotTrainerPlugin plugin; private final com.nowheberg.bottrainer.arena.ArenaManager arenas;

    public SessionManager(BotTrainerPlugin plugin, com.nowheberg.bottrainer.arena.ArenaManager arenas) { this.plugin = plugin; this.arenas = arenas; }

    public void prepareAndOpenLoadout(Player p, Arena arena, Difficulty diff, BotMode mode, long durationTicks) {
        if (sessions.containsKey(p.getUniqueId())) { p.sendMessage("§cTu as déjà une session en cours."); return; }
        Location spawn = arena.spawn();
        if (spawn.getWorld()==null) { p.sendMessage("§cArène non configurée (monde invalide)."); return; }
        var snap = PlayerSnapshot.of(p);
        p.getInventory().clear(); p.getInventory().setArmorContents(null);
        p.setHealth(p.getMaxHealth()); p.setFoodLevel(20);
        p.teleport(spawn);

        java.util.function.Consumer<Loadout> onConfirm = (load) -> {
            p.getInventory().setContents(load.contents);
            p.getInventory().setArmorContents(load.armor);
            double baseRange = plugin.getConfig().getDouble("settings.bot.hitRange", 2.8);
            int baseCd = plugin.getConfig().getInt("settings.bot.attackCooldownTicks", 12);
            var diffSec = plugin.getConfig().getConfigurationSection("settings.difficulty."+diff.name());
            double speed = diffSec.getDouble("speed", 0.26);
            double strafe = diffSec.getDouble("strafe", 1.0);
            var bot = new SimpleBot(p, spawn, mode, diff, baseRange, baseCd, speed, strafe);
            bot.start();

            long endTick = Bukkit.getCurrentTick() + durationTicks;
            var session = new Session(p.getUniqueId(), snap, endTick, mode, diff, load, bot);
            sessions.put(p.getUniqueId(), session);
            p.sendMessage("§aEntraînement démarré: §e"+mode+" §7| §b"+diff+" §7| §f"+(durationTicks/20)+"s");
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                Session s = sessions.get(p.getUniqueId());
                if (s == null) return;
                if (!p.isOnline()) { stopSession(p.getUniqueId(), false); return; }
                if (Bukkit.getCurrentTick() >= s.endTick) stopSession(p.getUniqueId(), true);
            }, 20L, 20L);
        };
        new com.nowheberg.bottrainer.gui.LoadoutGui(this, p, onConfirm).open();
    }

    public void stopSession(java.util.UUID playerId, boolean restore) {
        Session s = sessions.remove(playerId);
        if (s == null) return;
        if (s.bot != null) s.bot.stop();
        var p = Bukkit.getPlayer(playerId);
        if (p != null && restore) {
            p.getInventory().clear(); p.getInventory().setArmorContents(null);
            s.snapshot.restore(p);
            p.sendMessage("§eEntraînement terminé. Inventaire et position restaurés.");
        }
    }

    public void shutdown() {
        for (var id : new java.util.ArrayList<>(sessions.keySet())) stopSession(id, true);
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { stopSession(e.getPlayer().getUniqueId(), true); }

    @EventHandler public void onDeath(PlayerDeathEvent e) {
        java.util.UUID id = e.getEntity().getUniqueId();
        if (!sessions.containsKey(id)) return;
        e.setKeepInventory(true); e.setKeepLevel(true); e.getDrops().clear();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            var s = sessions.get(id); if (s == null) return; e.getEntity().spigot().respawn();
            e.getEntity().teleport(s.bot.spawnPoint());
        }, 1L);
    }

    // Dégâts sur le bot -> totem infini simulé
    @org.bukkit.event.EventHandler public void onBotDamage(org.bukkit.event.entity.EntityDamageEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.LivingEntity le)) return;
        if (le.getCustomName()==null || !le.getCustomName().contains("Training Bot")) return;
        double finalDamage = e.getFinalDamage();
        if (le.getHealth() - finalDamage <= 0) {
            e.setCancelled(true);
            le.setNoDamageTicks(15);
            le.getWorld().playSound(le.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 1f, 1f);
            le.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, le.getLocation().add(0,1,0), 40, 0.4, 0.6, 0.4, 0.1);
            double newHp = Math.min(le.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(), le.getHealth() + 14.0);
            le.setHealth(newHp);
            if (le.getEquipment()!=null) le.getEquipment().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
        }
    }

    // Réduction dégâts chute/explosions dans l'arène (si activé)
    @org.bukkit.event.EventHandler public void onPlayerDamage(org.bukkit.event.entity.EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        java.util.UUID id = p.getUniqueId();
        Session s = sessions.get(id); if (s == null) return;
        switch (e.getCause()) {
            case FALL -> e.setCancelled(true);
            case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> {
                double mult = plugin.getConfig().getDouble("settings.crystalDamageMultiplier", 1.0);
                e.setDamage(e.getDamage()*mult);
            }
            default -> {}
        }
    }

    public Loadout createDefaultLoadout() {
        Loadout l = new Loadout();
        l.contents = new ItemStack[36];
        l.armor = new ItemStack[4];
        l.armor[3] = new ItemStack(Material.NETHERITE_HELMET);
        l.armor[2] = new ItemStack(Material.NETHERITE_CHESTPLATE);
        l.armor[1] = new ItemStack(Material.NETHERITE_LEGGINGS);
        l.armor[0] = new ItemStack(Material.NETHERITE_BOOTS);
        l.contents[0] = new ItemStack(Material.NETHERITE_SWORD);
        l.contents[1] = new ItemStack(Material.MACE);
        l.contents[2] = new ItemStack(Material.END_CRYSTAL, 64);
        l.contents[3] = new ItemStack(Material.OBSIDIAN, 64);
        l.contents[8] = new ItemStack(Material.GOLDEN_APPLE, 16);
        return l;
    }
}
