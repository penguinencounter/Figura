package org.figuramc.figura.mixin.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.gui.ActionWheel;
import org.figuramc.figura.gui.FiguraGui;
import org.figuramc.figura.lua.api.RendererAPI;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeIngameGui.class)
public class ForgeIngameGuiMixin {


    @Inject(at = @At("HEAD"), method = "render", cancellable = true)
    private void onRender(PoseStack poseStack, float tickDelta, CallbackInfo ci) {
        FiguraGui.onRender(poseStack, tickDelta, ci);
    }

    @Inject(at = @At("RETURN"), method = "render")
    private void afterRender(PoseStack poseStack, float tickDelta, CallbackInfo ci) {
        if (!AvatarManager.panic)
            FiguraGui.renderOverlays(poseStack);
    }
}
