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

import java.util.*;

public class gameLogic {
    private final FloorIsLava plugin;
    private final saveFile saveFile;
    private final FileConfiguration config;
    public FileConfiguration savedConfig;
    public boolean activeGame = false;
    public boolean ifSaveFile = false;

    public World world;
    public BossBar bossBar;
    public String risingBlock;
    public ArrayList<Player> playersAlive = new ArrayList<>();
    public List<String> playerUUIDs;
    public Location startPosition;
    private int xPosition;
    private int zPosition;
    public double graceProgress = 1.0;
    public int gracePeriod;
    public int startingHeight;
    public int yLevel;
    public int heightIncrease;
    public int heightDelay;
    public int borderSize;
    public int playersNeeded = 2;

    public boolean clearActionBar;
    private boolean forceTeleport;
    private boolean forceClear;

    public gameLogic(FloorIsLava plugin) {
        this.plugin = plugin;
        saveFile = plugin.saveFile;
        savedConfig = plugin.savedConfig;
        ifSaveFile = savedConfig != null;
        config = plugin.getConfig();

        try {
            loadConfig();
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading configuration: " + e.getMessage());
        }
    }

    private void loadConfig() {
        risingBlock = ifSaveFile ? savedConfig.getString("risingBlock") : config.getString("risingBlock");
        forceTeleport = config.getBoolean("forceTeleport");
        forceClear = config.getBoolean("forceClear");
        clearActionBar = ifSaveFile ? savedConfig.getBoolean("clearActionBar") : config.getBoolean("clearActionBar");
        startingHeight = ifSaveFile ? savedConfig.getInt("startingHeight") : config.getInt("startingHeight");
        heightIncrease = ifSaveFile ? savedConfig.getInt("heightIncrease") : config.getInt("heightIncrease");
        heightDelay = ifSaveFile ? savedConfig.getInt("heightDelay") : config.getInt("heightDelay");
        gracePeriod = ifSaveFile ? savedConfig.getInt("gracePeriod") : config.getInt("gracePeriod");
        borderSize = ifSaveFile ? savedConfig.getInt("borderSize") : config.getInt("borderSize");
        xPosition = ifSaveFile ? savedConfig.getInt("startPosition.x") : config.getInt("startPosition.x");
        zPosition = ifSaveFile ? savedConfig.getInt("startPosition.z") : config.getInt("startPosition.z");

        if (ifSaveFile) {
            world = Bukkit.getWorld(Objects.requireNonNull(savedConfig.getString("world")));
            graceProgress = savedConfig.getDouble("graceProgress");
            playerUUIDs = savedConfig.getStringList("playersAlive");
            playersNeeded = savedConfig.getInt("playersNeeded");
        }
    }


