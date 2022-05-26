package com.pro4d.prophunt.misc;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public class FakeBlock {

    private final Location loc;
    private final UUID disguised;
    private final BlockData blockData;
    private LivingEntity entity;

    public FakeBlock(UUID owner, Location loc, BlockData bd) {
        this.loc = loc;
        this.disguised = owner;
        this.blockData = bd;
    }

    public ArmorStand spawnFakeBlock(Location loc, World world) {
        ArmorStand as = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
        as.setBasePlate(false);
        as.setArms(false);
        as.setAI(false);
        as.setGravity(false);
        as.setInvisible(true);
        as.setInvulnerable(true);

        return as;
    }

    public Location getLoc() {
        return loc;
    }

    public UUID getDisguised() {
        return disguised;
    }

    public BlockData getBlockData() {return blockData;}

    public void setEntity(LivingEntity entity) {this.entity = entity;}

    public LivingEntity getEntity() {return entity;}
}
