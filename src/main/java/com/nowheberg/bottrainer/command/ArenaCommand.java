package com.nowheberg.bottrainer.command;

import com.nowheberg.bottrainer.arena.ArenaManager;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ArenaCommand implements CommandExecutor {
    private final ArenaManager arenas;
    public ArenaCommand(ArenaManager a) { this.arenas = a; }

    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("trainbot.admin")) { sender.sendMessage("§cPermission manquante (trainbot.admin)."); return true; }
        if (!(sender instanceof Player p)) { sender.sendMessage("§cCommande joueur uniquement."); return true; }
        if (args.length == 0) { sender.sendMessage("§e/"+label+" setspawn | setbounds <1|2> | info"); return true; }
        switch (args[0].toLowerCase()){
            case "setspawn" -> { arenas.setSpawn(p); sender.sendMessage("§aSpawn arène défini."); return true; }
            case "setbounds" -> {
                if (args.length < 2) { sender.sendMessage("§e/"+label+" setbounds <1|2>"); return true; }
                Location l = p.getLocation();
                if ("1".equals(args[1])) arenas.setBounds(a -> a.setPos1(l));
                else if ("2".equals(args[1])) arenas.setBounds(a -> a.setPos2(l));
                else { sender.sendMessage("§cChoisis 1 ou 2."); return true; }
                sender.sendMessage("§aBound "+args[1]+" enregistré.");
                return true;
            }
            case "info" -> { sender.sendMessage("§7Arène actuelle: voir config.yml"); return true; }
            default -> { sender.sendMessage("§e/"+label+" setspawn | setbounds <1|2> | info"); return true; }
        }
    }
}