    public void startGame(Player player) {
        if (activeGame) {
            player.sendMessage(plugin.PLUGIN_NAME + "A game is currently in session");
            return;
        }

        if (ifSaveFile) {
            player.sendMessage(plugin.PLUGIN_NAME + "Your save file has not been loaded");
            return;
        }

        if (!(risingBlock.contains("LAVA") || risingBlock.contains("WATER"))) {
            player.sendMessage(plugin.PLUGIN_NAME + "Invalid block in configuration.");
            return;
        }

        if (startingHeight < -64) {
            player.sendMessage(plugin.PLUGIN_NAME + "Invalid starting height in configuration.");
            return;
        }

        activeGame = true;

        playersAlive = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (playersAlive.size() == 1) {
            playersNeeded = 1;
        }
        world = player.getWorld();
        startingHeight = config.getInt("startingHeight");

        Runnable initializeGame = () -> {
            startPosition = new Location(world, xPosition + 0.5, world.getHighestBlockYAt(xPosition, zPosition) + 0.5, zPosition + 0.5);
            WorldBorder border = world.getWorldBorder();
            border.setCenter(startPosition);
            border.setSize(borderSize);

            for (Player alivePlayer : playersAlive) {
                if (forceTeleport) {
                    alivePlayer.teleport(startPosition);
                }

                if (forceClear) {
                    alivePlayer.getInventory().clear();
                }

                alivePlayer.setGameMode(GameMode.SURVIVAL);
                alivePlayer.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue());
                alivePlayer.setFoodLevel(20);
            }

            if (gracePeriod > 0) {
                graceProgress = 1.0;
                gracePeriod(graceProgress);
            } else {
                gameLoop();
            }
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
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle(text, "", 1, 20, 1);
                    }
                    countdown -= 1;
                } else {
                    initializeGame.run();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void loadGame(Player player) {
        if (!ifSaveFile) {
            player.sendMessage(plugin.PLUGIN_NAME + "Save file not found");
            return;
        }

        if (!savedConfig.getBoolean("activeGame")) {
            player.sendMessage(plugin.PLUGIN_NAME + "Save data contains a finished game, please run /fl end");
            return;
        }

        activeGame = true;

        for (String uuidString : playerUUIDs) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                Player foundPlayer = Bukkit.getPlayer(uuid);

                if (foundPlayer != null) {
                    playersAlive.add(player);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID format: " + uuidString);
            }
        }

        Runnable initializeGame = () -> {
            startPosition = new Location(world, xPosition + 0.5, world.getHighestBlockYAt(xPosition, zPosition), zPosition + 0.5);

            if (gracePeriod > 0 && graceProgress > 0) {
                gracePeriod(graceProgress);
            } else {
                gameLoop();
            }
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
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle(text, "", 1, 20, 1);
                    }
                    countdown -= 1;
                } else {
                    initializeGame.run();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void gracePeriod(double progress) {
        bossBar = Bukkit.createBossBar(
                ChatColor.WHITE + "Grace Period",
                BarColor.GREEN,
                BarStyle.SOLID);

        for (Player player: Bukkit.getOnlinePlayers()) {
            player.sendMessage(plugin.PLUGIN_NAME + "Grace Period has started");
            bossBar.addPlayer(player);
            bossBar.setVisible(true);
            bossBar.setProgress(graceProgress);
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
                    gameLoop();
                    this.cancel();
                }

                try {
                    if (secondsPassed > 0 && secondsPassed % (gracePeriod) == 0) {
                        currentProgress = 0.0;
                        graceProgress = currentProgress;
                    }

                    bossBar.setProgress(currentProgress);
                } catch (Exception e) {
                    bossBar.setProgress(0.0);
                }

                currentProgress = currentProgress - (progress / gracePeriod);
                graceProgress = currentProgress;
                secondsPassed++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void gameLoop() {
        yLevel = startingHeight;

        bossBar = Bukkit.createBossBar(
                ChatColor.WHITE + "Rising Lava",
                BarColor.RED,
                BarStyle.SOLID);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(plugin.PLUGIN_NAME + "The lava has started to rise");
            bossBar.addPlayer(player);
            bossBar.setVisible(true);
            bossBar.setProgress(1.0);
        }

        new BukkitRunnable() {
            private double currentProgress = 1.0;
            private int secondsPassed = 0;

            @Override
            public void run() {
                if (!activeGame) {
                    bossBar.setVisible(false);
                    this.cancel();
                }

                borderSize = (int) world.getWorldBorder().getSize();

                Location topLeft = new Location(world, startPosition.getX() - ((double) borderSize / 2), yLevel + heightIncrease, startPosition.getZ() - ((double) borderSize / 2));
                Location bottomRight = new Location(world, startPosition.getX() + ((double) borderSize / 2), yLevel, startPosition.getZ() + ((double) borderSize / 2));
                if (!(yLevel >= world.getMaxHeight())) {
                    if (currentProgress < 0) {

                        int sectionSize = 750; // Number of blocks per tick
                        List<Block> blocksToPlace = new ArrayList<>();

                        for (int x = topLeft.getBlockX(); x <= bottomRight.getBlockX(); x++) {
                            for (int y = bottomRight.getBlockY(); y <= topLeft.getBlockY(); y++) {
                                for (int z = topLeft.getBlockZ(); z <= bottomRight.getBlockZ(); z++) {
                                    Block block = world.getBlockAt(x, y, z);
                                    if (block.getType() == Material.AIR) {
                                        blocksToPlace.add(block);
                                    }
                                }
                            }
                        }

                        int startX = topLeft.getBlockX();
                        int startZ = topLeft.getBlockZ();

                        blocksToPlace.sort(Comparator.comparingDouble(b ->
                                b.getX() * b.getX() - 2 * startX * b.getX() + startX * startX +
                                        b.getZ() * b.getZ() - 2 * startZ * b.getZ() + startZ * startZ
                        ));

                        new BukkitRunnable() {
                            int index = 0;

                            @Override
                            public void run() {
                                int end = Math.min(index + sectionSize, blocksToPlace.size());

                                for (; index < end; index++) {
                                    Block block = blocksToPlace.get(index);
                                    block.setType(Material.getMaterial(risingBlock));
                                }

                                if (index >= blocksToPlace.size()) {
                                    cancel(); // Done
                                }
                            }
                        }.runTaskTimer(plugin, 0L, 2L);

                        bossBar.setProgress(1.0);
                        currentProgress = 1.0;
                        yLevel += heightIncrease;
                        startingHeight = yLevel;

                        topLeft.setY(yLevel + heightIncrease);
                        bottomRight.setY(yLevel);

                        if (world.getMaxHeight() <= yLevel) {
                            yLevel = world.getMaxHeight();
                            startingHeight = yLevel;
                        }
                    }

                    try {
                        if (currentProgress != 1.0) {
                            secondsPassed++;
                            if (secondsPassed > 0 && secondsPassed % (heightDelay) == 0) {
                                currentProgress = 0.0;
                            }
                        }

                        bossBar.setProgress(currentProgress);
                    } catch (Exception e) {
                        bossBar.setProgress(0.0);
                    }

                    currentProgress = currentProgress - (1.0 / heightDelay);

                    if (!clearActionBar) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            TextComponent actionBar = new TextComponent("Y-Level: " + ChatColor.BOLD + yLevel);
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, actionBar);
                        }
                    }
                } else {
                    bossBar.setTitle("Height Limit Reached");
                }

                announceWinner(yLevel);
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L);
    }

    public void announceWinner(int yLevel) {
        if (!activeGame) return;

        if (playersNeeded == 1 && yLevel == world.getMaxHeight()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String lastPlayer = playersAlive.get(0).getName();
                player.sendTitle(ChatColor.GREEN + lastPlayer + " wins!", "", 10, 70, 20);
                endGame(null);
            }
        } else if (playersNeeded == 2 && playersAlive.size() == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String lastPlayer = playersAlive.get(0).getName();
                player.sendTitle(ChatColor.GREEN + lastPlayer + " wins!", "", 10, 70, 20);
                endGame(null);
            }
        }
    }

    public void endGame(Player player) {
        if (!activeGame) {
            plugin.getServer().broadcastMessage(plugin.PLUGIN_NAME + "A game is not in session or not loaded");
            return;
        }

        world.getWorldBorder().setCenter(new Location(world, 0, 0, 0));
        world.getWorldBorder().setSize(30000000);

        if (savedConfig != null) {
            saveFile.deleteFile();
            ifSaveFile = false;
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.setGameMode(GameMode.SURVIVAL);
            plugin.getServer().broadcastMessage(plugin.PLUGIN_NAME + "This game has ended!");
        }

        if (player != null) {
            TextComponent playAgain = new TextComponent();
            playAgain.setText(plugin.PLUGIN_NAME + ChatColor.AQUA + "Click to play again!");
            playAgain.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/floorislava start"));
            player.spigot().sendMessage(playAgain);
        }

        activeGame = false;
    }
}
