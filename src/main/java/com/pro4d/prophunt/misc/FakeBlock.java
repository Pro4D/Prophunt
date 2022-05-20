package com.pro4d.prophunt.misc;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.UUID;

public class FakeBlock {

    private final Location loc;
    private final UUID disguised;
    private final Material material;

    public FakeBlock(UUID owner, Material mat, Location loc) {
        this.loc = loc;
        this.material = mat;
        this.disguised = owner;
    }

    public Location getLoc() {
        return loc;
    }

    public UUID getDisguised() {
        return disguised;
    }

    public Material getMaterial() {
        return material;
    }
}
