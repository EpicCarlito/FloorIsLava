package org.epiccarlito.floorislava;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class saveFile {
    private final FloorIsLava plugin;
    private final File filePath;
    private gameLogic game;
    private FileConfiguration savedConfig;

    public saveFile(FloorIsLava plugin) {
        this.plugin = plugin;
        game = plugin.gameLogic;
        filePath = new File(plugin.getDataFolder(), "save.yml");
    }

    public FileConfiguration findFile() {
        if (filePath.exists() && filePath.length() > 0) {
            savedConfig = YamlConfiguration.loadConfiguration(filePath);
            plugin.getLogger().info("Found existing game data");
        } else {
            savedConfig = null;
        }
        return savedConfig;
    }

    public YamlConfiguration createConfig() {
        try {
            if (!filePath.exists()) {
                filePath.createNewFile();
            }
            return YamlConfiguration.loadConfiguration(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteFile() {
        if (filePath.exists() && filePath.delete()) {
            plugin.getLogger().info("Deleted existing game data");
        } else {
            plugin.getLogger().info("Failed to delete save.yml");
        }
    }

    public void saveConfig() {
        if (game == null) {
            game = plugin.gameLogic;
        }

        if (!game.activeGame) return;

        List<String> playerUUIDs = new ArrayList<>();
        if (game.playerUUIDs != null) {
            playerUUIDs.addAll(game.playerUUIDs);
        } else {
            for (Player player : game.playersAlive) {
                playerUUIDs.add(player.getUniqueId().toString());
            }
        }

        try {
            if (savedConfig == null) {
                savedConfig = createConfig();
            }

            savedConfig.set("activeGame", true);
            savedConfig.set("risingBlock", game.risingBlock);
            savedConfig.set("clearActionBar", game.clearActionBar);
            savedConfig.set("startingHeight", game.startingHeight);
            savedConfig.set("heightIncrease", game.heightIncrease);
            savedConfig.set("heightDelay", game.heightDelay);
            savedConfig.set("gracePeriod", game.gracePeriod);
            savedConfig.set("graceProgress", game.graceProgress);
            savedConfig.set("borderSize", game.borderSize);
            savedConfig.set("world", game.world.getName());
            savedConfig.set("playersAlive", playerUUIDs);
            savedConfig.set("startPosition.x", game.startPosition.getX());
            savedConfig.set("startPosition.z", game.startPosition.getZ());

            savedConfig.save(filePath);
            plugin.getLogger().info("Saved game data");
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to save game data: " + e.getMessage());
        }
    }
}
