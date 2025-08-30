package org.figuramc.figura.mixin.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import mod.azure.azurelibarmor.rewrite.model.AzBakedModel;
import mod.azure.azurelibarmor.rewrite.model.AzBone;
import mod.azure.azurelibarmor.rewrite.render.AzModelRenderer;
import mod.azure.azurelibarmor.rewrite.render.AzRendererPipelineContext;
import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorModelRenderer;
import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorRenderer;
import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorRendererPipeline;
import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorRendererPipelineContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.ducks.AzureAzArmorAccessor;
import org.figuramc.figura.ducks.AzureAzArmorModelRendererAccessor;
import org.figuramc.figura.ducks.AzureAzArmorPipelineAccessor;
import org.figuramc.figura.lua.api.vanilla_model.VanillaPart;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

@Pseudo
@Mixin(value = AzModelRenderer.class, remap = false)
public abstract class AzureAzModelRendererMixin<T> {
    @Shadow
    abstract protected void renderRecursively(AzRendererPipelineContext<T> context, AzBone bone, boolean isReRender);

//    @Shadow void updateAnimatedTextureFrame(T animatable);

    @Inject(method = "render", at = @At(
            value = "INVOKE",
            target = "Lmod/azure/azurelibarmor/rewrite/render/AzRendererPipeline;updateAnimatedTextureFrame(Ljava/lang/Object;)V",
            shift = At.Shift.AFTER
    ), cancellable = true)
    private void figura$renderInject(AzRendererPipelineContext<T> context, boolean isReRender, CallbackInfo ci) {
        figura$renderPivots(context, isReRender, ci);
    }

