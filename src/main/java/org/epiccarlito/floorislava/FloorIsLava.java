package org.epiccarlito.floorislava;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class FloorIsLava extends JavaPlugin {
    public final saveFile saveFile = new saveFile(this);
    public gameLogic gameLogic;
    public FileConfiguration savedConfig;
    public World world;
    public String PLUGIN_NAME = ChatColor.RED + "[FloorIsLava] " + ChatColor.WHITE;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        world = Bukkit.getWorld("world");
        savedConfig = saveFile.findFile();
        gameLogic = new gameLogic(this);
        getCommand("floorislava").setExecutor(new commands(this));
        getLogger().info("Plugin Enabled");
    }

    @Override
    public void onDisable() {
        saveFile.saveConfig(gameLogic.savedConfig);
        getLogger().info("Plugin Disabled");
    }
}
