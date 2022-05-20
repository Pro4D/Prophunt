package com.pro4d.prophunt.commands;

import com.pro4d.prophunt.Prophunt;
import com.pro4d.prophunt.enums.Teams;
import com.pro4d.prophunt.enums.WinningCondition;
import com.pro4d.prophunt.managers.PHuntGameManager;
import com.pro4d.prophunt.misc.GameStates;
import com.pro4d.prophunt.misc.PHuntMap;
import com.pro4d.prophunt.utils.PHuntMessages;
import com.pro4d.prophunt.utils.PHuntUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

//optimize
public class PHuntCommands implements CommandExecutor {

    private final Prophunt plugin;
    private final PHuntGameManager gameManager;

    public PHuntCommands(Prophunt plugin) {
        this.plugin = plugin;
        gameManager = plugin.getGameManager();
        plugin.getServer().getPluginCommand("prophunt").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if(args[0].equals("reload")) {
            plugin.getMapConfig().saveConfig();
            plugin.getSettingsConfig().saveConfig();
            plugin.getSwapConfig().saveConfig();

            gameManager.getSettingsManager().validateMapConfig();
            gameManager.getSettingsManager().validateSettingsConfig();
            gameManager.getSettingsManager().validateSwappablesConfig();

            sender.sendMessage(PHuntMessages.reloadMessage());
            return true;
        }
        if(!(sender instanceof Player)) {
            sender.sendMessage(PHuntMessages.onlyPlayersMessage());
            return false;
        }
        if(args.length == 0) {
            sender.sendMessage(PHuntMessages.invalidCommandUsageMessage());
            return false;
        }

        Player player = (Player) sender;

        switch (args[0]) {

            case "set-map":
                if(args.length != 2) {
                    player.sendMessage(PHuntMessages.invalidCommandUsageMessage());
                    return false;
                }

                boolean mapFound = false;
                for(PHuntMap map : gameManager.getMapManager().getAllMaps().keySet()) {
                    if (map.getName().equalsIgnoreCase(args[1])) {
                        mapFound = true;
                        break;
                    }
                }
                if(!mapFound) {
                    player.sendMessage(PHuntMessages.noMapWithThatName().replace("%input%", args[1]));
                    return false;
                }

                if(gameManager.getState() == GameStates.ACTIVE) {
                    player.sendMessage(PHuntMessages.cannotUseCommandDuringGame());
                    return false;
                }

                gameManager.getMapManager().setCurrentMapName(args[1]);
                return true;

            case "set-team":
                if(args.length > 3 || args.length < 2) {
                    player.sendMessage(PHuntMessages.invalidCommandUsageMessage());
                    return false;
                }
                if(gameManager.getState() == GameStates.ACTIVE || gameManager.getState() == GameStates.ENDING) {
                    player.sendMessage(PHuntMessages.cannotUseCommandDuringGame());
                }

                if(args.length == 2) {
                    if(Teams.getTeam(args[1]) == null) {
                        player.sendMessage(PHuntMessages.invalidTeam().replace("%team%", args[1]));
                        return false;
                    }
                    Teams team = Teams.getTeam(args[1]);
                    assert team != null;
                    gameManager.addToTeam(team, player);
                } else if(args[1].equals("-entity")) {
                        LivingEntity entity = PHuntUtils.getEntityInLOS(player, plugin.getDistance());
                        if(entity == null) {
                            player.sendMessage(PHuntMessages.invalidMob().replace("%num%", String.valueOf(plugin.getDistance())));
                            return false;
                        }
                        if(Teams.getTeam(args[2]) == null) {
                            player.sendMessage(PHuntMessages.invalidTeam().replace("%team%", args[2]));
                            return false;
                        }

                        Teams team = Teams.getTeam(args[2]);
                        gameManager.addToTeam(team, entity);
                        return true;
                } else if(Bukkit.getPlayer(args[1]) != null) {
                    Player target = Bukkit.getPlayer(args[1]);
                    assert target != null;

                    if(Teams.getTeam(args[2]) == null) {
                        player.sendMessage(PHuntMessages.invalidTeam().replace("%team%", args[2]));
                        return false;
                    }
                    Teams team = Teams.getTeam(args[1]);

                    gameManager.addToTeam(team, target);
                }
                return true;

            case "clear-team":
                if(args.length > 2) {
                    player.sendMessage(PHuntMessages.invalidCommandUsageMessage());
                    return false;
                }
                if(gameManager.getState() == GameStates.ACTIVE || gameManager.getState() == GameStates.ENDING) {
                    player.sendMessage(PHuntMessages.cannotUseCommandDuringGame());
                }

                if(args.length == 2) {
                    if (args[1].equals("-entity")) {
                        LivingEntity entity = PHuntUtils.getEntityInLOS(player, plugin.getDistance());
                        if (entity == null) {
                            player.sendMessage(PHuntMessages.invalidMob().replace("%num%", String.valueOf(plugin.getDistance())));
                            return false;
                        }
                        if (Teams.getPlayerTeam(entity) == null) {
                            player.sendMessage(PHuntMessages.translate("&cEntity not on team"));
                            return true;
                        }
                        Teams.clearTeam(entity, gameManager, true);
                    } else if (Bukkit.getPlayer(args[1]) != null) {
                        Player target = Bukkit.getPlayer(args[1]);
                        Teams.clearTeam(target, gameManager, true);
                    } else player.sendMessage(PHuntMessages.invalidCommandUsageMessage());
                }

                Teams.clearTeam(player, gameManager, true);
                return true;

            case "start":
                if(args.length != 1) {
                    player.sendMessage(PHuntMessages.invalidCommandUsageMessage());
                    return false;
                }
                if(gameManager.getState() != GameStates.LOBBY) {
                    player.sendMessage(PHuntMessages.cannotUseCommandDuringGame());
                    return false;
                }
                if(gameManager.getAllPlayers().size() < gameManager.getSettingsManager().getMinimumPlayerCount()) {
                    player.sendMessage(PHuntMessages.notEnoughPlayers());
                    return false;
                }

                gameManager.setGameState(GameStates.STARTING);
                return true;

            case "end":
                if(args.length != 1) {
                    player.sendMessage(PHuntMessages.invalidCommandUsageMessage());
                    return false;
                }
                if(gameManager.getState() != GameStates.ACTIVE) {
                    player.sendMessage(PHuntMessages.noGameIsRunning());
                    return false;
                }

                gameManager.setWinningCondition(WinningCondition.FORCE_QUIT);
                gameManager.setGameState(GameStates.ENDING);
                return true;

//            "chicken" death
//            "iron-golem" death
//            "anvil" place
//            "dragon" growl
//            "enderman" teleport
//            elder guardian "curse"
//            "firework" taunt
//            "wolf" ambient
//            "skeleton" hurt
//            "cave" sound
            case "play-sound":
                if(args.length != 2) {
                    player.sendMessage(PHuntMessages.invalidCommandUsageMessage());
                    return false;
                }
                World world = player.getWorld();
                Location loc = player.getLocation();
                switch (args[1]) {
                    case "chicken":
                        world.playSound(loc, Sound.ENTITY_CHICKEN_AMBIENT, SoundCategory.MASTER, 20, 1);
                        return true;

                    case "anvil":
                        world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.MASTER, 20, 1);
                        return true;

                    case "iron-golem":
                        world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.MASTER, 20, 1);
                        return true;

                    case "dragon":
                        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.MASTER, 20, 1);
                        return true;

