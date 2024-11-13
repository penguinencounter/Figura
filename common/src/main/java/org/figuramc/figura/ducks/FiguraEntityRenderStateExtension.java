package org.figuramc.figura.ducks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.UUID;

public interface FiguraEntityRenderStateExtension {
    Entity figura$getEntity();
    void figura$setEntity(Entity entity);
    float figura$getTickDelta();
    void figura$setTickDelta(float tickDelta);
}
