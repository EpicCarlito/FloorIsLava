package org.epiccarlito.floorislava;

import org.bukkit.GameMode;
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
        Player player  = event.getEntity();
        String playerName = player.getName();

        if (game.gracePeriod > 0 && game.graceProgress > 0) {
            player.sendMessage(plugin.PLUGIN_NAME + "You can respawn in grace period!");
        } else {
            if (game.playersAlive.contains(player)) {
                plugin.getServer().broadcastMessage(plugin.PLUGIN_NAME + playerName + " has been eliminated!");
                player.setGameMode(GameMode.SPECTATOR);
                game.playersAlive.remove(player);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player  = event.getPlayer();
        BossBar bossBar = game.bossBar;

        if (game.activeGame) {
            bossBar.addPlayer(player);
        }
    }
}
