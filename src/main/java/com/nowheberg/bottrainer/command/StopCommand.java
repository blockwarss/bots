package com.nowheberg.bottrainer.command;

import com.nowheberg.bottrainer.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StopCommand implements CommandExecutor {
    private final SessionManager sessions;
    public StopCommand(SessionManager s) { this.sessions = s; }

    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("trainbot.use")) { sender.sendMessage("§cPermission manquante (trainbot.use)."); return true; }

        if (args.length == 0) {
            if (sender instanceof Player p) { sessions.stopSession(p.getUniqueId(), true); }
            else sender.sendMessage("§cPrécise un joueur : /botstop <joueur>");
            return true;
        }

        Player t = Bukkit.getPlayerExact(args[0]);
        if (t == null) { sender.sendMessage("§cJoueur introuvable."); return true; }
        if (!sender.hasPermission("trainbot.admin")) {
            sender.sendMessage("§cPermission manquante (trainbot.admin) pour stopper un autre joueur.");
            return true;
        }
        sessions.stopSession(t.getUniqueId(), true);
        return true;
    }
}
