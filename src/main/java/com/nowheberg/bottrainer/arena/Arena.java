package com.nowheberg.bottrainer.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Arena {
    private String worldName;
    private Location spawn;
    private Location pos1, pos2;

    public Arena(String worldName, Location spawn, Location p1, Location p2) {
        this.worldName = worldName; this.spawn = spawn; this.pos1 = p1; this.pos2 = p2;
    }

    public World world() { return Bukkit.getWorld(worldName); }
    public String worldName() { return worldName; }
    public Location spawn() { return spawn; }
    public Location pos1() { return pos1; }
    public Location pos2() { return pos2; }

    public void setWorldName(String w) { this.worldName = w; }
    public void setSpawn(Location s) { this.spawn = s; }
    public void setPos1(Location l) { this.pos1 = l; }
    public void setPos2(Location l) { this.pos2 = l; }

    public boolean isInside(Location loc) {
        if (pos1 == null || pos2 == null || loc == null || !loc.getWorld().getName().equals(worldName)) return true; // sans bounds = pas de restriction
        double minX = Math.min(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
}
