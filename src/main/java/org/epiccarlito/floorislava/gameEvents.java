package org.epiccarlito.floorislava;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class gameEvents implements Listener {
    private final FloorIsLava plugin;
    private final gameLogic game;

    public gameEvents(FloorIsLava plugin) {
        this.plugin = plugin;
        game = plugin.gameLogic;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!game.activeGame) return;

        Player player = event.getEntity();
        String playerName = player.getName();
        Location targetLocation = player.getLocation().add(0, 1, 0);

        if (game.gracePeriod > 0 && game.graceProgress > 0) {
            player.sendMessage(plugin.PLUGIN_NAME + "You can respawn in grace period!");
        } else {
            if (game.playersAlive.contains(player)) {
                game.world.strikeLightningEffect(targetLocation);
                plugin.getServer().broadcastMessage(plugin.PLUGIN_NAME + playerName + " has been eliminated!");
                player.setGameMode(GameMode.SPECTATOR);
                game.playersAlive.remove(player);
            }
        }

        if (game.playersNeeded == 1 && game.playersAlive.isEmpty()) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendTitle(ChatColor.RED + "GAME OVER", "", 10, 70, 20);
                game.endGame(null);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BossBar bossBar = game.bossBar;

        if (game.activeGame) {
            bossBar.addPlayer(player);
        }
    }
}
