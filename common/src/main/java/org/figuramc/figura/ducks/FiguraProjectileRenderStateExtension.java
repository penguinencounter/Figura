package org.figuramc.figura.ducks;

import net.minecraft.world.entity.projectile.Projectile;

public interface FiguraProjectileRenderStateExtension {
    Projectile figura$getProjectile();
    void figura$setProjectile(Projectile arrow);
    float figura$getTickDelta();
    void figura$setTickDelta(float tickDelta);
}
