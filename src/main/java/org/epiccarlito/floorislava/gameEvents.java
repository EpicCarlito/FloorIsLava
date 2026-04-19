package org.epiccarlito.floorislava;

import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

        if ((game.graceProgress / 20) < game.gracePeriod) {
            return;
        }

        if (game.playersAlive.contains(player)) {
            LightningStrike lightning = player.getWorld().strikeLightningEffect(targetLocation);
            lightning.setSilent(true);
            for (Player alivePlayer : Bukkit.getOnlinePlayers()) {
                alivePlayer.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
            }
            plugin.getServer().broadcastMessage(plugin.PLUGIN_NAME + playerName + " has been eliminated!");
            player.setGameMode(GameMode.SPECTATOR);
            game.playersAlive.remove(player);
        }

        if (game.playersNeeded == 1 && game.playersAlive.isEmpty()) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendTitle(ChatColor.RED + "GAME OVER", "", 10, 70, 20);
            }
            game.endGame();
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

    @EventHandler
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Firework firework)) return;
        if (firework.hasMetadata("winnerFirework")) event.setCancelled(true);
    }
}
