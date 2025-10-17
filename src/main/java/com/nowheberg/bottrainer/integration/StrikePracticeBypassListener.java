package com.nowheberg.bottrainer.integration;

import com.nowheberg.bottrainer.BotTrainerPlugin;
import com.nowheberg.bottrainer.arena.Arena;
import com.nowheberg.bottrainer.session.SessionManager;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class StrikePracticeBypassListener implements Listener {

    public static final String META_KEY = "bottrainer_session";

    private final SessionManager sessions;
    private final StrikePracticeHook hook;

    public StrikePracticeBypassListener(SessionManager sessions, StrikePracticeHook hook) {
        this.sessions = sessions;
        this.hook = hook;
    }

    private boolean isOurTrainee(Player p) { return p != null && p.hasMetadata(META_KEY); }

    private boolean withinArena(Player p) {
        Arena a = sessions.getActiveArena(p.getUniqueId());
        return a != null && a.contains(p.getLocation());
    }

    private boolean inRealSpFight(Player p) { return hook != null && hook.isPresent() && hook.isInFight(p); }

    private void dbg(Player p, String msg) {
        if (BotTrainerPlugin.get().getConfig().getBoolean("settings.debug", false)) {
            BotTrainerPlugin.get().getLogger().info("[SP-Bypass] " + p.getName() + ": " + msg);
        }
    }

    // EARLY pass : autoriser l'interaction
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteractLowest(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isOurTrainee(p) || inRealSpFight(p) || !withinArena(p)) return;
        ItemStack item = e.getItem(); if (item == null) return;
        Material m = item.getType();
        if (m == Material.WIND_CHARGE || m == Material.FIREWORK_ROCKET || m == Material.END_CRYSTAL) {
            e.setUseItemInHand(Event.Result.ALLOW);
            e.setUseInteractedBlock(Event.Result.ALLOW);
            e.setCancelled(false);
            dbg(p, "ALLOW (LOWEST) " + m);
        }
    }

    // NORMAL/HIGHEST pass : maintenir autorisé
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractHighest(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isOurTrainee(p) || inRealSpFight(p) || !withinArena(p)) return;
        ItemStack item = e.getItem(); if (item == null) return;
        Material m = item.getType();
        if (m == Material.WIND_CHARGE || m == Material.FIREWORK_ROCKET || m == Material.END_CRYSTAL) {
            e.setCancelled(false);
            dbg(p, "Uncancel (HIGHEST) " + m);
        }
    }

    // Projectile lancé (WindCharge inclus)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        Projectile proj = e.getEntity();
        if (!(proj.getShooter() instanceof Player p)) return;
        if (!isOurTrainee(p) || inRealSpFight(p) || !withinArena(p)) return;
        e.setCancelled(false);
        dbg(p, "Uncancel ProjectileLaunch " + proj.getType());
    }

    // Forcer l'explosion de WindCharge à l'impact (si un autre plugin empêche son comportement)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onProjectileHit(ProjectileHitEvent e) {
        Projectile proj = e.getEntity();
        if (!(proj instanceof WindCharge wc)) return;
        if (!(proj.getShooter() instanceof Player p)) return;
        if (!isOurTrainee(p) || inRealSpFight(p) || !withinArena(p)) return;
        try {
            // Si SP a absorbé l'effet, déclencher nous-même
            wc.explode();
            dbg(p, "Force explode WindCharge on hit");
        } catch (Throwable t) {
            // rien, juste éviter un crash si l'API diffère
        }
    }

    // End Crystal : certains plugins annulent le place/spawn/explosion
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!isOurTrainee(p) || inRealSpFight(p) || !withinArena(p)) return;
        if (e.getBlockPlaced().getType() == Material.END_CRYSTAL) {
            e.setCancelled(false);
            dbg(p, "Uncancel BlockPlace END_CRYSTAL");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof EnderCrystal)) return;
        Player nearest = e.getEntity().getWorld().getNearbyPlayers(e.getLocation(), 6.0).stream().findFirst().orElse(null);
        if (nearest == null) return;
        if (!isOurTrainee(nearest) || inRealSpFight(nearest) || !withinArena(nearest)) return;
        e.setCancelled(false);
        dbg(nearest, "Uncancel EntitySpawn END_CRYSTAL");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof EnderCrystal)) return;
        Player nearest = ent.getWorld().getNearbyPlayers(ent.getLocation(), 6.0).stream().findFirst().orElse(null);
        if (nearest == null) return;
        if (!isOurTrainee(nearest) || inRealSpFight(nearest) || !withinArena(nearest)) return;
        e.setCancelled(false);
        dbg(nearest, "Uncancel EntityExplode END_CRYSTAL");
    }

    // Double-pass sur les dégâts pour contrer un re-cancel agressif
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDamageLowest(EntityDamageByEntityEvent e) { uncancelDamage(e, "LOWEST"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onDamageMonitor(EntityDamageByEntityEvent e) { uncancelDamage(e, "MONITOR"); }

    private void uncancelDamage(EntityDamageByEntityEvent e, String from) {
        Entity victim = e.getEntity();
        Entity damager = e.getDamager();
        Player pDamager = (damager instanceof Player) ? (Player) damager : null;
        Player pVictim  = (victim  instanceof Player) ? (Player) victim  : null;

        if (pDamager != null && isOurTrainee(pDamager) && !inRealSpFight(pDamager) && withinArena(pDamager)) {
            e.setCancelled(false);
            dbg(pDamager, "Uncancel Damage (" + from + ") -> " + victim.getType());
            return;
        }
        if (pVictim != null && isOurTrainee(pVictim) && !inRealSpFight(pVictim) && withinArena(pVictim)) {
            e.setCancelled(false);
            dbg(pVictim, "Uncancel Damage (" + from + ") from " + (damager==null ? "null" : damager.getType().name()));
        }
    }
}
