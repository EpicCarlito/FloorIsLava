package org.epiccarlito.floorislava;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class FloorIsLava extends JavaPlugin {
    public final saveFile saveFile = new saveFile(this);
    public final gameLogic gameLogic = new gameLogic(this);
    public FileConfiguration savedConfig;
    public World world;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        world = Bukkit.getWorld("world");
        savedConfig = saveFile.findFile();
        getCommand("floorislava").setExecutor(new commands(this));
        getLogger().info("Plugin Enabled");
    }

    @Override
    public void onDisable() {
        saveFile.shutdown();
        getLogger().info("Plugin Disabled");
    }
}
