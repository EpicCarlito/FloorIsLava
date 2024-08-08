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
    private gameLogic game;
    private final File filePath;
    private FileConfiguration savedConfig;

    public saveFile(FloorIsLava plugin) {
        this.plugin = plugin;
        game = plugin.gameLogic;
        filePath = new File(plugin.getDataFolder(), "save.yml");
    }

    public FileConfiguration findFile() {
        if (filePath.exists()) {
            savedConfig = YamlConfiguration.loadConfiguration(filePath);
            plugin.getLogger().info("Found existing game data");
            return savedConfig;
        }
        return null;
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
        if (filePath.exists()) {
            filePath.delete();
            plugin.getLogger().info("Deleted existing game data");
        }
    }

    public void saveConfig(FileConfiguration newConfig) {
        if (filePath == null || newConfig == null) return;
        savedConfig = newConfig;

        if (game == null) {
            game = plugin.gameLogic;
        }

        List<String> playerUUIDs = new ArrayList<>();
        for (Player player : game.playersAlive) {
            playerUUIDs.add(player.getUniqueId().toString());
            plugin.getLogger().info(player.getUniqueId().toString());
        }

        try {
            File saveFile = new File(plugin.getDataFolder(), "save.yml");

            savedConfig.set("activeGame", true);
            savedConfig.set("risingBlock", game.risingBlock);
            savedConfig.set("clearActionBar", game.clearActionBar);
            savedConfig.set("startingHeight", game.startingHeight);
            savedConfig.set("heightIncrease", game.heightIncrease);
            savedConfig.set("heightDelay", game.heightDelay);
            savedConfig.set("gracePeriod", game.gracePeriod);
            savedConfig.set("graceProgress", game.graceProgress);
            savedConfig.set("borderSize", game.borderSize);
            savedConfig.set("startingHeight", game.startingHeight);
            savedConfig.set("playersAlive", playerUUIDs);
            savedConfig.set("startPosition.x", game.startPosition.getX());
            savedConfig.set("startPosition.z", game.startPosition.getZ());

            savedConfig.save(saveFile);
            plugin.getLogger().info("Saved game data");
        } catch (IOException e) {
            plugin.getLogger().info("Unable to save the game data");
        }
    }
}
