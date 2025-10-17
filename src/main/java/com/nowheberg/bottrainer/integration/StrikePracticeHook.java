package com.nowheberg.bottrainer.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public final class StrikePracticeHook {
    private boolean present;

    public void init() {
        Plugin sp = Bukkit.getPluginManager().getPlugin("StrikePractice");
        present = sp != null && sp.isEnabled();
    }

    public boolean isPresent() { return present; }

    public boolean isInFight(Player player) {
        if (!present) return false;
        try {
            Class<?> spMain = Class.forName("ga.strikepractice.StrikePractice");
            Method getAPI = spMain.getMethod("getAPI");
            Object api = getAPI.invoke(null);
            if (api == null) return false;
            Method isInFight = api.getClass().getMethod("isInFight", Player.class);
            Object result = isInFight.invoke(api, player);
            return result instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }
}
