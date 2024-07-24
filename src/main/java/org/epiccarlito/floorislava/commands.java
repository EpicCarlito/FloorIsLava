package org.epiccarlito.floorislava;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class commands implements CommandExecutor, TabCompleter {
    public FloorIsLava plugin;
    public gameLogic game;

    public commands(FloorIsLava plugin) {
        this.plugin = plugin;
        game = plugin.gameLogic;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) return true;
        Player player = (Player) commandSender;

        switch (args.length) {
            case 0: {
                help(player);
                break;
            }
            case 1: {
                switch (args[0]) {
                    case "help": {
                        help(player);
                        break;
                    }
                    case "start": {
                        game.startGame(player);
                        break;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        switch (args.length) {
            case 1: {
                return Arrays.asList("help", "start");
            }
        }
        return null;
    }

    public void help(Player player) {
        player.sendMessage("Help Test Message");
    }
}