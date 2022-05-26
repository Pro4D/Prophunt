package com.pro4d.prophunt.misc;

import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public class FakeEntity {

    private final LivingEntity entity;
    private final UUID disguised;

    public FakeEntity(UUID uuid, LivingEntity entity) {
        this.entity = entity;
        this.disguised = uuid;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public UUID getDisguised() {
        return disguised;
    }
}
