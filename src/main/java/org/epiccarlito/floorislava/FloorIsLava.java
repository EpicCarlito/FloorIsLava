package org.epiccarlito.floorislava;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class FloorIsLava extends JavaPlugin {
    public final saveFile saveFile = new saveFile(this);
    public gameLogic gameLogic;
    public FileConfiguration savedConfig;
    public String PLUGIN_NAME = ChatColor.GOLD + "[FloorIsLava] " + ChatColor.WHITE;

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
        Bukkit.getScheduler().cancelTasks(this);

        saveFile.saveConfig();
        World world = Bukkit.getWorld(Objects.requireNonNull(savedConfig.getString("world")));
        assert world != null;
        WorldBorder border = world.getWorldBorder();
        border.setSize(savedConfig.getDouble("borderSize"));

        getLogger().info("Plugin Disabled");
    }
}
