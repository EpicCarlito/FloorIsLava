package org.epiccarlito.floorislava;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public class gameLogic {
    public FloorIsLava plugin;
    public FileConfiguration savedConfig;
    public FileConfiguration config;
    public saveFile saveFile;
    public World world;

    public boolean activeGame = false;
    public ArrayList<Player> playersAlive;
    public String risingBlock;
    public boolean forceTeleport;
    public boolean forceClear;

    public Integer heightDelay;
    public Integer gracePeriod;
    public Integer borderSize;
    public Integer xPosition;
    public Integer zPosition;

    BossBar bossBar;

    public gameLogic(FloorIsLava plugin) {
        this.plugin = plugin;
        saveFile = plugin.saveFile;
        savedConfig = plugin.savedConfig;
        config = plugin.getConfig();

        if (savedConfig == null) {
            risingBlock = config.getString("risingBlock");
            forceTeleport = config.getBoolean("forceTeleport");
            forceClear = config.getBoolean("forceClear");

            heightDelay = config.getInt("heightDelay");
            gracePeriod = config.getInt("gracePeriod");
            borderSize = config.getInt("borderSize");
            xPosition = config.getInt("borderPosition.x");
            zPosition = config.getInt("borderPosition.z");
        } else {
            activeGame = savedConfig.getBoolean("activeGame");
        }
    }

    public void startGame(Player player) {
        if (activeGame) {
            player.sendMessage(plugin.PLUGIN_NAME + "A game is currently in session");
            return;
        }

        if (!(risingBlock.contains("LAVA") || risingBlock.contains("WATER") || risingBlock.contains("VOID"))) {
            player.sendMessage(plugin.PLUGIN_NAME + "Invalid block in configuration. Select the following: ");
            return;
        }

        playersAlive = new ArrayList<>(Bukkit.getOnlinePlayers());
        world = Bukkit.getServer().getWorlds().get(0);

        Runnable initializeGame = () -> {
            Location startPosition = new Location(world, xPosition, world.getHighestBlockYAt(xPosition, zPosition), zPosition);
            WorldBorder border = world.getWorldBorder();
            border.setCenter(startPosition);
            border.setSize(borderSize);

            savedConfig = saveFile.createConfig();
            savedConfig.set("activeGame", true);
            savedConfig.set("playersAlive", playersAlive);

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

            if (gracePeriod > 0) {
                gracePeriod(1.0);
            } else {
                gameLoop();
            }

           activeGame = true;
        };

        new BukkitRunnable() {
            private int countdown = 3;
            private String text = ChatColor.RED + "➂";

            @Override
            public void run() {
                if (countdown > 0) {
                    if (countdown == 2) {
                        text = ChatColor.YELLOW + "➁";
                    } else if (countdown == 1) {
                        text = ChatColor.GREEN + "➀";
                    }
                    for (Player player : playersAlive) {
                        player.sendTitle(text, "", 1, 20, 1);
                    }
                    countdown -= 1;
                } else {
                    initializeGame.run();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    };

    public void gracePeriod(double progress) {
        bossBar = Bukkit.createBossBar(
                ChatColor.WHITE + "Grace Period",
                BarColor.GREEN,
                BarStyle.SOLID);

        for (Player player : playersAlive) {
            player.sendMessage(plugin.PLUGIN_NAME + "Grace Period has started");
            bossBar.addPlayer(player);
            bossBar.setVisible(true);
            bossBar.setProgress(1.0);
        }

        new BukkitRunnable() {
            private double currentProgress = progress;

            @Override
            public void run() {
                if (currentProgress < 0) {
                    bossBar.setVisible(false);
                    gameLoop();
                    this.cancel();
                }

                try {
                    bossBar.setProgress(currentProgress);
                } catch(Exception e) {
                    bossBar.setProgress(0.0);
                }

                currentProgress = currentProgress - ((double) 1 / gracePeriod);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void gameLoop() {
        bossBar = Bukkit.createBossBar(
                ChatColor.WHITE + "Rising Lava",
                BarColor.RED,
                BarStyle.SOLID);

        for (Player player : playersAlive) {
            player.sendMessage(plugin.PLUGIN_NAME + "The lava has started to rise");
            bossBar.addPlayer(player);
            bossBar.setVisible(true);
            bossBar.setProgress(1.0);
        }

        new BukkitRunnable() {
            private double currentProgress = 1.0;
            private Integer yLevel = 0;

            @Override
            public void run() {
                if (!(yLevel >= 3)) {
                    if (currentProgress < 0) {
                        bossBar.setProgress(1.0);
                        currentProgress = 1.0;
                        yLevel++;
                    }

                    try {
                        bossBar.setProgress(currentProgress);
                    } catch(Exception e) {
                        bossBar.setProgress(0.0);
                    }

                    currentProgress = currentProgress - ((double) 1 / heightDelay);
                } else {
                    bossBar.setTitle("Height Limit Reached");
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
