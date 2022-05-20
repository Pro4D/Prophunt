package com.pro4d.prophunt.misc;

import com.pro4d.prophunt.managers.PHuntGameManager;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class PHuntMap {
    
    private final Vector cornerOne;
    private final Vector cornerTwo;
    private final String name;
    private final World world;

    public PHuntMap(String name, World world, Vector cornerOne, Vector cornerTwo) {
        this.cornerOne = cornerOne;
        this.cornerTwo = cornerTwo;
        this.name = name;
        this.world = world;
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

}
