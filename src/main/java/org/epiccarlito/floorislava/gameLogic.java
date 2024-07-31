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

    public boolean activeGame = false;
    public ArrayList<Player> playersAlive;
    public String risingBlock;
    public boolean forceTeleport;
    public boolean forceClear;

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
            player.sendMessage("A game is currently in session");
            return;
        }

        if (!(risingBlock.contains("LAVA") || risingBlock.contains("WATER") || risingBlock.contains("VOID"))) {
            player.sendMessage("Invalid block in configuration. Select the following: ");
            return;
        }

        World world = Bukkit.getServer().getWorlds().get(0);
        Location startPosition = new Location(world, xPosition, world.getHighestBlockYAt(xPosition, zPosition), zPosition);
        WorldBorder border = world.getWorldBorder();
        border.setCenter(startPosition);
        border.setSize(borderSize);

        playersAlive = new ArrayList<>(Bukkit.getOnlinePlayers());

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

        startCountdown();
    }

    public void startCountdown() {
            Runnable gracePeriodCheck = () -> {
                if (gracePeriod > 0) {
                    gracePeriod(1.0);
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
                        gracePeriodCheck.run();
                        this.cancel();
                    }

                }
            }.runTaskTimer(plugin, 0L, 20L);
    }

    public void gracePeriod(double progress) {
        bossBar = Bukkit.createBossBar(
                ChatColor.GREEN + "Grace Period",
                BarColor.PURPLE,
                BarStyle.SOLID);

        for (Player player : playersAlive) {
            player.sendMessage("Grace Period has started");
            bossBar.addPlayer(player);
            bossBar.setVisible(true);
        }
    }
}
