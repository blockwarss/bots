package com.nowheberg.bottrainer.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class ArenaManager {
    private final Plugin plugin;
    private final FileConfiguration cfg;

    public ArenaManager(Plugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    public Arena load() {
        String world = cfg.getString("arena.world", "world");
        World w = Bukkit.getWorld(world);
        if (w == null && !Bukkit.getWorlds().isEmpty()) {
            w = Bukkit.getWorlds().get(0);
            world = w.getName();
        }
        Location spawn = new Location(w,
                cfg.getDouble("arena.spawn.x", 0.5),
                cfg.getDouble("arena.spawn.y", 64.0),
                cfg.getDouble("arena.spawn.z", 0.5),
                (float) cfg.getDouble("arena.spawn.yaw", 0.0),
                (float) cfg.getDouble("arena.spawn.pitch", 0.0));
        Location p1 = new Location(w,
                cfg.getDouble("arena.bounds.pos1.x", spawn.getX() - 10),
                cfg.getDouble("arena.bounds.pos1.y", spawn.getY() - 5),
                cfg.getDouble("arena.bounds.pos1.z", spawn.getZ() - 10));
        Location p2 = new Location(w,
                cfg.getDouble("arena.bounds.pos2.x", spawn.getX() + 10),
                cfg.getDouble("arena.bounds.pos2.y", spawn.getY() + 5),
                cfg.getDouble("arena.bounds.pos2.z", spawn.getZ() + 10));
        return new Arena(world, spawn, p1, p2);
    }

    public void saveArena(Arena a) {
        cfg.set("arena.world", a.worldName());
        cfg.set("arena.spawn.x", a.spawn().getX());
        cfg.set("arena.spawn.y", a.spawn().getY());
        cfg.set("arena.spawn.z", a.spawn().getZ());
        cfg.set("arena.spawn.yaw", a.spawn().getYaw());
        cfg.set("arena.spawn.pitch", a.spawn().getPitch());
        if (a.pos1() != null) { cfg.set("arena.bounds.pos1.x", a.pos1().getX()); cfg.set("arena.bounds.pos1.y", a.pos1().getY()); cfg.set("arena.bounds.pos1.z", a.pos1().getZ()); }
        if (a.pos2() != null) { cfg.set("arena.bounds.pos2.x", a.pos2().getX()); cfg.set("arena.bounds.pos2.y", a.pos2().getY()); cfg.set("arena.bounds.pos2.z", a.pos2().getZ()); }
        plugin.saveConfig();
    }

    public void setSpawn(Player p) {
        var w = p.getWorld().getName();
        var a = load();
        a.setWorldName(w);
        a.setSpawn(p.getLocation());
        saveArena(a);
        plugin.saveConfig();
    }

    public void setBounds(@Nullable Location p1, @Nullable Location p2) {
        var a = load();
        if (p1 != null) a.setPos1(p1);
        if (p2 != null) a.setPos2(p2);
        saveArena(a);
        plugin.saveConfig();
    }
}
