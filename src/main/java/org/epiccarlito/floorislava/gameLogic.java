package org.epiccarlito.floorislava;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
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
    public int playersNeeded = 2;
    public ArrayList<Player> playersAlive;
    public Location startPosition;
    public String risingBlock;
    public boolean forceTeleport;
    public boolean forceClear;
    public boolean clearActionBar;

    public int startingHeight;
    public int heightIncrease;
    public int heightDelay;
    public int gracePeriod;
    public int borderSize;
    public int xPosition;
    public int zPosition;

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
            clearActionBar = config.getBoolean("clearActionBar");

            startingHeight = config.getInt("startingHeight");
            heightIncrease = config.getInt("heightIncrease");
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
        if (playersAlive.size() == 1) {
            playersNeeded = 1;
        }
        world = Bukkit.getServer().getWorlds().get(0);

        Runnable initializeGame = () -> {
            startPosition = new Location(world, xPosition + 0.5, world.getHighestBlockYAt(xPosition, zPosition), zPosition + 0.5);
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
                gameLoop(1.0);
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
            private int secondsPassed = 0;

            @Override
            public void run() {
                if (!activeGame) {
                    bossBar.setVisible(false);
                    this.cancel();
                }

                if (currentProgress < 0) {
                    bossBar.setVisible(false);
                    gameLoop(1.0);
                    this.cancel();
                }

                try {
                    if (secondsPassed > 0 && secondsPassed % (gracePeriod) == 0) {
                        currentProgress = 0.0;
                    }

                    bossBar.setProgress(currentProgress);
                } catch(Exception e) {
                    bossBar.setProgress(0.0);
                }

                currentProgress = currentProgress - (progress / gracePeriod);
                secondsPassed++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void endGame(Player player) {
        if (activeGame) {
            for (Player alivePlayer : playersAlive) {
                alivePlayer.setGameMode(GameMode.SURVIVAL);
                player.sendMessage(plugin.PLUGIN_NAME + "Game has ended!");
            }

            TextComponent component = new TextComponent();
            component.setText(plugin.PLUGIN_NAME + ChatColor.AQUA + "Click to play again!");
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/floorislava start"));
            player.spigot().sendMessage(component);

            world.getWorldBorder().setCenter(new Location(world,0,0,0));
            world.getWorldBorder().setSize(30000000);

            activeGame = false;
        } else {
            player.sendMessage(plugin.PLUGIN_NAME + "A game is not in session");
        }
    }

    public void gameLoop(double progress) {
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
            private double currentProgress = progress;
            private int yLevel = startingHeight;
            private int secondsPassed = 0;
            final Location topLeft = new Location(world, startPosition.getX() - ((double) borderSize / 2), yLevel + heightIncrease, startPosition.getZ() - ((double) borderSize / 2));
            final Location bottomRight = new Location(world, startPosition.getX() + ((double) borderSize / 2), yLevel, startPosition.getZ() + ((double) borderSize / 2));

            @Override
            public void run() {
                if (!activeGame) {
                    bossBar.setVisible(false);
                    this.cancel();
                }

                if (!(yLevel >= world.getMaxHeight())) {
                    if (currentProgress < 0) {

                        for (int x = topLeft.getBlockX(); x <= bottomRight.getBlockX(); x++) {
                            for (int y = bottomRight.getBlockY(); y <= topLeft.getBlockY(); y++) {
                                for (int z = topLeft.getBlockZ(); z <= bottomRight.getBlockZ(); z++) {
                                    Block block = world.getBlockAt(x, y, z);
                                    if (block.getType() == Material.DIRT) {
                                        block.setType(Material.WATER);
                                    }
                                }
                            }
                        }

                        bossBar.setProgress(1.0);
                        currentProgress = 1.0;
                        yLevel += heightIncrease;

                        topLeft.setY(yLevel + heightIncrease);
                        bottomRight.setY(yLevel);
                    }

                    try {
                        if (currentProgress != 1.0) {
                            secondsPassed++;
                            if (secondsPassed > 0 && secondsPassed % (heightDelay) == 0) {
                                currentProgress = 0.0;
                            }
                        }

                        bossBar.setProgress(currentProgress);
                    } catch(Exception e) {
                        bossBar.setProgress(0.0);
                    }

                    currentProgress = currentProgress - (progress / heightDelay);

                    if (!clearActionBar) {
                        for (Player player : playersAlive) {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Y-Level: " + ChatColor.BOLD + yLevel));
                        }
                    }
                } else {
                    bossBar.setTitle("Height Limit Reached");
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
