package org.figuramc.figura.ducks;

import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorModel;
import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorRenderer;
import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorRendererPipelineContext;
import org.figuramc.figura.avatar.Avatar;
import org.joml.Matrix4f;

public interface AzureAzArmorPipelineAccessor {
    void figura$setEntityRenderTranslations(Matrix4f matrix4f);
    void figura$setModelRenderTranslations(Matrix4f matrix4f);

    void figura$scaleModelForRender(AzArmorRendererPipelineContext context, float widthScale, float heightScale, boolean isReRender);
}
