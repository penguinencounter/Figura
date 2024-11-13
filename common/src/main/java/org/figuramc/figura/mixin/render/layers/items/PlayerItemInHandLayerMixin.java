package org.figuramc.figura.mixin.render.layers.items;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.utils.RenderUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class only exists because of spyglass jank.
 * Has literally the exact same code as ItemInHandLayerMixin, just for the spyglass specifically.
 * For now, at least. Once spyglass category part exists, it may be different.
 * @param <S>
 * @param <M>
 */
@Mixin(PlayerItemInHandLayer.class)
public abstract class PlayerItemInHandLayerMixin <S extends PlayerRenderState, M extends EntityModel<S> & ArmedModel & HeadedModel> extends ItemInHandLayer<S, M> {

    @Shadow @Final private ItemRenderer itemRenderer;

    public PlayerItemInHandLayerMixin(RenderLayerParent<S, M> renderLayerParent, ItemRenderer itemRenderer) {
        super(renderLayerParent, itemRenderer);
    }

    @Unique
    S figura$renderState;

    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    void captureState(S playerRenderState, @Nullable BakedModel model, ItemStack stack, ItemDisplayContext itemDisplayContext, HumanoidArm humanoidArm, PoseStack matrices, MultiBufferSource vertexConsumers, int i, CallbackInfo ci) {
        this.figura$renderState = playerRenderState;
    }

    @Inject(method = "renderArmWithSpyglass", at = @At("HEAD"), cancellable = true)
    private void adjustSpyglassVisibility(BakedModel model, ItemStack itemStack, HumanoidArm humanoidArm, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, CallbackInfo ci) {
        if (itemStack.isEmpty())
            return;

        boolean left = humanoidArm == HumanoidArm.LEFT;

        Avatar avatar = AvatarManager.getAvatar(figura$renderState);
        if (!RenderUtils.renderArmItem(avatar, left, ci))
            return;

        // pivot part
        if (avatar.pivotPartRender(left ? ParentType.LeftSpyglassPivot : ParentType.RightSpyglassPivot, stack -> {
            // spyglass code is weird - might need a fix, however it will break with non-humanoid avatars
            float s = 10f;
            stack.scale(s, s, s);
            stack.translate(0, 0, 7 / 16f);
            this.itemRenderer.render(itemStack, ItemDisplayContext.HEAD, false, stack, multiBufferSource, light, OverlayTexture.NO_OVERLAY, model);
        })) {
            ci.cancel();
        }
    }
}
