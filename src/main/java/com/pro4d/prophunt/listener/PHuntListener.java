package com.pro4d.prophunt.listener;

import com.mojang.datafixers.util.Pair;
import com.pro4d.prophunt.Prophunt;
import com.pro4d.prophunt.misc.TeamInventory;
import com.pro4d.prophunt.enums.Teams;
import com.pro4d.prophunt.enums.WinningCondition;
import com.pro4d.prophunt.managers.PHuntGameManager;
import com.pro4d.prophunt.managers.PropManager;
import com.pro4d.prophunt.misc.FakeBlock;
import com.pro4d.prophunt.misc.GameStates;
import com.pro4d.prophunt.utils.PHuntMessages;
import com.pro4d.prophunt.utils.PHuntUtils;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ModeledEntity;
import net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment;
import net.minecraft.world.entity.EnumItemSlot;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PHuntListener implements Listener {

    private final Prophunt plugin;
    private final PHuntGameManager gameManager;
    private final PropManager propManager;

    private final Map<UUID, Teams> disconnectTeamMap;
    private final Map<UUID, Integer> disconnectRoundCountMap;

    private final List<Pair<EnumItemSlot, net.minecraft.world.item.ItemStack>> list;
    public PHuntListener(Prophunt plugin) {

        this.plugin = plugin;
        gameManager = plugin.getGameManager();
        propManager = gameManager.getPropManager();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        disconnectTeamMap = new HashMap<>();
        disconnectRoundCountMap = new HashMap<>();
        //lastTeleportedTo = new HashMap<>();

        list = new ArrayList<>();
        list.add(new Pair<>(EnumItemSlot.a, CraftItemStack.asNMSCopy(new ItemStack(Material.AIR))));
        
    }


    @EventHandler
    private void onLeave(PlayerQuitEvent event) {
        if(Teams.getPlayerTeam(event.getPlayer()) == null) return;
        Player player = event.getPlayer();
        Teams team = Teams.getPlayerTeam(player);


        int rc = gameManager.getRoundCount();
        disconnectTeamMap.put(player.getUniqueId(), team);
        disconnectRoundCountMap.put(player.getUniqueId(), rc);
    }


    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        if(gameManager.getState() == GameStates.LOBBY) {
            if (gameManager.getMapManager().getLobbySpawnpoint() != null) {
                //do player count check
                //enough players have joined, begin countdown
                event.getPlayer().teleport(gameManager.getMapManager().getLobbySpawnpoint());
            }
            if (gameManager.getSettingsManager().autoStart()) {
                long playerCount = plugin.getServer().getOnlinePlayers().stream().filter(player -> player.getGameMode() != GameMode.SPECTATOR).count();
                if (playerCount >= gameManager.getSettingsManager().getMinimumPlayerCount()) {
                    gameManager.setGameState(GameStates.STARTING);
                }
            }
            if(Teams.getPlayerTeam(event.getPlayer()) == null) {

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getMessages().getTeamMessages().forEach(teamMessage -> event.getPlayer().spigot().sendMessage(teamMessage));
                    }
                }.runTaskLater(plugin, 30L);

            }
        }
        if(gameManager.getState() == GameStates.ACTIVE) {
            Player player = event.getPlayer();
            if(!disconnectRoundCountMap.containsKey(player.getUniqueId())) return;
            if(disconnectRoundCountMap.get(player.getUniqueId()) == gameManager.getRoundCount()) {
                if(!disconnectTeamMap.containsKey(player.getUniqueId())) return;
                Teams team = disconnectTeamMap.get(player.getUniqueId());
                gameManager.addToTeam(team, player);

            }
        }

        if(!plugin.getHeads().containsKey(event.getPlayer().getUniqueId())) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    Player player = event.getPlayer();

                    ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                    assert meta != null;

                    meta.setDisplayName(PHuntMessages.translate("&e" + player.getName()));
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
                    skull.setItemMeta(meta);

                    plugin.getHeads().put(player.getUniqueId(), skull);
                }
            }.runTaskLaterAsynchronously(plugin, 10L);

        }

    }


    @EventHandler
    private void onDeath(EntityDeathEvent event) {
        if(gameManager.getState() != GameStates.ACTIVE) return;
        LivingEntity entity = event.getEntity();
        if(Teams.getPlayerTeam(entity) == null) return;

        Teams team = Teams.getPlayerTeam(entity);
        assert team != null;

        gameManager.removeFromTeam(team, entity, false);

        if(team == Teams.HIDERS) {
            propManager.clearDisguise(entity);
            if(team.getMembers().isEmpty()) {
                gameManager.setWinningCondition(WinningCondition.HUNTERS_WIN);
                gameManager.setGameState(GameStates.ENDING);
            }
        } else if(team == Teams.HUNTERS) {
            if(team.getMembers().isEmpty()) {
                gameManager.setWinningCondition(WinningCondition.HIDERS_WIN);
                gameManager.setGameState(GameStates.ENDING);
            }
        }

        gameManager.addToTeam(Teams.DEAD, entity);
        if(entity instanceof Player) {
            gameManager.giveKit((Player) entity);
            gameManager.getSpectatorManager().getOldTeam().put(entity.getUniqueId(), team);
        }

        if(entity.getKiller() != null) {
            Player killer = entity.getKiller();
            UUID killerUUID = killer.getUniqueId();
            if(gameManager.getKillCounter().containsKey(killerUUID)) {
                int kills = gameManager.getKillCounter().get(killerUUID);
                gameManager.getKillCounter().replace(killerUUID, kills + 1);
            } else {
                gameManager.getKillCounter().put(killerUUID, 1);
            }
        }
    }


    @EventHandler
    private void onRespawn(PlayerRespawnEvent event) {
        if(gameManager.getState() != GameStates.ACTIVE) return;
        Player player = event.getPlayer();
        if(Teams.getPlayerTeam(player) == null) return;
        if(Teams.getPlayerTeam(player) != Teams.DEAD) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                player.teleport(gameManager.tempSpecSpawn());
                player.setAllowFlight(true);
                player.setFlying(true);
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    private void onDamage(EntityDamageByEntityEvent event) {
        if(gameManager.getState() != GameStates.ACTIVE) return;
        if(!(event.getDamager() instanceof Player)) return;

        LivingEntity damager = (LivingEntity) event.getDamager();
        LivingEntity entityDamaged = (LivingEntity) event.getEntity();

        Teams damagerTeam = Teams.getPlayerTeam(damager);
        if(damagerTeam == null) return;

        Teams entityDamagedTeam = Teams.getPlayerTeam(entityDamaged);

        if(entityDamagedTeam == null) return;

        if(!gameManager.getSettingsManager().allowTeamPVP()) {
            if(damagerTeam == entityDamagedTeam) event.setCancelled(true);
        }

        if(!gameManager.getSettingsManager().allowPVP()) {
            if(gameManager.getState() == GameStates.ACTIVE) event.setCancelled(true);
        }

    }


    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
        if(gameManager.getState() != GameStates.ACTIVE) return;
        if(event.getHand() == EquipmentSlot.OFF_HAND) return;
        Player player = event.getPlayer();
        Teams team = Teams.getPlayerTeam(player);
        if(team == null) return;
        //if(team == Teams.SPECTATORS) return;

        if(team == Teams.HUNTERS) {
            if(!event.hasBlock()) return;
            if(event.getClickedBlock() == null) return;

            FakeBlock fakeBlock = propManager.getFakeBlock(event.getClickedBlock().getLocation());
            if(fakeBlock == null) return;

            double damage = 2.0;
            AttributeInstance attackDamage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if(attackDamage != null) {
                damage = attackDamage.getValue();
            }

            LivingEntity hider = (LivingEntity) Bukkit.getEntity(fakeBlock.getDisguised());
            if(hider != null) {
                hider.damage(damage, player);
            }
            return;
        }

        if(!event.hasItem()) return;
        if(event.getItem() == null) return;

        if(!gameManager.getAllItems().contains(event.getItem())) return;
        ItemStack item = event.getItem();
        assert item != null;

        if(item.isSimilar(gameManager.getSpectatorCompass().getItem())) {
            if(Teams.getPlayerTeam(player) != Teams.DEAD) return;
            Teams oldTeam = gameManager.getSpectatorManager().getOldTeam().get(player.getUniqueId());

            for(TeamInventory teamInv : gameManager.getSpectatorManager().getInventories()) {
                if(teamInv.getTeam() == oldTeam) {
                    gameManager.getSpectatorManager().openMenu(player, oldTeam);
                }
            }
        }

        if(event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) return;
        event.setCancelled(true);

        if(item.isSimilar(gameManager.getSettingsManager().getPropSwapper())) {
            if(event.hasBlock()) {
                Block b = event.getClickedBlock();
                assert b != null;

                if(!gameManager.getSettingsManager().getBlockHealth().containsKey(b.getType())) return;
                propManager.disguiseAsBlock(player, b.getBlockData());

            } else if (PHuntUtils.getEntityInLOS(player, plugin.getDistance()) != null) {
                LivingEntity entity = PHuntUtils.getEntityInLOS(player, plugin.getDistance());
                if(entity instanceof Player) {
                    player.sendMessage(PHuntMessages.cantDisguiseAsPlayer());
                    return;
                }
                assert entity != null;

                ModeledEntity me = ModelEngineAPI.api.getModelManager().getModeledEntity(entity.getUniqueId());
                if(me != null) {

                    String modelID = "";

                    for(String s : me.getAllActiveModel().keySet()) {
                        if(me.getActiveModel(s) != null) {
                            modelID = me.getActiveModel(s).getModelId();
                        }
                    }

                    if(ModelEngineAPI.getModelBlueprint(modelID) == null) return;

                    if(!gameManager.getSettingsManager().getMegHealth().containsKey(modelID)) return;
                    propManager.disguiseAsModelEngineMob(player, modelID);

                } else {
                    if(!gameManager.getSettingsManager().getEntityHealth().containsKey(entity.getType())) return;
                    propManager.disguiseAsMob(player, entity);
                }
            }
        }

        if(item.isSimilar(gameManager.getSettingsManager().getPropTaunter())) {
            plugin.getMessages().getTaunts().forEach(comp -> player.spigot().sendMessage(comp));
        }

    }


    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {
        if(gameManager.getState() != GameStates.ACTIVE) return;
        if(gameManager.getSettingsManager().allowBlockBreaking()) return;
        event.setCancelled(true);
    }


    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if(gameManager.getState() != GameStates.ACTIVE) return;
        Player player = event.getPlayer();
        if(Teams.getPlayerTeam(player) == null) return;
        Teams team = Teams.getPlayerTeam(player);
        if(gameManager.areHuntersFrozen()) {
            if(team == Teams.HUNTERS) {
                event.setCancelled(true);
            }
        }
        //do full block check
        if(team == Teams.HIDERS) {
            if (propManager.getLockedPlayers().contains(player.getUniqueId())) {
                double fX = event.getFrom().getX();
                double fY = event.getFrom().getY();
                double fZ = event.getFrom().getZ();

                double tX = event.getTo().getX();
                double tY = event.getTo().getY();
                double tZ = event.getTo().getZ();

                if(fX != tX || fY != tY || fZ != tZ) {
                    event.setCancelled(true);
                }
            }
        }

