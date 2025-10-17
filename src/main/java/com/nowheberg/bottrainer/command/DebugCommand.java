package com.nowheberg.bottrainer.command;

import com.nowheberg.bottrainer.session.SessionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DebugCommand implements CommandExecutor {
    private final SessionManager sessions;
    public DebugCommand(SessionManager s) { this.sessions = s; }

    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cCommande en jeu uniquement."); return true; }
        if (!sender.hasPermission("trainbot.admin")) { sender.sendMessage("§cPermission manquante (trainbot.admin)."); return true; }
        var a = sessions.getActiveArena(p.getUniqueId());
        boolean in = a != null && a.contains(p.getLocation());
        boolean active = sessions.hasSession(p.getUniqueId());
        sender.sendMessage("§7Session: " + (active ? "§aOUI" : "§cNON") + " §8| §7Dans arène: " + (in ? "§aOUI" : "§cNON"));
        return true;
    }
}
