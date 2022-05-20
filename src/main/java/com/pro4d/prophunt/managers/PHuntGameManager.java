package com.pro4d.prophunt.managers;

import com.pro4d.prophunt.Prophunt;
import com.pro4d.prophunt.misc.TeamInventory;
import com.pro4d.prophunt.enums.Teams;
import com.pro4d.prophunt.enums.WinningCondition;
import com.pro4d.prophunt.misc.GameStates;
import com.pro4d.prophunt.misc.ItemBuilder;
import com.pro4d.prophunt.misc.PHuntMap;
import com.pro4d.prophunt.utils.PHuntMessages;
import com.pro4d.prophunt.utils.PHuntUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;

public class PHuntGameManager {

    private GameStates state;
    private WinningCondition winningCondition = WinningCondition.TIED;
    private final List<UUID> allPlayers;
    private final Map<UUID, Integer> killCounter;

    private boolean huntersFrozen = false;
    private int roundCount = 0;

    private final ItemBuilder swapper;
    private final ItemBuilder taunter;
    private final ItemBuilder spectatorCompass;
    private final ItemBuilder hunterWeapon;

    private final List<ItemBuilder> allItems;
    private final DecimalFormat df;

    private final Prophunt plugin;
    private final PropManager propManager;
    private final PHuntSettingsManager settingsManager;
    private final MapManager mapManager;
    private final SpectatorManager spectatorManager;
    private final BossBar bar;

    private Location tempSpecSpawn;

    public PHuntGameManager(Prophunt plugin) {
        this.plugin = plugin;
        bar = new BarManager().getBar();

        df = new DecimalFormat("#.##");

        state = GameStates.LOBBY;
        allItems = new ArrayList<>();
        allPlayers = new ArrayList<>();

        killCounter = new HashMap<>();

        settingsManager = new PHuntSettingsManager(plugin, this);
        propManager = new PropManager(settingsManager);

        mapManager = new MapManager(plugin);

        settingsManager.validateSettingsConfig();
        settingsManager.validateMapConfig();
        settingsManager.validateSwappablesConfig();

        mapManager.createMapMessages();

        allItems.add(swapper = new ItemBuilder(settingsManager.getPropSwapper(), 0));
        allItems.add(taunter = new ItemBuilder(settingsManager.getPropTaunter(), 8));
        allItems.add(hunterWeapon = new ItemBuilder(settingsManager.getHunterWeaponItem(), 0));

        ItemStack compass = new ItemStack(Material.COMPASS, 1);
        ItemMeta compassMeta = compass.getItemMeta();

        if(compassMeta != null) {
            compassMeta.setDisplayName(PHuntMessages.translate("&7Spectator Menu"));
            compass.setItemMeta(compassMeta);
        }

        allItems.add(spectatorCompass = new ItemBuilder(compass, 4));

        spectatorManager = new SpectatorManager(plugin);

    }

    public void setGameState(GameStates gameState) {
        if(gameState == GameStates.ACTIVE) {
            if (mapManager.getAllMaps().isEmpty()) {
                plugin.getServer().getOnlinePlayers().forEach(p -> {
                    p.sendMessage(PHuntMessages.translate("&cError starting game, check console"));
                    plugin.getUtils().log(Level.WARNING, PHuntMessages.translate("&cNo maps found, setup maps in config."));
                    cleanup();
                });
                return;
            }

            if (mapManager.getLobbySpawnpoint() == null) {
                plugin.getServer().getOnlinePlayers().forEach(p -> {
                    p.sendMessage(PHuntMessages.translate("&cError starting game, check console"));
                    plugin.getUtils().log(Level.WARNING, PHuntMessages.translate("&cLobby spawnpoint from config is invalid!"));
                    cleanup();
                });
                return;
            }

            if(plugin.getServer().getOnlinePlayers().stream().filter(p -> p.getGameMode() != GameMode.SPECTATOR).count() < settingsManager.getMinimumPlayerCount()) {
                plugin.getServer().getOnlinePlayers().forEach(p -> {
                    p.sendMessage(PHuntMessages.translate("&cError starting game, check console"));
                    cleanup();
                });
                return;
            }

        }


        state = gameState;

        switch (gameState) {
            case LOBBY:
                //send team select chat messages
                allPlayers.forEach(uuid -> {
                    LivingEntity entity = (LivingEntity) Bukkit.getEntity(uuid);
                    if(entity != null) {
                        if(Teams.getPlayerTeam(entity) != null) {
                            if(Teams.getPlayerTeam(entity) == Teams.HIDERS) {
                                propManager.clearDisguise(entity);
                            }
                        }
                        entity.getActivePotionEffects().forEach(pot -> {
                            if(pot.getType() == PotionEffectType.BLINDNESS) {
                                entity.removePotionEffect(pot.getType());
                            }
                        });
                        if(entity instanceof Player) {
                            Player player = (Player) entity;
                            allItems.forEach(ib -> ItemBuilder.removeItem(player, ib.getItem()));

                        }

                        //clear team

                    }
                });

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getServer().getOnlinePlayers().forEach(player -> plugin.getMessages().getTeamMessages().forEach(teamMessages -> player.spigot().sendMessage(teamMessages)));
                    }
                }.runTaskLater(plugin, 20L);