//        if(team == Teams.HIDERS) {
//            if(!player.getInventory().getItemInMainHand().isSimilar(gameManager.getSettingsManager().getPropSwapper())) return;
//
//            //highlight
//            if (PHuntUtils.findItem(player, gameManager.getSettingsManager().getPropSwapper()) != -1) {
//                if(player.getTargetBlockExact(5, FluidCollisionMode.ALWAYS) != null) {
//                    Block b = player.getTargetBlockExact(5, FluidCollisionMode.ALWAYS);
//                    Bukkit.broadcastMessage("B: " + b.getType().name() + " L: " + b.getLocation());
//                }
//            }
//        }
    }


    @EventHandler
    private void sneak(PlayerToggleSneakEvent event) {
        if(gameManager.getState() != GameStates.ACTIVE) return;
        if(Teams.getPlayerTeam(event.getPlayer()) != Teams.HIDERS) return;
        if(!event.isSneaking()) return;
        Player player = event.getPlayer();

        if(propManager.isDisguised(player)) {
            if(!player.isOnGround()) return;

            if(propManager.isDisguisedAsMob(player)) {
                propManager.lockEntity(player);

            } else if(propManager.isDisguisedAsMEGModel(player)) {
                propManager.lockMEG(player);

            } else if(propManager.isDisguisedAsBlock(player)) {
                propManager.lockBlock(player, propManager.getDisguisedBlocks().get(player.getUniqueId()));
            }

        } else if(propManager.getLockedBlocks().containsKey(player.getUniqueId())) {
            propManager.unlockBlock(player);

        } else if(propManager.getLockedEntities().containsKey(player.getUniqueId())) {
            propManager.unlockEntity(player);

        } else if(propManager.getLockedMEG().containsKey(player.getUniqueId())) {
            propManager.unlockMEG(player);

        }


    }


    @EventHandler
    private void hideItem(PlayerItemHeldEvent event) {
        if(gameManager.getState() != GameStates.ACTIVE) return;
        Player player = event.getPlayer();
        if(Teams.getPlayerTeam(player) == null) return;
        Teams team = Teams.getPlayerTeam(player);
        if(team != Teams.HIDERS) return;
        
        CraftPlayer hider = (CraftPlayer) player;
        PacketPlayOutEntityEquipment itemPacket = new PacketPlayOutEntityEquipment(hider.getEntityId(), list);

        for(UUID uuid : gameManager.getAllPlayers()) {
            Player hunter = Bukkit.getPlayer(uuid);
            if(hunter == null) continue;
            if(hunter == player) continue;

            CraftPlayer craftHunter = (CraftPlayer) hunter;
            craftHunter.getHandle().b.a(itemPacket);
        }

    }


    @EventHandler
    private void foodDeplete(FoodLevelChangeEvent event) {
        if(!(event.getEntity() instanceof Player)) return;
        event.setCancelled(true);
    }


    @EventHandler
    private void onClick(InventoryClickEvent event) {
        if(event.getClickedInventory() == null) return;
        if(event.getCurrentItem() == null) return;
        if(Teams.getPlayerTeam(event.getWhoClicked()) != Teams.DEAD) return;
        Player player = (Player) event.getWhoClicked();
        Teams oldTeam = gameManager.getSpectatorManager().getOldTeam().get(player.getUniqueId());
        if(oldTeam == null) return;

        for(TeamInventory teamInv : gameManager.getSpectatorManager().getInventories()) {
            if(teamInv.getTeam() == oldTeam) {
                event.setCancelled(true);

                UUID toTeleport = gameManager.getSpectatorManager().getUUID(event.getCurrentItem());

                if(toTeleport == null) return;
                Player teleportTo = Bukkit.getPlayer(toTeleport);
                if(Teams.getPlayerTeam(teleportTo) != oldTeam) return;
                if(teleportTo == null) return;
                player.teleport(teleportTo);
                player.sendMessage(PHuntMessages.teleportedTo().replace("%target%", teleportTo.getName()));

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.closeInventory();
                    }
                }.runTask(plugin);
                return;
            }
        }

    }


    @EventHandler
    private void dropItem(PlayerDropItemEvent event) {
        if(gameManager.getState() != GameStates.ACTIVE) return;
        if(!gameManager.getAllItems().contains(event.getItemDrop().getItemStack())) return;
        event.getItemDrop().setPickupDelay(20);
        event.setCancelled(true);
    }


    @EventHandler
    private void damageItem(PlayerItemDamageEvent event) {
        if(gameManager.getState() != GameStates.ACTIVE) return;
        if(!gameManager.getAllItems().contains(event.getItem())) return;
        event.setCancelled(true);
    }



    public Map<UUID, Teams> getDisconnectTeamMap() {return disconnectTeamMap;}

    public Map<UUID, Integer> getDisconnectRoundCountMap() {
        return disconnectRoundCountMap;
    }

}
