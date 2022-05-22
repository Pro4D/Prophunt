package com.pro4d.prophunt.misc;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class PHuntMap {
    
    private final Vector cornerOne;
    private final Vector cornerTwo;
    private final String name;
    private final World world;
    private final List<Block> blocks;

    public PHuntMap(String name, World world, Vector cornerOne, Vector cornerTwo) {
        this.cornerOne = cornerOne;
        this.cornerTwo = cornerTwo;
        this.name = name;
        this.world = world;
        blocks = new ArrayList<>();
    }

    public void createBlockList() {
        blocks.clear();
        Location firstCorner = new Location(world, cornerOne.getBlockX(), cornerOne.getBlockY(), cornerOne.getZ());
        Location secondCorner = new Location(world, cornerTwo.getBlockX(), cornerTwo.getBlockY(), cornerTwo.getZ());

        int minX = Math.min(firstCorner.getBlockX(), secondCorner.getBlockX());
        int minY = Math.min(firstCorner.getBlockY(), secondCorner.getBlockY());
        int minZ = Math.min(firstCorner.getBlockZ(), secondCorner.getBlockZ());

        int maxX = Math.max(firstCorner.getBlockX(), secondCorner.getBlockX());
        int maxY = Math.max(firstCorner.getBlockY(), secondCorner.getBlockY());
        int maxZ = Math.max(firstCorner.getBlockZ(), secondCorner.getBlockZ());

        for(int lX = minX; lX <= maxX; lX++) {
            for(int lY = minY; lY <= maxY; lY++) {
                for(int lZ = minZ; lZ <= maxZ; lZ++) {
                    Block b = world.getBlockAt(lX, lY, lZ);
                    blocks.add(b);
                }
            }
        }

    }

    public Vector getCornerOne() {
        return cornerOne;
    }

    public Vector getCornerTwo() {
        return cornerTwo;
    }

    public String getName() {
        return name;
    }

    public World getWorld() {
        return world;
    }

    public List<Block> getBlocks() {return blocks;}

}
