package org.epiccarlito.floorislava;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class commands implements CommandExecutor, TabCompleter {
    private final String INSUFFICIENT_ARGS;
    public FloorIsLava plugin;
    public gameLogic game;

    public commands(FloorIsLava plugin) {
        this.plugin = plugin;
        game = plugin.gameLogic;

        INSUFFICIENT_ARGS = plugin.PLUGIN_NAME + "Insufficient or unknown arguments.";
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) return true;

        if (!commandSender.hasPermission("floorislava.commands")) {
            commandSender.sendMessage(plugin.PLUGIN_NAME + "You do not have access to this command!");
            return true;
        }

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
                    case "load": {
                        if (game.activeGame) {
                            player.sendMessage(plugin.PLUGIN_NAME + "A game is currently in session");
                        } else {
                            game.loadGame(player);
                        }

                        break;
                    }
                    case "end": {
                        game.endGame(player);
                        break;
                    }
                    default: {
                        player.sendMessage(INSUFFICIENT_ARGS);
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
                return Arrays.asList("help", "start", "load", "end");
            }
        }
        return null;
    }

    public void help(Player player) {
        player.sendMessage(plugin.PLUGIN_NAME + "Commands:");
        player.sendMessage(ChatColor.BOLD + "/fl start" + ChatColor.RESET + " - Starts a new match");
        player.sendMessage(ChatColor.BOLD + "/fl load" + ChatColor.RESET + " - Loads an existing match");
        player.sendMessage(ChatColor.BOLD + "/fl end" + ChatColor.RESET + " - Ends the current match");
    }
}
