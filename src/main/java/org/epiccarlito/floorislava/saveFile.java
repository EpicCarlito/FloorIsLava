package org.epiccarlito.floorislava;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class saveFile {
    private FloorIsLava plugin;

    public saveFile(FloorIsLava plugin) {
        this.plugin = plugin;
    }

    public YamlConfiguration findFile() {
        File saveFile = new File(plugin.getDataFolder(), "save.yml");
        if (saveFile.exists()) {
            plugin.getLogger().info("Found existing game data");
            return YamlConfiguration.loadConfiguration(saveFile);
        }
        return null;
    }

    public void shutdown() {
        File saveFile = new File(plugin.getDataFolder(), "save.yml");
        if (!saveFile.exists()) {
            plugin.getLogger().info("Found existing game data");
        }
    }
}
