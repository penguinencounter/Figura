package org.figuramc.figura.mixin.render.layers.items;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
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

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin<S extends LivingEntityRenderState, M extends EntityModel<S> & ArmedModel> extends RenderLayer<S, M> {

    @Shadow @Final private ItemRenderer itemRenderer;

    public ItemInHandLayerMixin(RenderLayerParent<S, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    protected void renderArmWithItem(S state, @Nullable BakedModel model, ItemStack itemStack, ItemDisplayContext itemDisplayContext, HumanoidArm humanoidArm, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci) {
        if (itemStack.isEmpty())
            return;

        boolean left = humanoidArm == HumanoidArm.LEFT;

        Avatar avatar = AvatarManager.getAvatar(state);
        if (!RenderUtils.renderArmItem(avatar, left, ci))
            return;

        // pivot part
        if (avatar.pivotPartRender(left ? ParentType.LeftItemPivot : ParentType.RightItemPivot, stack -> {
            final float s = 16f;
            stack.scale(s, s, s);
            stack.mulPose(Axis.XP.rotationDegrees(-90f));
            boolean bl = humanoidArm == HumanoidArm.LEFT;
            this.itemRenderer.render(itemStack, itemDisplayContext, bl, stack, multiBufferSource, i, OverlayTexture.NO_OVERLAY, model);
        })) {
            ci.cancel();
        }
    }
}
