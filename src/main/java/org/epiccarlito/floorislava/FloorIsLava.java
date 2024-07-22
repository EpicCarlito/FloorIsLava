package org.epiccarlito.floorislava;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class FloorIsLava extends JavaPlugin {
    private final saveFile saveFile = new saveFile(this);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration gameData = saveFile.findFile();
        gameLogic game = new gameLogic(this, gameData);
        getCommand("floorislava").setExecutor(new commands(this, game));
        getLogger().info("Plugin Enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin Disabled");
    }
}
