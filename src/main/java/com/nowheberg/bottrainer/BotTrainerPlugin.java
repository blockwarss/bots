package com.nowheberg.bottrainer;

import com.nowheberg.bottrainer.arena.ArenaManager;
import com.nowheberg.bottrainer.command.ArenaCommand;
import com.nowheberg.bottrainer.command.BotCommand;
import com.nowheberg.bottrainer.command.StopCommand;
import com.nowheberg.bottrainer.integration.StrikePracticeBypassListener;
import com.nowheberg.bottrainer.integration.StrikePracticeHook;
import com.nowheberg.bottrainer.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BotTrainerPlugin extends JavaPlugin {
    private static BotTrainerPlugin instance;
    private ArenaManager arenaManager;
    private SessionManager sessionManager;
    private final StrikePracticeHook spHook = new StrikePracticeHook();

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

        spHook.init();
        if (spHook.isPresent()) {
            getLogger().info("[BotTrainer] StrikePractice détecté, activation du bypass.");
            Bukkit.getPluginManager().registerEvents(new StrikePracticeBypassListener(sessionManager, spHook), this);
        } else {
            getLogger().info("[BotTrainer] StrikePractice non détecté.");
        }
    }
}
