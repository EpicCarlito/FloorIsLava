package org.epiccarlito.floorislava;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class FloorIsLava extends JavaPlugin {
    public final saveFile saveFile = new saveFile(this);
    public gameLogic gameLogic;
    public FileConfiguration savedConfig;
    public String PLUGIN_NAME = ChatColor.RED + "[FloorIsLava] " + ChatColor.WHITE;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        savedConfig = saveFile.findFile();
        gameLogic = new gameLogic(this);
        Objects.requireNonNull(getCommand("floorislava")).setExecutor(new commands(this));
        getServer().getPluginManager().registerEvents(new gameEvents(this), this);
        getLogger().info("Plugin Enabled");
    }

    @Override
    public void onDisable() {
        saveFile.saveConfig();
        getLogger().info("Plugin Disabled");
    }
}