                huntersFrozen = false;
                break;


            case ACTIVE:
                //start game timer
                huntersFrozen = true;
                Teams.HUNTERS.getMembers().forEach(uuid -> {
                    LivingEntity e = (LivingEntity) plugin.getServer().getEntity(uuid);
                    if(e != null) {
                        e.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) ((settingsManager.getHunterFrozenTime() + 1) * 20L), 10, false, false, false));
                    }
                });
                startHunterCountdown();
                mapManager.clearVotes();
                break;

            case STARTING:
                allPlayers.forEach(uuid -> {
                    if(Bukkit.getPlayer(uuid) != null) {
                        Player player = Bukkit.getPlayer(uuid);
                        assert player != null;
                        if(mapManager.getAllMaps().keySet().size() != 1) {
                            mapManager.getMessages().forEach(comp -> player.spigot().sendMessage(comp));
                        }
                        player.setInvisible(false);
                    }
                });
                startCountdown();
                break;

            case ENDING:
                displayWinner();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        endGame();
                    }
                }.runTaskLater(plugin, 20 * 10L);
                break;

        }

    }

    public void addToTeam(Teams team, LivingEntity entity) {
        if(team.getMembers().contains(entity.getUniqueId())) {
            if(team == Teams.DEAD) return;
            entity.sendMessage(PHuntMessages.translate("&cAlready apart of this team"));
            return;
        }

        Teams.clearTeam(entity, this, false);
        team.addMember(entity);
        allPlayers.add(entity.getUniqueId());

        if(entity instanceof Player) {
            Player player = (Player) entity;
            player.setPlayerListName(PHuntMessages.translate(team.getColor() + "[" + team.getName() + "] " + player.getName()));

            if (team == Teams.DEAD) {
                player.setGameMode(GameMode.ADVENTURE);
                player.setInvisible(true);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.setCollidable(false);
                player.setInvulnerable(true);
                player.setArrowsInBody(0);

                player.setPlayerListName(PHuntMessages.translate("&o" + player.getPlayerListName()));

                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (p != player) {
                        p.hidePlayer(plugin, player);
                    }
                }
            }
        }

        for(TeamInventory tInv : spectatorManager.getInventories()) {
            if(tInv.getTeam() == team) {
                tInv.updateInventory();
            }
        }

        if(team == Teams.DEAD) return;
        entity.sendMessage(PHuntMessages.translate("&aJoined team&r: " + team.getColor() + team.getName()));
    }

    public void removeFromTeam(Teams team, LivingEntity entity, boolean message) {
        if(!team.getMembers().contains(entity.getUniqueId())) {
            if(team == Teams.DEAD) return;
            entity.sendMessage(PHuntMessages.translate("&cNot a member of this team"));
            return;
        }

        team.removeMember(entity);
        allPlayers.remove(entity.getUniqueId());

        if(entity instanceof Player) {
            Player player = (Player) entity;
            player.setPlayerListName(PHuntMessages.translate("&r" + player.getName()));
            if (team == Teams.DEAD) {

                player.setGameMode(GameMode.SURVIVAL);
                player.setInvisible(false);
                player.setAllowFlight(false);
                player.setFlying(false);
                player.setCollidable(true);

                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    if (p != player) {
                        p.showPlayer(plugin, player);
                    }
                }

                spectatorManager.getOldTeam().remove(player.getUniqueId());

            }
        }

        for(TeamInventory tInv : spectatorManager.getInventories()) {
            tInv.updateInventory();
        }

        if(team == Teams.DEAD) return;
        if(message) entity.sendMessage(PHuntMessages.translate("&aLeft team&r: " + team.getColor() + team.getName()));
    }

    private void startCountdown() {
        int voteTime = settingsManager.getVoteTime();
        bar.setTitle(PHuntMessages.translate("&lStarting in " + voteTime));
        bar.setVisible(true);

        plugin.getServer().getOnlinePlayers().forEach(bar::addPlayer);

        new BukkitRunnable() {
            final BossBar bossBar = bar;
            double progress = 1.0;
            final double increment = Double.parseDouble(df.format(progress / voteTime));
            final int time = voteTime;
            int c = 0;

            @Override
            public void run() {
                if(progress <= 0) {
                    bar.setVisible(false);
                    startGame();
                    cancel();
                } else {
                    bossBar.setProgress(progress);
                    progress = Double.parseDouble(df.format(progress - increment));
                    //Bukkit.broadcastMessage("P: " + progress);
                    c++;

                    if((time - c) > -1) {
                        bossBar.setTitle(PHuntMessages.translate("&lStarting in " + (time - c)));
                    }

                }

            }
        }.runTaskTimer(plugin, 0, 20);

    }

    private void startHunterCountdown() {
        int hunterReleaseTime = settingsManager.getHunterFrozenTime();
        bar.setTitle(PHuntMessages.translate("&l&cHunters released in " + hunterReleaseTime));
        bar.setVisible(true);

        plugin.getServer().getOnlinePlayers().forEach(bar::addPlayer);

        new BukkitRunnable() {
            final BossBar bossBar = bar;
            double progress = 1.0;
            final double increment = Double.parseDouble(df.format(progress / hunterReleaseTime));
            final int time = hunterReleaseTime;
            int c = 0;

            @Override
            public void run() {
                if(progress <= 0) {
                    bar.setVisible(false);
                    huntersFrozen = false;
                    startGameCountdown();
                    cancel();
                } else {
                    bossBar.setProgress(progress);
                    progress = Double.parseDouble(df.format(progress - increment));
                    //Bukkit.broadcastMessage("P: " + progress);
                    c++;
                    if((time - c) > -1) {
                        bossBar.setTitle(PHuntMessages.translate("&l&cHunters released in " + (time - c)));
                    }

                }

            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void startGameCountdown() {
        int gameTime = settingsManager.getRoundTime();
        bar.setTitle(PHuntMessages.translate("&lTime Left " + gameTime));
        bar.setVisible(true);

        plugin.getServer().getOnlinePlayers().forEach(bar::addPlayer);

        new BukkitRunnable() {
            final BossBar bossBar = bar;
            double progress = 1.0;
            final double increment = Double.parseDouble(df.format(progress / gameTime));
            final int time = gameTime;
            int c = 0;

            @Override
            public void run() {
                if(progress <= 0) {
                    setGameState(GameStates.ENDING);
                    bar.setVisible(false);
                    cancel();
                } else {
                    bossBar.setProgress(progress);
                    progress = Double.parseDouble(df.format(progress - increment));
                    c++;
                    if((time - c) > -1) {
                        bossBar.setTitle(PHuntMessages.translate("&lTime Left " + (time - c)));
                    }

                    allPlayers.forEach(uuid -> {
                        if(Bukkit.getPlayer(uuid) != null) {
                            Player player = Bukkit.getPlayer(uuid);
                            assert player != null;

                            if(propManager.isDisguised(player)) {
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, plugin.getMessages().getLockMessage());

                            } else if(propManager.isLocked(player)) {
                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, plugin.getMessages().getUnlockMessage());
                            }

                        }
                    });

                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void startGame() {
        PHuntMap map;
        if(mapManager.getAllMaps().keySet().size() == 1) {
            List<PHuntMap> mapList = new ArrayList<>(mapManager.getAllMaps().keySet());
            map = mapList.get(0);

        } else if(mapManager.getMapWithHighestVotes() == null) {
            map = mapManager.getMapWithHighestVotes();
        } else {
            map = mapManager.getRandomMap();
        }

        Vector min = map.getCornerOne();
        Vector max = map.getCornerTwo();
        World world = map.getWorld();

        Location loc = PHuntUtils.randomLocation(world, min, max);
        loc.getChunk().load(true);
        //block check
        while(!PHuntUtils.isLocationSafe(loc)) {
            loc = PHuntUtils.randomLocation(world, min, max);
            loc.getChunk().load(true);
        }

        loc = loc.add(.5, 1, .5);

        tempSpecSpawn = loc;

        plugin.getServer().getOnlinePlayers().stream().filter(player -> player.getGameMode() != GameMode.SPECTATOR).forEach(player -> {
            if (!allPlayers.contains(player.getUniqueId())) {
                int c = PHuntUtils.randomInteger(1, 2);
                if (c == 1) {
                    addToTeam(Teams.HIDERS, player);
                } else {
                    addToTeam(Teams.HUNTERS, player);
                }
            }

        });

        for (UUID uuid : allPlayers) {
            LivingEntity entity = (LivingEntity) Bukkit.getEntity(uuid);
            if(entity != null) {
                entity.teleport(loc);
                if(entity instanceof Player) {
                    giveKit((Player) entity);
                }
            }
        }

        huntersFrozen = true;
        roundCount++;
        setGameState(GameStates.ACTIVE);
    }

    public void endGame() {
        List<UUID> players = new ArrayList<>(allPlayers);

        Map<UUID, Integer> mostKills = entityWithHighestKills();
        List<LivingEntity> topKillers = new ArrayList<>();

        int kills = 0;
        if(mostKills != null) {
            for (UUID uuid : mostKills.keySet()) {
                LivingEntity entity = (LivingEntity) Bukkit.getEntity(uuid);
                if (entity == null) continue;
                topKillers.add(entity);
                kills = mostKills.get(uuid);
            }
        }

        int finalKills = kills;
        players.forEach(uuid -> {
            LivingEntity entity = (LivingEntity) Bukkit.getEntity(uuid);
            if(entity == null) return;
            Teams team = Teams.getPlayerTeam(entity);

            if(entity instanceof Player) {
                Player player = (Player) entity;

                allItems.forEach(ib -> ItemBuilder.removeItem(player, ib.getItem()));
                player.setGameMode(GameMode.SURVIVAL);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
            }

            if(team != null) {
                if(team == Teams.HIDERS) {
                    propManager.clearDisguise(entity);
                }
                Teams.clearTeam(entity, this, false);
            }

            entity.teleport(getMapManager().getLobbySpawnpoint());

            entity.resetMaxHealth();
            entity.setHealth(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            entity.setInvisible(false);

            displayRoundInfo(entity, topKillers, finalKills);

        });
        players.clear();

        cleanup();
        setGameState(GameStates.LOBBY);
    }

    public void displayWinner() {
        cancelTimers();
        plugin.getServer().getOnlinePlayers().forEach(bar::addPlayer);
        bar.setVisible(false);

        switch (winningCondition) {
            case TIED:
                bar.setTitle(PHuntMessages.translate("&eRound ended in a tie!"));
                bar.setVisible(true);
                break;

            case FORCE_QUIT:
                bar.setTitle(PHuntMessages.translate("&c&lForce Quit Round!"));
                bar.setVisible(true);
                break;

            case TIME_OVER:
                int hunters = Teams.HUNTERS.getMembers().size();
                int hiders = Teams.HIDERS.getMembers().size();
                if(hunters > hiders) {
                    setWinningCondition(WinningCondition.HUNTERS_WIN);
                } else if(hiders > hunters) {
                    setWinningCondition(WinningCondition.HIDERS_WIN);
                } else {
                    setWinningCondition(WinningCondition.TIED);
                }
                displayWinner();
                break;

            case HIDERS_WIN:
                bar.setTitle(PHuntMessages.translate(Teams.HIDERS.getColor() + Teams.HIDERS.getName() + " have won!"));
                bar.setVisible(true);
                break;

            case HUNTERS_WIN:
                bar.setTitle(PHuntMessages.translate(Teams.HUNTERS.getColor() + Teams.HUNTERS.getName() + " have won!"));
                bar.setVisible(true);
                break;

        }

    }

    public void giveKit(Player player) {
        Teams team = Teams.getPlayerTeam(player);
        if(team == null) return;
        switch (team) {

            case HIDERS:
                allItems.forEach(ib -> ItemBuilder.removeItem(player, ib.getItem()));

                swapper.giveToPlayer(player);
                taunter.giveToPlayer(player);
                break;

            case HUNTERS:
                allItems.forEach(ib -> ItemBuilder.removeItem(player, ib.getItem()));

                hunterWeapon.giveToPlayer(player);
                break;

            case DEAD:
                allItems.forEach(ib -> ItemBuilder.removeItem(player, ib.getItem()));

                spectatorCompass.giveToPlayer(player);
                break;
        }
    }

    public void cancelTimers() {
        Bukkit.getScheduler().cancelTasks(plugin);
        bar.setTitle("");
        bar.setVisible(false);
        bar.removeAll();
    }

    public void cleanup() {
        cancelTimers();

        allPlayers.clear();
        killCounter.clear();
        plugin.getListener().getDisconnectTeamMap().clear();
        plugin.getListener().getDisconnectRoundCountMap().clear();

        propManager.getLockedMEG().clear();
        propManager.getLockedEntities().clear();
        propManager.getLockedBlocks().clear();

        propManager.getLockedPlayers().clear();

        propManager.getDisguisedMobs().clear();
        propManager.getDisguisedMEG().clear();
        propManager.getDisguisedBlocks().clear();

        mapManager.resetMap(mapManager.getCurrentMap());
        mapManager.getVotedFor().clear();
        mapManager.clearVotes();

    }

    private void displayRoundInfo(LivingEntity receiver, List<LivingEntity> topKillers, int kills) {
        receiver.sendMessage(PHuntMessages.translate("&e&l&m-------------------------"));

        if(topKillers.size() != 0) {
            if (topKillers.size() >= 2) {
                StringBuilder topKillerList = new StringBuilder();
                for(LivingEntity entity : topKillers) {
                    if (topKillerList.length() == 0) {
                        topKillerList.append(PHuntMessages.translate("&7" + entity.getName() + "&r"));
                    } else {
                        topKillerList.append(PHuntMessages.translate("&r, &7" + entity.getName() + "&r"));
                    }
                }

                receiver.sendMessage(PHuntMessages.translate("Top killers were: " + topKillerList + " with " + kills + (kills > 1 ? " kills" : " kill") + " each."));
            } else {
                receiver.sendMessage(PHuntMessages.translate("Top killer was: " + topKillers.get(0).getName() + " with " + kills + (kills > 1 ? " kills." : " kill.")));
            }
        }

        if(killCounter.containsKey(receiver.getUniqueId())) {
            int playerKills = killCounter.get(receiver.getUniqueId());
            receiver.sendMessage(PHuntMessages.translate("You had a total of " + playerKills + " kills"));

        } else receiver.sendMessage(PHuntMessages.translate("You had a total of 0 kills"));

        receiver.sendMessage(PHuntMessages.translate("&e&l&m-------------------------"));
    }

    public void bloodAnimation(Location loc) {
        World world = loc.getWorld();
        if(world == null) return;

        Particle.DustOptions options = new Particle.DustOptions(Color.fromRGB(222, 30, 24), 1.5F);
        world.spawnParticle(Particle.REDSTONE, loc, 25,
                PHuntUtils.randomDouble(0.008, 0.01),
                PHuntUtils.randomDouble(0.005, 0.01),
                PHuntUtils.randomDouble(0.007, 0.01),
                options);

    }

    public void setWinningCondition(WinningCondition winningCondition) {
        this.winningCondition = winningCondition;
    }

    public GameStates getState() {
        return state;
    }

    public int getRoundCount() {
        return roundCount;
    }

    public boolean areHuntersFrozen() {
        return huntersFrozen;
    }

    public ItemBuilder getSpectatorCompass() {
        return spectatorCompass;
    }

    public List<ItemStack> getAllItems() {
        List<ItemStack> items = new ArrayList<>();
        allItems.forEach(ib -> items.add(ib.getItem()));
        return items;
    }

    public List<UUID> getAllPlayers() {
        return allPlayers;
    }

    public Map<UUID, Integer> getKillCounter() {return killCounter;}

    public Map<UUID, Integer> entityWithHighestKills() {
        Map<Integer, UUID> killMap = new TreeMap<>();
        for(UUID uuid : killCounter.keySet()) {
            int kills = killCounter.get(uuid);
            killMap.put(kills, uuid);
        }

        List<Integer> killOrder = new ArrayList<>(killMap.keySet());
        Comparator<Integer> comp = Comparator.reverseOrder();
        killOrder.sort(comp);

        int k = 0;
        if(!killOrder.isEmpty()) {
            k = killOrder.get(0);
        }


        List<UUID> entityWithHighestKills = new ArrayList<>();
        for(UUID uuid : killCounter.keySet()) {
            if(killCounter.get(uuid) == k) {
                entityWithHighestKills.add(uuid);
            }
        }

        Map<UUID, Integer> highestKillTracker = new HashMap<>();
        for(UUID uuid : entityWithHighestKills) {
            highestKillTracker.put(uuid, k);
        }

        if(!entityWithHighestKills.isEmpty()) return highestKillTracker;

        return null;
    }

    public PHuntSettingsManager getSettingsManager() {
        return settingsManager;
    }

    public SpectatorManager getSpectatorManager() {
        return spectatorManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public PropManager getPropManager() {return propManager;}

    public Location tempSpecSpawn() {return tempSpecSpawn;}


}
