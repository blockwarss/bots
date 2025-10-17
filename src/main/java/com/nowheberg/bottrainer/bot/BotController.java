package com.nowheberg.bottrainer.bot;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface BotController {
    LivingEntity entity();
    void start();
    void stop();

    void tick(Player target);
    void onArenaDamage(double finalDamage);
    BotMode mode();
    Difficulty difficulty();
    Location spawnPoint();
}
