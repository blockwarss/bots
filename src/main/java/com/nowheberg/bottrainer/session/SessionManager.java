package com.nowheberg.bottrainer.session;

import com.nowheberg.bottrainer.BotTrainerPlugin;
import com.nowheberg.bottrainer.arena.Arena;
import com.nowheberg.bottrainer.bot.BotMode;
import com.nowheberg.bottrainer.bot.Difficulty;
import com.nowheberg.bottrainer.bot.impl.SimpleBot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager implements Listener {
    public static class Session {
        public final java.util.UUID playerId; public final PlayerSnapshot snapshot; public final long endTick;
        public final BotMode mode; public final Difficulty diff; public final long startedTick;
        public final SimpleBot bot;
        public Session(java.util.UUID playerId, PlayerSnapshot snapshot, long endTick, BotMode mode, Difficulty diff, SimpleBot bot) {
            this.playerId = playerId; this.snapshot = snapshot; this.endTick = endTick; this.mode = mode; this.diff = diff; this.bot = bot; this.startedTick = Bukkit.getCurrentTick();
        }
    }

    private final java.util.Map<java.util.UUID, Session> sessions = new ConcurrentHashMap<>();
    private final BotTrainerPlugin plugin; private final com.nowheberg.bottrainer.arena.ArenaManager arenas;

    public SessionManager(BotTrainerPlugin plugin, com.nowheberg.bottrainer.arena.ArenaManager arenas) { this.plugin = plugin; this.arenas = arenas; }

    public void startFixedSession(Player p, Arena arena, Difficulty diff, BotMode mode, long durationTicks) {
        // Si une session existe, on la stoppe proprement (évite les doublons)
        if (sessions.containsKey(p.getUniqueId())) stopSession(p.getUniqueId(), true);

        // Nettoyage d'anciens bots taggés pour ce joueur
        cleanupPlayerBots(p.getUniqueId(), arena);

        Location spawn = arena.spawn();
        if (spawn.getWorld()==null) { p.sendMessage("§cArène non configurée (monde invalide)."); return; }
        var snap = PlayerSnapshot.of(p);

        // Donner le kit fixe
        giveFixedKit(p);
        p.setHealth(p.getMaxHealth()); p.setFoodLevel(20);
        p.teleport(spawn);

        // Créer le bot
        double baseRange = plugin.getConfig().getDouble("settings.bot.hitRange", 2.8);
        int baseCd = plugin.getConfig().getInt("settings.bot.attackCooldownTicks", 12);
        var diffSec = plugin.getConfig().getConfigurationSection("settings.difficulty."+diff.name());
        double speed = diffSec.getDouble("speed", 0.26);
        double strafe = diffSec.getDouble("strafe", 1.0);
        var bot = new SimpleBot(p, spawn, mode, diff, baseRange, baseCd, speed, strafe);
        bot.start();
        // Tag pour pouvoir le supprimer de manière fiable
        if (bot.entity()!=null) {
            bot.entity().addScoreboardTag("bottrainer:" + p.getUniqueId());
        }

        long endTick = Bukkit.getCurrentTick() + durationTicks;
        var session = new Session(p.getUniqueId(), snap, endTick, mode, diff, bot);
        sessions.put(p.getUniqueId(), session);
        p.sendMessage("§aEntraînement démarré: §e"+mode+" §7| §b"+diff+" §7| §f"+(durationTicks/20)+"s");

        // Tâche de fin de session
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Session s = sessions.get(p.getUniqueId());
            if (s == null) return;
            if (!p.isOnline()) { stopSession(p.getUniqueId(), false); return; }
            if (Bukkit.getCurrentTick() >= s.endTick) stopSession(p.getUniqueId(), true);
        }, 20L, 20L);
    }

    public void stopSession(java.util.UUID playerId, boolean restore) {
        Session s = sessions.remove(playerId);
        if (s != null && s.bot != null) s.bot.stop();
        // Supprime tous les bots taggés de ce joueur (sécurité anti-doublon)
        cleanupPlayerBots(playerId, null);

        var p = Bukkit.getPlayer(playerId);
        if (p != null && restore && s != null) {
            p.getInventory().clear(); p.getInventory().setArmorContents(null);
            s.snapshot.restore(p);
            p.sendMessage("§eEntraînement terminé. Inventaire et position restaurés.");
        }
    }

    private void cleanupPlayerBots(java.util.UUID playerId, Arena arena) {
        if (arena != null && arena.world() != null) {
            for (var e : arena.world().getEntities()) {
                if (e.getScoreboardTags().contains("bottrainer:" + playerId)) e.remove();
            }
        } else {
            // Parcourt tous les mondes (fallback)
            Bukkit.getWorlds().forEach(w -> {
                for (var e : w.getEntities()) {
                    if (e.getScoreboardTags().contains("bottrainer:" + playerId)) e.remove();
                }
            });
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
        if (!le.getScoreboardTags().stream().anyMatch(t -> t.startsWith("bottrainer:"))) return;
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

    // ===== FIXED KIT =====
    private void giveFixedKit(Player p) {
        p.getInventory().clear(); p.getInventory().setArmorContents(null);

        // Armor netherite
        ItemStack helm = new ItemStack(Material.NETHERITE_HELMET);
        ItemStack chest = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemStack legs = new ItemStack(Material.NETHERITE_LEGGINGS);
        ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
        p.getInventory().setHelmet(helm);
        p.getInventory().setChestplate(chest);
        p.getInventory().setLeggings(legs);
        p.getInventory().setBoots(boots);

        // Sword netherite Unbreaking 5
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        sword.addUnsafeEnchantment(Enchantment.UNBREAKING, 5);

        // Mace 1: Wind Burst 2 + Density 5
        ItemStack maceWB = new ItemStack(Material.MACE);
        maceWB.addUnsafeEnchantment(Enchantment.WIND_BURST, 2);
        maceWB.addUnsafeEnchantment(Enchantment.DENSITY, 5);

        // Mace 2: Breach 4
        ItemStack maceBR = new ItemStack(Material.MACE);
        maceBR.addUnsafeEnchantment(Enchantment.BREACH, 4);

        // Elytra Unbreaking 10 (dans l'inventaire, pas équipée)
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        elytra.addUnsafeEnchantment(Enchantment.UNBREAKING, 10);

        // Steaks x64
        ItemStack steaks = new ItemStack(Material.COOKED_BEEF, 64);

        // Fireworks Force 1/2/3 (power=1,2,3) — 2 stacks chacun
        ItemStack fw1 = fireworkStack(1, 64);
        ItemStack fw1b = fireworkStack(1, 64);
        ItemStack fw2 = fireworkStack(2, 64);
        ItemStack fw2b = fireworkStack(2, 64);
        ItemStack fw3 = fireworkStack(3, 64);
        ItemStack fw3b = fireworkStack(3, 64);

        // Wind Charges x6 stacks (64)
        ItemStack wind = new ItemStack(Material.WIND_CHARGE, 64);
        ItemStack wind2 = wind.clone();
        ItemStack wind3 = wind.clone();
        ItemStack wind4 = wind.clone();
        ItemStack wind5 = wind.clone();
        ItemStack wind6 = wind.clone();

        // Ender pearls x3 stacks (16)
        ItemStack pearls = new ItemStack(Material.ENDER_PEARL, 16);
        ItemStack pearls2 = pearls.clone();
        ItemStack pearls3 = pearls.clone();

        // Placement dans l'inventaire (hotbar + reste)
        var inv = p.getInventory();
        inv.setItem(0, sword);
        inv.setItem(1, maceWB);
        inv.setItem(2, maceBR);
        inv.setItem(3, elytra);
        inv.setItem(4, steaks);
        inv.setItem(5, fw1);
        inv.setItem(6, fw2);
        inv.setItem(7, fw3);
        inv.setItem(8, pearls);

        inv.addItem(fw1b, fw2b, fw3b, wind, wind2, wind3, wind4, wind5, wind6, pearls2, pearls3);
    }

    private ItemStack fireworkStack(int power, int amount) {
        ItemStack it = new ItemStack(Material.FIREWORK_ROCKET, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta instanceof FireworkMeta fm) {
            fm.setPower(Math.max(1, Math.min(3, power)));
            it.setItemMeta(fm);
        }
        return it;
    }
}