    @Unique
    private void figura$renderPivots(AzRendererPipelineContext<T> context, boolean isReRender, CallbackInfo boundCi) {
        boolean allFailed = true;
        AzModelRenderer<?> typedThis = (AzModelRenderer<?>) (Object) this;
        if (typedThis instanceof AzArmorModelRenderer) {
            AzArmorModelRenderer typedThisToo = (AzArmorModelRenderer) typedThis;
            AzureAzArmorModelRendererAccessor sameThingButAccessor = (AzureAzArmorModelRendererAccessor) typedThisToo;
            AzArmorRendererPipeline pipeline = sameThingButAccessor.figura$getArmorRendererPipeline();
            AzureAzArmorAccessor aFourthOne = (AzureAzArmorAccessor) pipeline.renderer();

            if (aFourthOne.figura$getAvatar() == null) return;

            Avatar avatar = aFourthOne.figura$getAvatar();

            // can we even do this?
            if (avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) < 1) return;

        }
    }

    /*
    @Unique
    default void figura$renderPivots(PoseStack poseStack,
                                     GeoAnimatable geoAnimatable,
                                     BakedGeoModel bakedGeoModel,
                                     RenderType renderType,
                                     MultiBufferSource multiBufferSource,
                                     VertexConsumer vertexConsumer,
                                     boolean isReRender,
                                     float partialTick,
                                     int packedLight,
                                     int packedOverlay,
                                     int color,
                                     CallbackInfo ci) {
        boolean allFailed = true;

        // If the renderer is an armor renderer and the avatar is not null
        if (this instanceof GeoArmorRenderer && ((AzureAzArmorAccessor) this).figura$getAvatar() != null) {
            GeoArmorRenderer armorRenderer = (GeoArmorRenderer<?>) this;
            if (armorRenderer.getCurrentSlot() == null) return; // ?
            Avatar avatar = ((AzureAzArmorAccessor) armorRenderer).figura$getAvatar();

            // Check the user can edit the model
            if (avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT) < 1) return;
            GeoModel<?> model = armorRenderer.getGeoModel();

            // Render the pivot depending on the current slot
            switch (armorRenderer.getCurrentSlot()) {
                case HEAD:
                    allFailed = figura$renderPivot(
                            armorRenderer,
                            avatar,
                            ParentType.HelmetPivot,
                            geoAnimatable,
                            armorRenderer.getHeadBone(model),
                            renderType,
                            multiBufferSource,
                            vertexConsumer,
                            isReRender,
                            partialTick,
                            packedLight,
                            packedOverlay,
                            color
                    );
                    if (allFailed)
                        renderRecursively(
                                poseStack,
                                geoAnimatable,
                                armorRenderer.getHeadBone(model),
                                renderType,
                                multiBufferSource,
                                vertexConsumer,
                                isReRender,
                                partialTick,
                                packedLight,
                                packedOverlay,
                                color
                        );
                    break;
                case CHEST:
                    allFailed = figura$renderPivot(
                            armorRenderer,
                            avatar,
                            ParentType.ChestplatePivot,
                            geoAnimatable,
                            armorRenderer.getBodyBone(model),
                            renderType,
                            multiBufferSource,
                            vertexConsumer,
                            isReRender,
                            partialTick,
                            packedLight,
                            packedOverlay,
                            color
                    );
                    if (allFailed)
                        renderRecursively(
                                poseStack,
                                geoAnimatable,
                                armorRenderer.getBodyBone(model),
                                renderType,
                                multiBufferSource,
                                vertexConsumer,
                                isReRender,
                                partialTick,
                                packedLight,
                                packedOverlay,
                                color
                        );
                    allFailed = figura$renderPivot(
                            armorRenderer,
                            avatar,
                            ParentType.LeftShoulderPivot,
                            geoAnimatable,
                            armorRenderer.getLeftArmBone(model),
                            renderType,
                            multiBufferSource,
                            vertexConsumer,
                            isReRender,
                            partialTick,
                            packedLight,
                            packedOverlay,
                            color
                    );
                    if (allFailed)
                        renderRecursively(
                                poseStack,
                                geoAnimatable,
                                armorRenderer.getLeftArmBone(model),
                                renderType,
                                multiBufferSource,
                                vertexConsumer,
                                isReRender,
                                partialTick,
                                packedLight,
                                packedOverlay,
                                color
                        );
                    allFailed = figura$renderPivot(
                            armorRenderer,
                            avatar,
                            ParentType.RightShoulderPivot,
                            geoAnimatable,
                            armorRenderer.getRightArmBone(model),
                            renderType,
                            multiBufferSource,
                            vertexConsumer,
                            isReRender,
                            partialTick,
                            packedLight,
                            packedOverlay,
                            color
                    );
                    if (allFailed)
                        renderRecursively(
                                poseStack,
                                geoAnimatable,
                                armorRenderer.getRightArmBone(model),
                                renderType,
                                multiBufferSource,
                                vertexConsumer,
                                isReRender,
                                partialTick,
                                packedLight,
                                packedOverlay,
                                color
                        );
                    break;
                case LEGS:
                    allFailed = figura$renderPivot(
                            armorRenderer,
                            avatar,
                            ParentType.LeftLeggingPivot,
                            geoAnimatable,
                            armorRenderer.getLeftLegBone(model),
                            renderType,
                            multiBufferSource,
                            vertexConsumer,
                            isReRender,
                            partialTick,
                            packedLight,
                            packedOverlay,
                            color
                    );
                    if (allFailed)
                        renderRecursively(
                                poseStack,
                                geoAnimatable,
                                armorRenderer.getLeftLegBone(model),
                                renderType,
                                multiBufferSource,
                                vertexConsumer,
                                isReRender,
                                partialTick,
                                packedLight,
                                packedOverlay,
                                color
                        );
                    figura$renderPivot(
                            armorRenderer,
                            avatar,
                            ParentType.RightLeggingPivot,
                            geoAnimatable,
                            armorRenderer.getRightLegBone(model),
                            renderType,
                            multiBufferSource,
                            vertexConsumer,
                            isReRender,
                            partialTick,
                            packedLight,
                            packedOverlay,
                            color
                    );
                    if (allFailed)
                        renderRecursively(
                                poseStack,
                                geoAnimatable,
                                armorRenderer.getRightLegBone(model),
                                renderType,
                                multiBufferSource,
                                vertexConsumer,
                                isReRender,
                                partialTick,
                                packedLight,
                                packedOverlay,
                                color
                        );
                    break;
                case FEET:
                    allFailed = figura$renderPivot(
                            armorRenderer,
                            avatar,
                            ParentType.LeftBootPivot,
                            geoAnimatable,
                            armorRenderer.getLeftBootBone(model),
                            renderType,
                            multiBufferSource,
                            vertexConsumer,
                            isReRender,
                            partialTick,
                            packedLight,
                            packedOverlay,
                            color
                    );
                    if (allFailed)
                        renderRecursively(
                                poseStack,
                                geoAnimatable,
                                armorRenderer.getLeftBootBone(model),
                                renderType,
                                multiBufferSource,
                                vertexConsumer,
                                isReRender,
                                partialTick,
                                packedLight,
                                packedOverlay,
                                color
                        );
                    allFailed = figura$renderPivot(
                            armorRenderer,
                            avatar,
                            ParentType.RightBootPivot,
                            geoAnimatable,
                            armorRenderer.getRightBootBone(model),
                            renderType,
                            multiBufferSource,
                            vertexConsumer,
                            isReRender,
                            partialTick,
                            packedLight,
                            packedOverlay,
                            color
                    );
                    if (allFailed)
                        renderRecursively(
                                poseStack,
                                geoAnimatable,
                                armorRenderer.getRightBootBone(model),
                                renderType,
                                multiBufferSource,
                                vertexConsumer,
                                isReRender,
                                partialTick,
                                packedLight,
                                packedOverlay,
                                color
                        );
                    break;
                default:
                    break;
            }
            ci.cancel();
        }
    }
    */

    @Unique
    private boolean figura$renderPivot(
            AzArmorRendererPipelineContext context,
            AzArmorModelRenderer armorModelRenderer,
            AzureAzArmorModelRendererAccessor armorModelRendererAccess,
            AzureAzArmorAccessor armorRendererAccess,
            Avatar avatar,
            ParentType parentType,
            AzBone bone,
            boolean rerendered
    ) {
        if (bone == null) return true;

        int armorEditPermit = avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT);
        // it's fine if it's hidden
        VanillaPart relevantPart = RenderUtils.pivotToPart(avatar, parentType);
        if (armorEditPermit == 1 && relevantPart != null && !relevantPart.checkVisible()) return false;
        // it's not fine if there's no permissions
        if (armorEditPermit != 1) return true;

        return !avatar.pivotPartRender(
                parentType, stack -> {
                    bone.setRotX(0);
                    bone.setRotY(0);
                    bone.setRotZ(0);

                    stack.pushPose();
                    figura$prepareArmorRender(stack);
                    figura$transformBasedOnType(bone, stack, parentType);

                    AzArmorRendererPipeline pipeline = armorModelRendererAccess.figura$getArmorRendererPipeline();
                    ((AzureAzArmorPipelineAccessor) pipeline).figura$setEntityRenderTranslations(stack.last().pose());

                    stack.pushPose();
                    AzBakedModel model = context.bakedModel();
                    pipeline.scaleModelForBaby(context, rerendered);
                    pipeline.scaleModelForRender(context, rerendered);
                }
        );
    }

    // Returns true if the pivot failed to render, false if it was successful to match HumanoidArmorLayerMixin
    /*
    @Unique
    default boolean figura$renderPivot(GeoArmorRenderer armorRenderer,
                                       Avatar avatar,
                                       ParentType parentType,
                                       GeoAnimatable geoAnimatable,
                                       GeoBone geoBone,
                                       RenderType renderType,
                                       MultiBufferSource multiBufferSource,
                                       VertexConsumer vertexConsumer,
                                       boolean isReRender,
                                       float partialTick,
                                       int packedLight,
                                       int packedOverlay,
                                       int color) {
        if (geoBone == null)
            return true;

        int armorEditPermission = avatar.permissions.get(Permissions.VANILLA_MODEL_EDIT);
        // Returns successfully but skips rendering if the part is hidden
        VanillaPart part = RenderUtils.pivotToPart(avatar, parentType);
        if (armorEditPermission == 1 && part != null && !part.checkVisible())
            return false;

        // If the user has no permission disable pivots
        if (armorEditPermission != 1)
            return true;

        return !avatar.pivotPartRender(
                parentType, stack -> {
                    geoBone.setRotX(0);
                    geoBone.setRotY(0);
                    geoBone.setRotZ(0);

                    stack.pushPose();
                    figura$prepareArmorRender(stack);
                    figura$transformBasedOnType(geoBone, stack, parentType);

                    ((AzureAzArmorAccessor) armorRenderer).figura$setEntityRenderTranslations(stack.last().pose());

                    stack.pushPose();
                    BakedGeoModel model = armorRenderer.getGeoModel()
                            .getBakedModel(armorRenderer.getGeoModel().getModelResource(geoAnimatable));
                    armorRenderer.scaleModelForBaby(stack, (Item) geoAnimatable, partialTick, isReRender);
                    armorRenderer.scaleModelForRender(
                            ((AzureAzArmorAccessor) armorRenderer).figura$getScaleWidth(),
                            ((AzureAzArmorAccessor) armorRenderer).figura$getScaleHeight(),
                            stack,
                            geoAnimatable,
                            model,
                            isReRender,
                            partialTick,
                            packedLight,
                            packedOverlay
                    );

                    stack.translate(0, 24 / 16f, 0);
                    stack.scale(-1, -1, 1);

                    ((AzureAzArmorAccessor) armorRenderer).figura$setModelRenderTranslations(stack.last().pose());
                    renderRecursively(
                            stack,
                            geoAnimatable,
                            geoBone,
                            renderType,
                            multiBufferSource,
                            vertexConsumer,
                            isReRender,
                            partialTick,
                            packedLight,
                            packedOverlay,
                            color
                    );
                    stack.popPose();
                    stack.popPose();
                }
        );
    }
    */

    // Based on the values from HumanoidArmorLayerMixin
    @Unique
    private void figura$transformBasedOnType(AzBone bone, PoseStack poseStack, ParentType parentType) {
        // Arm Bones have to be moved to 0, as the vanilla hitting animation moves them, but we do too when copying the transforms, this fixes clipping issues
        if (parentType == ParentType.LeftShoulderPivot) {
            bone.setPosY(0.0f);
            bone.setPosZ(0.0f);
            bone.setPosX(0.0f);
            poseStack.translate(-6 / 16f, 0f, 0f);
        } else if (parentType == ParentType.RightShoulderPivot) {
            bone.setPosY(0.0f);
            bone.setPosZ(0.0f);
            bone.setPosX(0.0f);
            poseStack.translate(6 / 16f, 0f, 0f);
        } else if (parentType == ParentType.LeggingsPivot) {
            poseStack.translate(0, -12 / 16f, 0);
        } else if (parentType == ParentType.LeftLeggingPivot) {
            poseStack.translate(-2 / 16f, -12 / 16f, 0);
        } else if (parentType == ParentType.RightLeggingPivot) {
            poseStack.translate(2 / 16f, -12 / 16f, 0);
        } else if (parentType == ParentType.LeftBootPivot) {
            poseStack.translate(-2 / 16f, -24 / 16f, 0);
        } else if (parentType == ParentType.RightBootPivot) {
            poseStack.translate(2 / 16f, -24 / 16f, 0);
        }
    }


    @Unique
    void figura$prepareArmorRender(PoseStack stack) {
        stack.scale(16, 16, 16);
        stack.mulPose(Axis.XP.rotationDegrees(180f));
        stack.mulPose(Axis.YP.rotationDegrees(180f));
    }
}