                    case "enderman":
                        world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.MASTER, 20, 1);
                        return true;

                    case "curse":
                        world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.MASTER, 20, 1);
                        return true;

                    case "firework":
                        world.spawnEntity(loc, EntityType.FIREWORK);
                        return true;

                    case "wolf":
                        world.playSound(loc, Sound.ENTITY_WOLF_AMBIENT, SoundCategory.MASTER, 20, 1);
                        return true;

                    case "skeleton":
                        world.playSound(loc, Sound.ENTITY_SKELETON_HURT, SoundCategory.MASTER, 20, 1);
                        return true;


                    case "cave":
                        world.playSound(loc, Sound.AMBIENT_CAVE, SoundCategory.MASTER, 40, 1);
                        return true;

                }

                return true;

            case "map-vote":
                if(args.length != 2) {
                    player.sendMessage(PHuntMessages.invalidCommandUsageMessage());
                    return false;
                }
                if(gameManager.getMapManager().isMap(args[1])) {
                    PHuntMap map = gameManager.getMapManager().getMap(args[1]);
                    gameManager.getMapManager().voteForMap(map.getName(), player);
                }
                return true;


            case "lobby":
                player.teleport(gameManager.getMapManager().getLobbySpawnpoint());
                return true;

//            case "text":
//                StringBuilder builder2 = new StringBuilder();
//                for(String arg : args) {
//                    if(arg.equals(args[0])) continue;
//                    builder2.append(" ").append(arg);
//                }
//                player.sendMessage("S: " + PHuntMessages.translate(builder2.toString()));
//                return true;

            case "bA":
                gameManager.bloodAnimation(player.getLocation().add(0, .3, 0));
                return true;

            case "nT":

                return true;

//            case "shU":
//                Location l = player.getWorld().getBlockAt(player.getLocation()).getLocation();
//                LivingEntity shulker = (LivingEntity) player.getWorld().spawnEntity(l, EntityType.SHULKER);
//
//                shulker.setAI(false);
//                shulker.setInvulnerable(true);
//                shulker.setInvisible(true);
//                shulker.setGlowing(true);
//                return true;

//            case "temp":
////                try {
////                    plugin.getMapConfig().getConfig().save(plugin.getMapConfig().getFile());
////                } catch (IOException e) {
////                    plugin.getUtils().log(Level.CONFIG, "Could not save 'map.yml' ");
////                }
////                //plugin.getMapConfig().saveConfig();

//                return true;

        }

        player.sendMessage(PHuntMessages.invalidCommandUsageMessage());
        return false;
    }


}
