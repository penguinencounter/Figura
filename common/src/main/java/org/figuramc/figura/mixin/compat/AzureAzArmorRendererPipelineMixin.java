package org.figuramc.figura.mixin.compat;

import mod.azure.azurelibarmor.rewrite.render.AzRendererConfig;
import mod.azure.azurelibarmor.rewrite.render.AzRendererPipeline;
import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorRenderer;
import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorRendererPipeline;
import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorRendererPipelineContext;
import net.minecraft.world.item.ItemStack;
import org.figuramc.figura.ducks.AzureAzArmorPipelineAccessor;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(value = AzArmorRendererPipeline.class, remap = false)
public abstract class AzureAzArmorRendererPipelineMixin extends AzRendererPipeline<ItemStack> implements AzureAzArmorPipelineAccessor {
    protected AzureAzArmorRendererPipelineMixin(AzRendererConfig<ItemStack> config) {
        super(config);
    }

    @Override
    @Accessor("entityRenderTranslations")
    public abstract void figura$setEntityRenderTranslations(Matrix4f matrix4f);

    @Override
    @Accessor("modelRenderTranslations")
    public abstract void figura$setModelRenderTranslations(Matrix4f matrix4f);

    @Override
    @Invoker("scaleModelForRender")
    public abstract void figura$scaleModelForRender(AzArmorRendererPipelineContext context,
                                           float widthScale,
                                           float heightScale,
                                           boolean isReRender);
}
