package org.figuramc.figura.mixin.compat;

import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorModelRenderer;
import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorRendererPipeline;
import org.figuramc.figura.ducks.AzureAzArmorModelRendererAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(value = AzArmorModelRenderer.class, remap = false)
public abstract class AzureAzArmorModelRendererMixin implements AzureAzArmorModelRendererAccessor {
    @Override
    @Accessor("armorRendererPipeline")
    public abstract AzArmorRendererPipeline figura$getArmorRendererPipeline();
}
