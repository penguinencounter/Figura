package org.figuramc.figura.mixin.compat;

import mod.azure.azurelibarmor.rewrite.render.AzRendererConfig;
import org.figuramc.figura.ducks.AzureAzRendererConfigAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(value = AzRendererConfig.class, remap = false)
public abstract class AzureAzRendererConfigMixin implements AzureAzRendererConfigAccessor {
    @Override
    @Accessor("scaleWidth")
    public abstract float figura$getScaleWidth();

    @Override
    @Accessor("scaleHeight")
    public abstract float figura$getScaleHeight();
}
