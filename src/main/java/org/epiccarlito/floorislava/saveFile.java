package org.epiccarlito.floorislava;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class saveFile {
    private final FloorIsLava plugin;
    private final File filePath;
    private FileConfiguration savedConfig;

    public saveFile(FloorIsLava plugin) {
        this.plugin = plugin;
        filePath = new File(plugin.getDataFolder(), "save.yml");
    }

    public FileConfiguration findFile() {
        if (filePath.exists()) {
            plugin.getLogger().info("Found existing game data");
            savedConfig = YamlConfiguration.loadConfiguration(filePath);
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

    public void saveConfig(FileConfiguration newConfig) {
        if (filePath == null || newConfig == null) return;
        savedConfig = newConfig;

        try {
            File saveFile = new File(plugin.getDataFolder(), "save.yml");
            savedConfig.save(saveFile);
            plugin.getLogger().info("Saved game data");
        } catch (IOException e) {
            plugin.getLogger().info("Unable to save the game data");
        }
    }
}
