package com.nowheberg.bottrainer.command;

import com.nowheberg.bottrainer.arena.Arena;
import com.nowheberg.bottrainer.arena.ArenaManager;
import com.nowheberg.bottrainer.bot.BotMode;
import com.nowheberg.bottrainer.bot.Difficulty;
import com.nowheberg.bottrainer.session.SessionManager;
import com.nowheberg.bottrainer.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BotCommand implements CommandExecutor, TabCompleter {
    private final SessionManager sessions; private final ArenaManager arenas;
    public BotCommand(SessionManager s, ArenaManager a) { this.sessions = s; this.arenas = a; }

    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length < 4) { sender.sendMessage("§cUsage: /"+label+" <joueur> <difficulte> <mode> <temps>"); return true; }
        if (!sender.hasPermission("trainbot.use")) { sender.sendMessage("§cPermission manquante (trainbot.use)."); return true; }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { sender.sendMessage("§cJoueur introuvable."); return true; }

        if (!sender.equals(target) && !sender.hasPermission("trainbot.admin")) {
            sender.sendMessage("§cTu n'as pas la permission de démarrer pour un autre joueur (trainbot.admin).");
            return true;
        }

        Difficulty diff;
        try { diff = Difficulty.valueOf(args[1].toUpperCase()); }
        catch (IllegalArgumentException ex) { sender.sendMessage("§cDifficulté invalide (EASY/NORMAL/HARD/INSANE)."); return true; }

        BotMode mode;
        try { mode = BotMode.valueOf(args[2].toUpperCase()); }
        catch (IllegalArgumentException ex) { sender.sendMessage("§cMode invalide (BASIC/MACE/CRYSTAL)."); return true; }

        long durationTicks = TimeUtil.parseToTicks(args[3]);
        if (durationTicks <= 0) { sender.sendMessage("§cTemps invalide. Ex: 90s, 2m, 1m30s"); return true; }

        Arena arena = arenas.load();
        sessions.startFixedSession(target, arena, diff, mode, durationTicks);
        sender.sendMessage("§aTéléportation dans l'arène et démarrage de l'entraînement...");
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        switch (args.length) {
            case 1 -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            case 2 -> { for (var d : Difficulty.values()) out.add(d.name()); }
            case 3 -> { for (var m : BotMode.values()) out.add(m.name()); }
            case 4 -> out.addAll(List.of("60s","90s","2m","3m","5m"));
        }
        return out;
    }
}
