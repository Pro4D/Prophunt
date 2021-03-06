package com.pro4d.prophunt.managers;

import com.pro4d.prophunt.misc.FakeBlock;
import com.pro4d.prophunt.misc.FakeEntity;
import com.pro4d.prophunt.utils.PHuntMessages;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.ModeledPlayer;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.DisguiseConfig;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MiscDisguise;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class PropManager {

    private final Map<UUID, LivingEntity> lockedEntities;
    private final Map<UUID, String> lockedMEG;
    private final Map<UUID, BlockData> lockedBlocks;

    private final Map<UUID, LivingEntity> disguisedMobs;
    private final Map<UUID, String> disguisedMEG;
    private final Map<UUID, BlockData> disguisedBlocks;

    private final List<FakeBlock> fakeBlocks;
    private final List<FakeEntity> fakeEntities;
    private final List<UUID> lockedPlayers;

    private final PHuntSettingsManager settingsManager;
    public PropManager(PHuntSettingsManager settingsManager) {
        lockedPlayers = new ArrayList<>();
        lockedBlocks = new HashMap<>();
        lockedEntities = new HashMap<>();
        lockedMEG = new HashMap<>();

        disguisedMobs = new HashMap<>();
        disguisedMEG = new HashMap<>();
        disguisedBlocks = new HashMap<>();
        this.settingsManager = settingsManager;

        fakeBlocks = new ArrayList<>();
        fakeEntities = new ArrayList<>();

    }

    public void disguiseAsEntity(LivingEntity target, LivingEntity entity) {
        clearDisguise(target);

        EntityType type = entity.getType();

        DisguiseType disguiseType = DisguiseType.getType(type);
        MobDisguise disguise = new MobDisguise(disguiseType);

        target.setCollidable(false);

        disguise.setEntity(target);
        disguise.setViewSelfDisguise(true);
        disguise.setNotifyBar(DisguiseConfig.NotifyBar.NONE);
        disguise.startDisguise();

        if(settingsManager.getEntityHealth().containsKey(type)) {
            AttributeInstance maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if(maxHealth != null) {
                maxHealth.setBaseValue(settingsManager.getEntityHealth().get(type));
                target.setHealth(maxHealth.getValue());
            }
        }

        disguisedMobs.put(target.getUniqueId(), entity);
    }

    public void disguiseAsMEGEntity(LivingEntity entity, String modelID) {
        clearDisguise(entity);

        ModeledEntity me = ModelEngineAPI.getModeledEntity(entity.getUniqueId());

        if (me != null) {
            me.clearModels();
            me.getAllActiveModel().clear();
        } else {
            me = ModelEngineAPI.createModeledEntity(entity);
        }

        me.addActiveModel(ModelEngineAPI.createActiveModel(modelID));
        me.detectPlayers();
        me.setInvisible(true);

        if (entity instanceof Player) {
            me.addPlayerAsync((Player) entity);
            if (me instanceof ModeledPlayer)
                ((ModeledPlayer) me).setViewSelf(true);
        }

        if(settingsManager.getMegHealth().containsKey(modelID)) {
            AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if(maxHealth != null) {
                maxHealth.setBaseValue(settingsManager.getMegHealth().get(modelID));
                entity.setHealth(maxHealth.getValue());
            }
        }
        disguisedMEG.put(entity.getUniqueId(), modelID);
    }

    public void disguiseAsBlock(LivingEntity entity, BlockData b) {
        clearDisguise(entity);

        MiscDisguise miscDisguise = new MiscDisguise(DisguiseType.FALLING_BLOCK, b.getMaterial());

        miscDisguise.setEntity(entity);
        miscDisguise.setNotifyBar(DisguiseConfig.NotifyBar.NONE);
        miscDisguise.startDisguise();

        if(settingsManager.getBlockHealth().containsKey(b.getMaterial())) {
            AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if(maxHealth != null) {
                maxHealth.setBaseValue(settingsManager.getBlockHealth().get(b.getMaterial()));
                entity.setHealth(maxHealth.getValue());
            }
        }

        disguisedBlocks.put(entity.getUniqueId(), b);
    }

    public boolean isDisguisedAsMob(LivingEntity entity) {
        return disguisedMobs.containsKey(entity.getUniqueId());
    }

    public boolean isDisguisedAsBlock(LivingEntity entity) {
        return disguisedBlocks.containsKey(entity.getUniqueId());
    }

    public boolean isDisguisedAsMEGModel(LivingEntity entity) {
        return disguisedMEG.containsKey(entity.getUniqueId());
    }

    public void lockBlock(LivingEntity entity) {
        if(!isDisguisedAsBlock(entity)) return;

        Location l = entity.getLocation().clone();
        World world = l.getWorld();
        if(world == null) return;

        if(world.getBlockAt(l).getType() != Material.AIR) {
            entity.sendMessage(PHuntMessages.cannotLockHere());
            return;
        }

        BlockData b = disguisedBlocks.get(entity.getUniqueId());

        clearDisguise(entity);
        entity.setInvisible(true);

        Location bLoc = world.getBlockAt(l).getLocation();
        Location bCenter = bLoc.clone().add(0.5, 0, 0.5);

        world.getPlayers().stream().filter(p -> p != entity).forEach(p -> p.sendBlockChange(bLoc, b));

        FakeBlock fb = new FakeBlock(entity.getUniqueId(), bLoc, b);

        if(entity instanceof Player) {
            ArmorStand as = fb.spawnFakeBlock(bCenter, world);

            MiscDisguise miscDisguise = new MiscDisguise(DisguiseType.FALLING_BLOCK, b.getMaterial());
            DisguiseAPI.disguiseToPlayers(as, miscDisguise, Collections.singletonList(entity));

            fb.setEntity(as);
        }

        lockedPlayers.add(entity.getUniqueId());
        lockedBlocks.put(entity.getUniqueId(), b);

        fakeBlocks.add(fb);

    }

    public void lockEntity(LivingEntity target) {
        if(!isDisguisedAsMob(target)) return;

        LivingEntity entity = disguisedMobs.get(target.getUniqueId());

        DisguiseType disguiseType = DisguiseType.getType(entity);
        MobDisguise disguise = new MobDisguise(disguiseType);

        Location loc = target.getLocation().clone();
        if(loc.getWorld() == null) return;

        clearDisguise(target);

        ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        as.setBasePlate(false);
        as.setArms(false);
        as.setAI(false);
        as.setGravity(false);

        if(settingsManager.getEntityHealth().containsKey(entity.getType())) {
            AttributeInstance maxHealth = as.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if(maxHealth != null) {
                maxHealth.setBaseValue(settingsManager.getEntityHealth().get(entity.getType()));
                as.setHealth(maxHealth.getValue());
            }
        }

        disguise.setEntity(as);
        disguise.startDisguise();
        DisguiseAPI.disguiseToAll(as, disguise);

        target.setInvisible(true);

        fakeEntities.add(new FakeEntity(target.getUniqueId(), as));

        lockedPlayers.add(target.getUniqueId());
        lockedEntities.put(target.getUniqueId(), entity);

    }

    public void lockMEG(LivingEntity entity) {
        if(!isDisguisedAsMEGModel(entity)) return;

        Location loc = entity.getLocation().clone();
        if(loc.getWorld() == null) return;

        clearDisguise(entity);
        ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        as.setBasePlate(false);
        as.setArms(false);
        as.setAI(false);
        as.setGravity(false);

        String modelID = disguisedMEG.get(entity.getUniqueId());

        if(settingsManager.getMegHealth().containsKey(modelID)) {
            AttributeInstance maxHealth = as.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if(maxHealth != null) {
                maxHealth.setBaseValue(settingsManager.getMegHealth().get(modelID));
                as.setHealth(maxHealth.getValue());
            }
        }

        ModeledEntity me = ModelEngineAPI.api.getModelManager().createModeledEntity(as);

        me.addActiveModel(ModelEngineAPI.createActiveModel(modelID));

        me.detectPlayers();
        me.setInvisible(true);
        me.setWalking(false);
        me.setJumping(false);

        entity.setInvisible(true);

        fakeEntities.add(new FakeEntity(entity.getUniqueId(), as));

        lockedPlayers.add(entity.getUniqueId());
        lockedMEG.put(entity.getUniqueId(), modelID);

    }


    public void unlockBlock(LivingEntity entity) {
        Location loc = entity.getLocation().add(0, 0.3, 0);
        entity.teleport(loc);

        if(loc.getWorld() != null) loc.getWorld().getBlockAt(loc).setType(Material.AIR);

        BlockData b = lockedBlocks.get(entity.getUniqueId());

        disguiseAsBlock(entity, b);

        lockedPlayers.remove(entity.getUniqueId());
        lockedBlocks.remove(entity.getUniqueId(), b);

    }

    public void unlockEntity(LivingEntity entity) {
        LivingEntity disguise = lockedEntities.get(entity.getUniqueId());
        disguiseAsEntity(entity, disguise);

        lockedEntities.remove(entity.getUniqueId(), disguise);
        lockedPlayers.remove(entity.getUniqueId());

    }

    public void unlockMEG(LivingEntity entity) {
        String modelID = lockedMEG.get(entity.getUniqueId());

        disguiseAsMEGEntity(entity, modelID);

        lockedPlayers.remove(entity.getUniqueId());
        lockedMEG.remove(entity.getUniqueId(), modelID);

    }


    public boolean isDisguised(LivingEntity entity) {return isDisguisedAsMob(entity) || isDisguisedAsBlock(entity) || isDisguisedAsMEGModel(entity);}

    public boolean isLocked(LivingEntity entity) {return lockedPlayers.contains(entity.getUniqueId());}

    public void clearDisguise(LivingEntity entity) {
        FakeBlock fb = getFakeBlock(entity.getUniqueId());
        if(fb != null) {
            World world = fb.getLoc().getWorld();
            if(world != null) {
                world.setBlockData(fb.getLoc(), Material.AIR.createBlockData());
                world.getPlayers().forEach(p -> p.sendBlockChange(fb.getLoc(), Material.AIR.createBlockData()));
            }

            if(fb.getEntity() != null) {
                fb.getEntity().remove();
            }
            fakeBlocks.remove(fb);
        }

        FakeEntity fE = getFakeEntity(entity.getUniqueId());
        if(fE != null) {fE.getEntity().remove();}

        if(isDisguisedAsMob(entity)) {
            DisguiseAPI.undisguiseToAll(entity);
            disguisedMobs.remove(entity.getUniqueId());


        } else if(isDisguisedAsBlock(entity)) {
            DisguiseAPI.undisguiseToAll(entity);
            disguisedBlocks.remove(entity.getUniqueId());

        } else if(isDisguisedAsMEGModel(entity)) {
            if(ModelEngineAPI.getModeledEntity(entity.getUniqueId()) != null) {
                ModeledEntity me = ModelEngineAPI.getModeledEntity(entity.getUniqueId());
                me.clearModels();
                me.getAllActiveModel().clear();
                disguisedMEG.remove(entity.getUniqueId());
            }
        }

        entity.setInvisible(false);
    }

    public FakeBlock getFakeBlock(UUID u) {
        for(FakeBlock fb : fakeBlocks) {
            if(fb.getDisguised().equals(u)) return fb;
        }
        return null;
    }

    public FakeBlock getFakeBlock(Location loc) {
        for(FakeBlock fb : fakeBlocks) {
            Location l = fb.getLoc();
            if(!l.equals(loc)) {
                l.subtract(.5, .5, .5);
            }
            if(l.equals(loc)) return fb;
        }
        return null;
    }

    public FakeEntity getFakeEntity(UUID u) {
        for(FakeEntity fE : fakeEntities) {
            if(fE.getDisguised().equals(u)) return fE;
        }

        return null;
    }

    public FakeEntity getFakeEntity(LivingEntity entity) {
        for(FakeEntity fE : fakeEntities) {
            if(fE.getEntity() == entity) return fE;
        }
        return null;
    }

    public List<UUID> getLockedPlayers() {return lockedPlayers;}

    public Map<UUID, BlockData> getLockedBlocks() {return lockedBlocks;}

    public Map<UUID, LivingEntity> getLockedEntities() {return lockedEntities;}

    public Map<UUID, String> getLockedMEG() {return lockedMEG;}

    public Map<UUID, BlockData> getDisguisedBlocks() {return disguisedBlocks;}

    public Map<UUID, LivingEntity> getDisguisedMobs() {
        return disguisedMobs;
    }

    public Map<UUID, String> getDisguisedMEG() {
        return disguisedMEG;
    }

}

