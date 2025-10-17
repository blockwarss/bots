package com.nowheberg.bottrainer.integration;

import com.nowheberg.bottrainer.BotTrainerPlugin;
import com.nowheberg.bottrainer.arena.Arena;
import com.nowheberg.bottrainer.session.SessionManager;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleGlideEvent;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isOurTrainee(p) || inRealSpFight(p) || !withinArena(p)) return;
        ItemStack item = e.getItem(); if (item == null) return;
        Material m = item.getType();
        // Autoriser Wind Charge, Firework, End Crystal
        if (m == Material.WIND_CHARGE || m == Material.FIREWORK_ROCKET || m == Material.END_CRYSTAL) {
            e.setCancelled(false);
            dbg(p, "Uncancel PlayerInteract for " + m);
        }
    }

    // Certaines versions d'End Crystal ne passent pas par BlockPlace : on garde ce handler si jamais
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
    public void onLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player p)) return;
        if (!isOurTrainee(p) || inRealSpFight(p) || !withinArena(p)) return;
        e.setCancelled(false);
        dbg(p, "Uncancel ProjectileLaunch " + e.getEntity().getType());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity victim = e.getEntity();
        Entity damager = e.getDamager();
        Player pDamager = (damager instanceof Player) ? (Player) damager : null;
        Player pVictim  = (victim  instanceof Player) ? (Player) victim  : null;

        if (pDamager != null && isOurTrainee(pDamager) && !inRealSpFight(pDamager) && withinArena(pDamager)) {
            e.setCancelled(false);
            dbg(pDamager, "Uncancel Damage -> " + victim.getType());
            return;
        }
        if (pVictim != null && isOurTrainee(pVictim) && !inRealSpFight(pVictim) && withinArena(pVictim)) {
            e.setCancelled(false);
            dbg(pVictim, "Uncancel Damage from " + (damager==null?"null":damager.getType().name()));
        }
    }

    // Elytra glide parfois bloqué par SP
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onToggleGlide(PlayerToggleGlideEvent e) {
        Player p = e.getPlayer();
        if (!isOurTrainee(p) || inRealSpFight(p) || !withinArena(p)) return;
        e.setCancelled(false);
        dbg(p, "Uncancel ToggleGlide -> " + e.isGliding());
    }

    // Spawn d'EnderCrystal annulé par certains plugins
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
}
