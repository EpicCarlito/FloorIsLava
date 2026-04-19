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
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class gameLogic {
    private final FloorIsLava plugin;
    private final saveFile saveFile;
    private final FileConfiguration config;
    public FileConfiguration savedConfig;
    public boolean activeGame = false;
    public boolean ifSaveFile = false;
    public boolean isFilling = false;

    public World world;
    public BossBar bossBar;
    public String risingBlock;
    public ArrayList<Player> playersAlive = new ArrayList<>();
    public List<String> playerUUIDs;
    public Location startPosition;
    private int xPosition;
    private int zPosition;
    public int graceProgress = 0;
    public int gracePeriod;
    public int startingHeight;
    public int yLevel;
    public int heightIncrease;
    public int heightDelay;
    public int borderSize;
    public int finalBorderSize;
    public int playersNeeded = 2;

    public boolean clearActionBar;
    private boolean forceTeleport;
    private boolean forceClear;

    public long yIntervals;
    private static final int SERVER_VERSION = Integer.parseInt(
            Bukkit.getBukkitVersion().split("-")[0].split("\\.")[0]
    );

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
        finalBorderSize = ifSaveFile ? savedConfig.getInt("finalBorderSize") : config.getInt("finalBorderSize");
        xPosition = ifSaveFile ? savedConfig.getInt("startPosition.x") : config.getInt("startPosition.x");
        zPosition = ifSaveFile ? savedConfig.getInt("startPosition.z") : config.getInt("startPosition.z");

        if (ifSaveFile) {
            world = Bukkit.getWorld(Objects.requireNonNull(savedConfig.getString("world")));
            graceProgress = savedConfig.getInt("graceProgress");
            playerUUIDs = savedConfig.getStringList("playersAlive");
            playersNeeded = savedConfig.getInt("playersNeeded");
        }
    }

    public void startGame(Player player) {
        if (activeGame) {
            player.sendMessage(plugin.PLUGIN_NAME + "There is a match in session!");
            return;
        }

        if (ifSaveFile) {
            player.sendMessage(plugin.PLUGIN_NAME + "Your match file has not been loaded! (/fl load)");
            return;
        }

        if (!(risingBlock.contains("LAVA") || risingBlock.contains("WATER"))) {
            player.sendMessage(plugin.PLUGIN_NAME + "Invalid block in configuration!");
            return;
        }

        if (startingHeight < -64 || startingHeight > player.getWorld().getMaxHeight()) {
            player.sendMessage(plugin.PLUGIN_NAME + "Invalid starting height in configuration!");
            return;
        }

        if (heightDelay < 0) {
            player.sendMessage(plugin.PLUGIN_NAME + "Invalid height delay in configuration!");
            return;
        }

        if (heightIncrease <= 0 || heightIncrease > 10) {
            player.sendMessage(plugin.PLUGIN_NAME + "Invalid heightIncrease in configuration!");
            return;
        }

        if (gracePeriod < 0 || gracePeriod > 1800) {
            player.sendMessage(plugin.PLUGIN_NAME + "Invalid grace period in configuration!");
            return;
        }

        if (borderSize < 0 || borderSize > 500) {
            player.sendMessage(plugin.PLUGIN_NAME + "Invalid border size in configuration!");
            return;
        }

        if (finalBorderSize > borderSize || finalBorderSize < 0) {
            player.sendMessage(plugin.PLUGIN_NAME + "Invalid final border size in configuration!");
            return;
        }

        activeGame = true;
        world = player.getWorld();
        startPosition = new Location(world, xPosition + 0.5, world.getHighestBlockYAt(xPosition, zPosition) + 1, zPosition + 0.5);

        playersAlive = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (playersAlive.size() == 1) {
            playersNeeded = 1;
        }

        Runnable initializeGame = () -> {
            WorldBorder border = world.getWorldBorder();
            world.setSpawnLocation(startPosition);
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

            yIntervals = (long) ((double)(world.getMaxHeight() - startingHeight) / heightIncrease);

            if (gracePeriod > 0) {
                world.setTime(1000);
                gracePeriod();
            } else {
                world.setTime(1000);
                if (SERVER_VERSION >= 26) {
                    world.getWorldBorder().setSize(finalBorderSize, TimeUnit.MILLISECONDS, (yIntervals * heightDelay) * 20);
                } else {
                    world.getWorldBorder().setSize(finalBorderSize, TimeUnit.SECONDS, (yIntervals * heightDelay));
                }

                gameLoop();
            }
        };

        new BukkitRunnable() {
            private int countdown = 3;
            private String text = ChatColor.RED + "3";

            @Override
            public void run() {
                if (countdown > 0) {
                    if (countdown == 2) {
                        text = ChatColor.YELLOW + "2";
                    } else if (countdown == 1) {
                        text = ChatColor.GREEN + "1";
                    }
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle(text, "", 1, 20, 1);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
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
            player.sendMessage(plugin.PLUGIN_NAME + "Match file not found!");
            return;
        }

        if (!savedConfig.getBoolean("activeGame")) {
            player.sendMessage(plugin.PLUGIN_NAME + "Match file contains a complete game (/fl end)!");
            return;
        }

        activeGame = true;
        startPosition = new Location(world, xPosition + 0.5, world.getHighestBlockYAt(xPosition, zPosition), zPosition + 0.5);

        for (String uuidString : playerUUIDs) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                Player foundPlayer = Bukkit.getPlayer(uuid);

                if (foundPlayer != null) {
                    playersAlive.add(foundPlayer);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID format: " + uuidString);
            }
        }

        yIntervals = (long) ((double)(world.getMaxHeight() - startingHeight) / heightIncrease);

        Runnable initializeGame = () -> {
            if (gracePeriod > 0 && graceProgress > 0) {
                world.setTime(1000);
                gracePeriod();
            } else {
                world.setTime(1000);
                if (SERVER_VERSION >= 26) {
                    world.getWorldBorder().setSize(finalBorderSize, TimeUnit.MILLISECONDS, (yIntervals * heightDelay) * 20);
                } else {
                    world.getWorldBorder().setSize(finalBorderSize, TimeUnit.SECONDS, (yIntervals * heightDelay));
                }

                gameLoop();
            }
        };

        new BukkitRunnable() {
            private int countdown = 3;
            private String text = ChatColor.RED + "3";

            @Override
            public void run() {
                if (countdown > 0) {
                    if (countdown == 2) {
                        text = ChatColor.YELLOW + "2";
                    } else if (countdown == 1) {
                        text = ChatColor.GREEN + "1";
                    }
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle(text, "", 1, 20, 1);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.5f);
                    }
                    countdown -= 1;
                } else {
                    initializeGame.run();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void gracePeriod() {
        bossBar = Bukkit.createBossBar(
                ChatColor.WHITE + "Grace Period",
                BarColor.GREEN,
                BarStyle.SOLID);

        double totalTicks = gracePeriod * 20;
        double progress = (totalTicks - graceProgress) / totalTicks;
        String minutes = (gracePeriod / 60) > 0 ? (gracePeriod / 60) + "m" : "";
        String seconds = (gracePeriod % 60) > 0 ? (minutes.isEmpty() ? "" : " and ") + (gracePeriod % 60) + "s" : "";

        Bukkit.broadcastMessage(plugin.PLUGIN_NAME + "Grace Period is " + minutes + seconds);

        for (Player player: Bukkit.getOnlinePlayers()) {
            player.sendTitle(ChatColor.GREEN + "GRACE PERIOD", "Respawns Enabled", 10, 70, 20);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.5f);
            bossBar.addPlayer(player);
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            bossBar.setVisible(true);
        }

        new BukkitRunnable() {
            private int ticksPassed = graceProgress == 0 ? 1 : graceProgress;

            @Override
            public void run() {
                if (!activeGame) {
                    bossBar.setVisible(false);
                    this.cancel();
                    return;
                }

                ticksPassed++;
                graceProgress++;

                if (ticksPassed > totalTicks) {
                    bossBar.setVisible(false);
                    if (SERVER_VERSION >= 26) {
                        world.getWorldBorder().setSize(finalBorderSize, TimeUnit.MILLISECONDS, (yIntervals * heightDelay) * 20);
                    } else {
                        world.getWorldBorder().setSize(finalBorderSize, TimeUnit.SECONDS, (yIntervals * heightDelay));
                    }

                    gameLoop();
                    this.cancel();
                    return;
                };

                double progress = (totalTicks - ticksPassed) / totalTicks;
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    public void gameLoop() {
        yLevel = startingHeight;
        isFilling = false;
        bossBar = Bukkit.createBossBar(
                ChatColor.WHITE + "Rising Lava",
                BarColor.RED,
                BarStyle.SOLID);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(ChatColor.GOLD + "RISING LAVA", ChatColor.RED + "Death is Permanent", 10, heightDelay * 20, 20);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
            bossBar.addPlayer(player);
            bossBar.setVisible(true);
            bossBar.setProgress(1.0);
        }

        int totalTicks = heightDelay * 20;

        new BukkitRunnable() {
            int ticksPassed = 0;

            @Override
            public void run() {
                boolean doFill = ticksPassed >= totalTicks;
                double progress = (yLevel >= world.getMaxHeight()) ? 0.0 : Math.max(0.0, Math.min(1.0, 1.0 - ((double) ticksPassed / totalTicks)));

                if (doFill) {
                    ticksPassed = 0;
                } else {
                    ticksPassed++;
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!activeGame) {
                            bossBar.setVisible(false);
                            return;
                        }

                        bossBar.setProgress(progress);

                        if (!clearActionBar) {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                TextComponent actionBar = new TextComponent("Y-Level: " + ChatColor.BOLD + yLevel);
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, actionBar);
                            }
                        }

                        announceWinner();

                        if (yLevel >= world.getMaxHeight()) {
                            bossBar.setTitle("Max Height");
                            bossBar.setProgress(1.0);
                        }

                        if (doFill && !isFilling) fillBlocks();
                    }
                }.runTask(plugin);

                if (!activeGame) this.cancel();
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    public void fillBlocks() {
        isFilling = true;
        int currBorder = (int) world.getWorldBorder().getSize();
        int halfBorder = currBorder / 2;
        int fillY = yLevel;
        int startX = (int)(startPosition.getX() - halfBorder);
        int endX   = (int)(startPosition.getX() + halfBorder);
        int startZ = (int)(startPosition.getZ() - halfBorder);
        int endZ   = (int)(startPosition.getZ() + halfBorder);

        new BukkitRunnable() {
            int x = startX;

            @Override
            public void run() {
                int totalBlocks = (endX - startX + 1) * (endZ - startZ + 1) * (heightIncrease + 1);
                int ticksNeeded = heightDelay * 20;
                int sectionSize = totalBlocks / ticksNeeded;
                int blocksPlaced = 0;

                while (x <= endX && blocksPlaced < sectionSize) {
                    for (int z = startZ; z <= endZ; z++) {
                        for (int y = fillY; y <= Math.min(fillY + heightIncrease, world.getMaxHeight()); y++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (block.getType() == Material.AIR) {
                                block.setType(Material.getMaterial(risingBlock));
                                blocksPlaced++;
                            }
                        }
                    }
                    x++;
                }
                if (x > endX) {
                    yLevel += heightIncrease;
                    if (yLevel >= world.getMaxHeight()) {
                        yLevel = world.getMaxHeight();
                    }
                    isFilling = false;
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void announceWinner() {
        if (!activeGame) return;

        boolean singleWin = playersNeeded == 1 && yLevel == world.getMaxHeight();
        boolean multiWin = playersNeeded == 2 && playersAlive.size() == 1;

        if (!singleWin && !multiWin) return;

        String lastPlayer = playersAlive.get(0).getName();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(ChatColor.GREEN + lastPlayer + " wins!", "", 10, 70, 20);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        launchFireworks();
        endGame();
    }

    public void launchFireworks() {
        Player winner = playersAlive.get(0);

        new BukkitRunnable() {
            Location location = winner.getLocation();
            int shots = 0;

            @Override
            public void run() {
                if (shots >= 16) {
                    this.cancel();
                    return;
                }

                Firework fw = location.getWorld().spawn(location, Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();

                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.RED, Color.WHITE, Color.BLUE)
                        .withTrail()
                        .build());

                meta.setPower(1);
                fw.setFireworkMeta(meta);
                fw.setMetadata("winnerFirework", new FixedMetadataValue(plugin, true));

                shots++;
                location = winner.getLocation();
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public void endGame() {
        if (!activeGame) {
            plugin.getServer().broadcastMessage(plugin.PLUGIN_NAME + "There is no current match to end!");
            return;
        }

        graceProgress = 0;

        world.setSpawnLocation(0, world.getHighestBlockYAt(0, 0), 0);
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(30000000);

        saveFile.deleteFile();
        ifSaveFile = false;
        isFilling = false;

        Player lastPlayer = playersAlive.isEmpty() ? null : playersAlive.get(0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SPECTATOR);

            TextComponent playAgain = new TextComponent();
            playAgain.setText(plugin.PLUGIN_NAME + ChatColor.AQUA + "Click to play again!");
            playAgain.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/floorislava start"));
            player.spigot().sendMessage(playAgain);
        }

        if (lastPlayer != null) {
            lastPlayer.setGameMode(GameMode.SURVIVAL);
        }

        bossBar.setVisible(false);
        activeGame = false;
    }
}

