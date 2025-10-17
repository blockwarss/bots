package com.nowheberg.bottrainer.bot.impl;

import com.nowheberg.bottrainer.BotTrainerPlugin;
import com.nowheberg.bottrainer.bot.BotController;
import com.nowheberg.bottrainer.bot.BotMode;
import com.nowheberg.bottrainer.bot.Difficulty;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vindicator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Random;

public class SimpleBot implements BotController {
    private final BotMode mode; private final Difficulty difficulty; private final Location spawn;
    private final Player target; private final double hitRange; private final int attackCd;
    private final double speed; private final double strafe;
    private LivingEntity entity; private BukkitTask task; private int attackTick;
    private final Random random = new Random();

    public SimpleBot(Player target, Location spawn, BotMode mode, Difficulty diff, double baseHitRange, int baseCd, double speed, double strafe) {
        this.target = target; this.spawn = spawn; this.mode = mode; this.difficulty = diff;
        this.hitRange = baseHitRange; this.attackCd = baseCd; this.speed = speed; this.strafe = strafe;
    }

    @Override public LivingEntity entity() { return entity; }
    @Override public BotMode mode() { return mode; }
    @Override public Difficulty difficulty() { return difficulty; }
    @Override public Location spawnPoint() { return spawn; }

    @Override public void start() {
        Location loc = spawn.clone();
        if (target.isOnline() && loc.getWorld() != null && target.getWorld() != null && loc.getWorld().equals(target.getWorld())) {
            if (loc.distanceSquared(target.getLocation()) < 4.0) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                loc.add(Math.cos(angle) * 2.0, 0, Math.sin(angle) * 2.0);
            }
        }
        Vindicator vind = (Vindicator) loc.getWorld().spawnEntity(loc, EntityType.VINDICATOR);
        vind.setCustomNameVisible(true);
        vind.setCustomName("§6Training Bot §7[" + mode + "/" + difficulty + "]");
        vind.setAI(false);
        vind.setPersistent(false);
        vind.setCanPickupItems(false);
        vind.setRemoveWhenFarAway(false);
        vind.getEquipment().setItemInMainHand(new ItemStack(org.bukkit.Material.NETHERITE_SWORD));
        vind.getEquipment().setItemInOffHand(new ItemStack(org.bukkit.Material.TOTEM_OF_UNDYING));
        vind.getEquipment().setHelmet(new ItemStack(org.bukkit.Material.NETHERITE_HELMET));
        vind.getEquipment().setChestplate(new ItemStack(org.bukkit.Material.NETHERITE_CHESTPLATE));
        vind.getEquipment().setLeggings(new ItemStack(org.bukkit.Material.NETHERITE_LEGGINGS));
        vind.getEquipment().setBoots(new ItemStack(org.bukkit.Material.NETHERITE_BOOTS));
        AttributeInstance maxHealth = vind.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(BotTrainerPlugin.get().getConfig().getDouble("settings.bot.baseHealth", 40.0));
        vind.setHealth(vind.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        vind.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false, false));
        this.entity = vind;

        this.task = BotTrainerPlugin.get().getServer().getScheduler().runTaskTimer(BotTrainerPlugin.get(), () -> tick(target), 1L, 1L);
    }

    @Override public void stop() {
        if (task != null) task.cancel();
        if (entity != null && !entity.isDead()) entity.remove();
    }

    @Override public void tick(Player target) {
        if (entity == null || entity.isDead() || !target.isOnline()) { stop(); return; }
        Location el = entity.getLocation();
        Location tl = target.getLocation();

        var toTarget = tl.toVector().subtract(el.toVector());
        double dist2 = toTarget.lengthSquared();
        if (Double.isNaN(dist2) || dist2 < 1.0e-6) { return; }
        var dir = toTarget.clone().normalize();

        org.bukkit.util.Vector move;
        switch (mode) {
            case BASIC -> {
                var v = dir.multiply(speed);
                var side = new org.bukkit.util.Vector(-dir.getZ(), 0, dir.getX()).multiply(Math.sin(System.currentTimeMillis()*0.004) * 0.15 * strafe);
                move = v.add(side);
                tryAttack(target);
            }
            case MACE -> {
                var jitter = new org.bukkit.util.Vector((random.nextDouble()-0.5)*0.6*strafe, 0, (random.nextDouble()-0.5)*0.6*strafe);
                move = dir.multiply(speed*1.1).add(jitter);
            }
            case CRYSTAL -> {
                var side = new org.bukkit.util.Vector(-dir.getZ(), 0, dir.getX());
                move = side.multiply((random.nextDouble()-0.5) * 0.8 * strafe).add(dir.multiply((random.nextDouble()-0.5) * 0.2));
            }
            default -> move = new org.bukkit.util.Vector();
        }

        if (Double.isFinite(move.getX()) && Double.isFinite(move.getY()) && Double.isFinite(move.getZ())) {
            Location next = el.clone().add(move.getX(), 0.0, move.getZ());
            next.setDirection(dir);
            entity.teleport(next);
        }

        if (el.distanceSquared(spawn) > 150*150) { entity.teleport(spawn); }
    }

    private void tryAttack(Player target) {
        if (attackTick > 0) { attackTick--; return; }
        double range = hitRange;
        if (difficulty == Difficulty.HARD) range += 0.2; else if (difficulty == Difficulty.INSANE) range += 0.35;
        if (entity.getLocation().distanceSquared(target.getLocation()) <= range*range) {
            attackTick = attackCd;
            target.damage(3.0, entity);
            entity.swingMainHand();
            entity.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.7f, 1.0f);
        }
    }

    @Override public void onArenaDamage(double finalDamage) {
        if (entity.getHealth() - finalDamage <= 0) {
            entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
            entity.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, entity.getLocation().add(0,1,0), 40, 0.4, 0.6, 0.4, 0.1);
            entity.setHealth(Math.min(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), entity.getHealth() + 14.0));
        }
    }
}
