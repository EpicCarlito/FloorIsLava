package org.epiccarlito.floorislava;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;

public class gameLogic {
    public FloorIsLava plugin;
    public FileConfiguration savedConfig;
    public FileConfiguration config;
    private World world;

    public boolean activeGame = false;
    public ArrayList<Player> playersAlive;
    public String risingBlock;
    public boolean forceTeleport;
    public boolean forceClear;

    public Integer borderSize;
    public Integer xPosition;
    public Integer zPosition;

    public gameLogic(FloorIsLava plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();

        if (savedConfig == null) {
            risingBlock = config.getString("risingBlock");
            forceTeleport = config.getBoolean("forceTeleport");
            forceClear = config.getBoolean("forceClear");

            borderSize = config.getInt("borderSize");
            xPosition = config.getInt("borderPosition.x");
            zPosition = config.getInt("borderPosition.z");
        } else {
            plugin.savedConfig = savedConfig;

            activeGame = savedConfig.getBoolean("activeGame");
            risingBlock = savedConfig.getString("risingBlock");
        }
    }

    public void startGame(Player player) {
        if (activeGame) {
            player.sendMessage("A game is currently in session");
            return;
        }

        if (!(risingBlock.contains("LAVA") || risingBlock.contains("WATER") || risingBlock.contains("VOID"))) {
            player.sendMessage("Invalid block in configuration. Select the following: ");
            return;
        }

        world = Bukkit.getServer().getWorlds().get(0);
        Location startPosition = new Location(world, xPosition, world.getHighestBlockYAt(xPosition, zPosition), zPosition);
        WorldBorder border = world.getWorldBorder();
        border.setCenter(startPosition);
        border.setSize(borderSize * 2);

        playersAlive = new ArrayList<>(Bukkit.getOnlinePlayers());

        savedConfig = plugin.saveFile.createConfig();
        if (savedConfig != null) {
            savedConfig.set("gameStatus", true);
            savedConfig.set("playersAlive", playersAlive);
        }

        for (Player alivePlayer : playersAlive) {
            if (forceTeleport) {
                alivePlayer.teleport(startPosition);
            }
            if (forceClear) {
                alivePlayer.getInventory().clear();
            }

            alivePlayer.setGameMode(GameMode.SURVIVAL);
            alivePlayer.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
            alivePlayer.setFoodLevel(20);
        }
    }
}
