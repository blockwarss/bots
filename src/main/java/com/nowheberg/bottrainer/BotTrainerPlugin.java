package com.nowheberg.bottrainer;

import com.nowheberg.bottrainer.arena.ArenaManager;
import com.nowheberg.bottrainer.command.ArenaCommand;
import com.nowheberg.bottrainer.command.BotCommand;
import com.nowheberg.bottrainer.command.StopCommand;
import com.nowheberg.bottrainer.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BotTrainerPlugin extends JavaPlugin {
    private static BotTrainerPlugin instance;
    private ArenaManager arenaManager;
    private SessionManager sessionManager;

    public static BotTrainerPlugin get() { return instance; }

    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.arenaManager = new ArenaManager(this);
        this.sessionManager = new SessionManager(this, arenaManager);

        getCommand("bot").setExecutor(new BotCommand(sessionManager, arenaManager));
        getCommand("bot").setTabCompleter(new BotCommand(sessionManager, arenaManager));
        getCommand("botarena").setExecutor(new ArenaCommand(arenaManager));
        getCommand("botstop").setExecutor(new StopCommand(sessionManager));

        Bukkit.getPluginManager().registerEvents(sessionManager, this);
        getLogger().info("BotTrainer activé.");
    }

    @Override public void onDisable() {
        if (sessionManager != null) sessionManager.shutdown();
        getLogger().info("BotTrainer désactivé.");
    }
}
