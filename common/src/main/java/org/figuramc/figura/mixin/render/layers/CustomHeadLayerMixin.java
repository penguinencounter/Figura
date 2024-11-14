package org.figuramc.figura.mixin.render.layers;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.FiguraEntityRenderStateExtension;
import org.figuramc.figura.ducks.SkullBlockRendererAccessor;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin<S extends LivingEntityRenderState, M extends EntityModel<S> & HeadedModel> extends RenderLayer<S, M> {

    public CustomHeadLayerMixin(RenderLayerParent<S, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Shadow @Final private Map<SkullBlock.Type, SkullModelBase> skullModels;

    @Shadow @Final private ItemRenderer itemRenderer;

    @Inject(at = @At("HEAD"), method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", cancellable = true)
    private void render(PoseStack matrices, MultiBufferSource multiBufferSource, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        ItemStack itemStack = livingEntityRenderState.headItem;
        if (itemStack.getItem() instanceof ArmorItem armorItem && armorItem.components().has(DataComponents.EQUIPPABLE) && armorItem.components().get(DataComponents.EQUIPPABLE).slot() == EquipmentSlot.HEAD)
            return;

        Avatar avatar = AvatarManager.getAvatar(livingEntityRenderState);
        if (!RenderUtils.vanillaModel(avatar))
            return;

        // script hide
        if (avatar.luaRuntime != null && !avatar.luaRuntime.vanilla_model.HELMET_ITEM.checkVisible()) {
            ci.cancel();
            return;
        }

        // pivot part
        if (itemStack.getItem() instanceof BlockItem block && block.getBlock() instanceof AbstractSkullBlock) {
            // fetch skull data
            ResolvableProfile gameProfile = null;
            if (itemStack.getComponents().has(DataComponents.PROFILE)) {
                    gameProfile = itemStack.get(DataComponents.PROFILE);
            }

            SkullBlock.Type type = ((AbstractSkullBlock) ((BlockItem) itemStack.getItem()).getBlock()).getType();
            SkullModelBase skullModelBase = this.skullModels.get(type);
            RenderType renderType = SkullBlockRenderer.getRenderType(type, gameProfile);

            // render!!
            if (avatar.pivotPartRender(ParentType.HelmetItemPivot, stack -> {
                float s = 19f;
                stack.scale(s, s, s);
                stack.translate(-0.5d, 0d, -0.5d);

                // set item context
                SkullBlockRendererAccessor.setItem(itemStack);
                SkullBlockRendererAccessor.setEntity(Minecraft.getInstance().level.getEntity(((FiguraEntityRenderStateExtension)livingEntityRenderState).figura$getEntityId()));
                SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.HEAD);
                SkullBlockRenderer.renderSkull(null, 0f, f, stack, multiBufferSource, i, skullModelBase, renderType);
            })) {
                ci.cancel();
            }
        } else if (avatar.pivotPartRender(ParentType.HelmetItemPivot, stack -> {
            float s = 10f;
            stack.translate(0d, 4d, 0d);
            stack.scale(s, s, s);
            this.itemRenderer.render(itemStack, ItemDisplayContext.HEAD, false, stack, multiBufferSource, i, OverlayTexture.NO_OVERLAY, livingEntityRenderState.headItemModel);
        })) {
            ci.cancel();
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/SkullBlockRenderer;renderSkull(Lnet/minecraft/core/Direction;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/SkullModelBase;Lnet/minecraft/client/renderer/RenderType;)V"), method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V")
    private void renderSkull(PoseStack matrices, MultiBufferSource vertexConsumers, int i, S livingEntityRenderState, float f, float g, CallbackInfo ci) {
        SkullBlockRendererAccessor.setItem(livingEntityRenderState.headItem);
        SkullBlockRendererAccessor.setEntity(Minecraft.getInstance().level.getEntity(((FiguraEntityRenderStateExtension)livingEntityRenderState).figura$getEntityId()));
        SkullBlockRendererAccessor.setRenderMode(SkullBlockRendererAccessor.SkullRenderMode.HEAD);
    }
}
