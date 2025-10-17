package com.nowheberg.bottrainer.session;

import com.nowheberg.bottrainer.BotTrainerPlugin;
import com.nowheberg.bottrainer.arena.Arena;
import com.nowheberg.bottrainer.bot.BotMode;
import com.nowheberg.bottrainer.bot.Difficulty;
import com.nowheberg.bottrainer.bot.impl.SimpleBot;
import com.nowheberg.bottrainer.integration.StrikePracticeBypassListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager implements Listener {

    public static class Session {
        public final UUID playerId;
        public final PlayerSnapshot snapshot;
        public final long endTick;
        public final BotMode mode;
        public final Difficulty diff;
        public final SimpleBot bot;
        public final Arena arena;
        public final long startedTick;
        public Session(UUID playerId, PlayerSnapshot snapshot, long endTick, BotMode mode, Difficulty diff, SimpleBot bot, Arena arena) {
            this.playerId = playerId; this.snapshot = snapshot; this.endTick = endTick; this.mode = mode; this.diff = diff; this.bot = bot; this.arena = arena; this.startedTick = Bukkit.getCurrentTick();
        }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final BotTrainerPlugin plugin;
    private final com.nowheberg.bottrainer.arena.ArenaManager arenas;

    public SessionManager(BotTrainerPlugin plugin, com.nowheberg.bottrainer.arena.ArenaManager arenas) { this.plugin = plugin; this.arenas = arenas; }

    public void startFixedSession(Player p, Arena arena, Difficulty diff, BotMode mode, long durationTicks) {
        if (sessions.containsKey(p.getUniqueId())) stopSession(p.getUniqueId(), true);
        cleanupPlayerBots(p.getUniqueId(), arena);

        Location spawn = arena.spawn();
        if (spawn.getWorld()==null) { p.sendMessage("§cArène non configurée (monde invalide)."); return; }
        var snap = PlayerSnapshot.of(p);

        giveFixedKit(p);
        p.setHealth(p.getMaxHealth()); p.setFoodLevel(20);
        p.teleport(spawn);

        p.setMetadata(StrikePracticeBypassListener.META_KEY, new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        double baseRange = plugin.getConfig().getDouble("settings.bot.hitRange", 2.8);
        int baseCd = plugin.getConfig().getInt("settings.bot.attackCooldownTicks", 12);
        var diffSec = plugin.getConfig().getConfigurationSection("settings.difficulty."+diff.name());
        double speed = diffSec.getDouble("speed", 0.26);
        double strafe = diffSec.getDouble("strafe", 1.0);
        var bot = new SimpleBot(p, spawn, mode, diff, baseRange, baseCd, speed, strafe);
        bot.start();
        if (bot.entity()!=null) bot.entity().addScoreboardTag("bottrainer:" + p.getUniqueId());

        long endTick = Bukkit.getCurrentTick() + durationTicks;
        var session = new Session(p.getUniqueId(), snap, endTick, mode, diff, bot, arena);
        sessions.put(p.getUniqueId(), session);
        p.sendMessage("§aEntraînement démarré: §e"+mode+" §7| §b"+diff+" §7| §f"+(durationTicks/20)+"s");

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Session s = sessions.get(p.getUniqueId());
            if (s == null) return;
            if (!p.isOnline()) { stopSession(p.getUniqueId(), false); return; }
            if (Bukkit.getCurrentTick() >= s.endTick) stopSession(p.getUniqueId(), true);
        }, 20L, 20L);
    }

    public boolean hasSession(UUID id) { return sessions.containsKey(id); }

    public void stopSession(UUID playerId, boolean restore) {
        Session s = sessions.remove(playerId);
        if (s != null && s.bot != null) s.bot.stop();
        cleanupPlayerBots(playerId, s != null ? s.arena : null);
        var p = Bukkit.getPlayer(playerId);
        if (p != null) { p.removeMetadata(StrikePracticeBypassListener.META_KEY, plugin); }
        if (p != null && restore && s != null) {
            p.getInventory().clear(); p.getInventory().setArmorContents(null);
            s.snapshot.restore(p);
            p.sendMessage("§eEntraînement terminé. Inventaire et position restaurés.");
        }
    }

    private void cleanupPlayerBots(UUID playerId, Arena arena) {
        if (arena != null && arena.world() != null) {
            for (var e : arena.world().getEntities()) {
                if (e.getScoreboardTags().contains("bottrainer:" + playerId)) e.remove();
            }
        } else {
            Bukkit.getWorlds().forEach(w -> {
                for (var e : w.getEntities()) {
                    if (e.getScoreboardTags().contains("bottrainer:" + playerId)) e.remove();
                }
            });
        }
    }

    public void shutdown() { for (var id : new java.util.ArrayList<>(sessions.keySet())) stopSession(id, true); }

    @EventHandler public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) { stopSession(e.getPlayer().getUniqueId(), true); }

    @EventHandler public void onDeath(PlayerDeathEvent e) {
        UUID id = e.getEntity().getUniqueId();
        if (!sessions.containsKey(id)) return;
        e.setKeepInventory(true); e.setKeepLevel(true); e.getDrops().clear();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            var s = sessions.get(id); if (s == null) return; e.getEntity().spigot().respawn();
            e.getEntity().teleport(s.bot.spawnPoint());
        }, 1L);
    }

    @org.bukkit.event.EventHandler public void onBotDamage(org.bukkit.event.entity.EntityDamageEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.LivingEntity le)) return;
        if (!le.getScoreboardTags().stream().anyMatch(t -> t.startsWith("bottrainer:"))) return;
        double finalDamage = e.getFinalDamage();
        if (le.getHealth() - finalDamage <= 0) {
            e.setCancelled(true);
            le.setNoDamageTicks(15);
            le.getWorld().playSound(le.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 1f, 1f);
            le.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, le.getLocation().add(0,1,0), 40, 0.4, 0.6, 0.4, 0.1);
            double newHp = Math.min(le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(), le.getHealth() + 14.0);
            le.setHealth(newHp);
            if (le.getEquipment()!=null) le.getEquipment().setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
        }
    }

    @org.bukkit.event.EventHandler public void onPlayerDamage(org.bukkit.event.entity.EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        UUID id = p.getUniqueId();
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

    private void giveFixedKit(Player p) {
        p.getInventory().clear(); p.getInventory().setArmorContents(null);
        p.getInventory().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        p.getInventory().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        p.getInventory().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        p.getInventory().setBoots(new ItemStack(Material.NETHERITE_BOOTS));

        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD); sword.addUnsafeEnchantment(Enchantment.UNBREAKING, 5);
        ItemStack maceWB = new ItemStack(Material.MACE); maceWB.addUnsafeEnchantment(Enchantment.WIND_BURST, 2); maceWB.addUnsafeEnchantment(Enchantment.DENSITY, 5);
        ItemStack maceBR = new ItemStack(Material.MACE); maceBR.addUnsafeEnchantment(Enchantment.BREACH, 4);
        ItemStack elytra = new ItemStack(Material.ELYTRA); elytra.addUnsafeEnchantment(Enchantment.UNBREAKING, 10);
        ItemStack steaks = new ItemStack(Material.COOKED_BEEF, 64);

        ItemStack fw1 = firework(1, 64), fw1b = firework(1, 64);
        ItemStack fw2 = firework(2, 64), fw2b = firework(2, 64);
        ItemStack fw3 = firework(3, 64), fw3b = firework(3, 64);

        ItemStack wind = new ItemStack(Material.WIND_CHARGE, 64);
        ItemStack wind2 = wind.clone(), wind3 = wind.clone(), wind4 = wind.clone(), wind5 = wind.clone(), wind6 = wind.clone();
        ItemStack pearls = new ItemStack(Material.ENDER_PEARL, 16);
        ItemStack pearls2 = pearls.clone(), pearls3 = pearls.clone();

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

    private ItemStack firework(int power, int amount) {
        ItemStack it = new ItemStack(Material.FIREWORK_ROCKET, amount);
        var meta = it.getItemMeta();
        if (meta instanceof FireworkMeta fm) {
            fm.setPower(Math.max(1, Math.min(3, power)));
            it.setItemMeta(fm);
        }
        return it;
    }

    public Arena getActiveArena(UUID uuid) {
        Session s = sessions.get(uuid);
        return s != null ? s.arena : null;
    }
}
