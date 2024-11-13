package org.figuramc.figura.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.RenderUtils;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Final private RenderBuffers renderBuffers;
    @Shadow @Final private Minecraft minecraft;

    @ModifyArg(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
    private Entity renderLevelRenderEntity(Entity entity) {
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (avatar != null)
            avatar.renderMode = EntityRenderMode.RENDER;
        return entity;
    }

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, PoseStack matrices, MultiBufferSource bufferSource, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (avatar == null)
            return;

        if (bufferSource instanceof OutlineBufferSource outline && RenderUtils.vanillaModelAndScript(avatar) && avatar.luaRuntime.renderer.outlineColor != null) {
            int i = ColorUtils.rgbToInt(avatar.luaRuntime.renderer.outlineColor);
            outline.setColor(
                    i >> 16 & 0xFF,
                    i >> 8 & 0xFF,
                    i & 0xFF,
                    0xFF // does nothing :(
            );
        }

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);
        FiguraMod.pushProfiler(avatar);
        FiguraMod.pushProfiler("worldRender");

        avatar.worldRender(entity, cameraX, cameraY, cameraZ, matrices, bufferSource, entityRenderDispatcher.getPackedLightCoords(entity, tickDelta), tickDelta, EntityRenderMode.WORLD);

        FiguraMod.popProfiler(3);
    }

    // TODO: Neo does not boot, complains method must be static but it won't compile if it is, the hell?
    // method_62214 for Fabric, lambda$addMainPass$2 for Neo and lambda$addMainPass$1 for Lex
    @Inject(method = {"method_62214", "lambda$addMainPass$2", "lambda$addMainPass$1"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 0))
    private void renderLevelFirstPerson(FogParameters fogParameters, DeltaTracker deltaTracker, Camera camera, ProfilerFiller profiler, Matrix4f matrix4f, Matrix4f matrix4f2, ResourceHandle<RenderTarget> resourceHandle, ResourceHandle<RenderTarget> resourceHandle2, ResourceHandle<RenderTarget> resourceHandle3, ResourceHandle<RenderTarget> resourceHandle4, boolean bl, Frustum frustum, ResourceHandle<RenderTarget> resourceHandle5, CallbackInfo ci, @Local PoseStack stack) {
        if (camera.isDetached())
            return;

        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
        Entity e = camera.getEntity();
        Avatar avatar = AvatarManager.getAvatar(e);

        if (avatar == null || !(e instanceof LivingEntity livingEntity))
            return;

        EntityRenderer<LivingEntity, LivingEntityRenderState> entityRenderer = (EntityRenderer<LivingEntity, LivingEntityRenderState>) this.entityRenderDispatcher.getRenderer(livingEntity);

        LivingEntityRenderState state = entityRenderer.createRenderState(livingEntity, deltaTracker.getGameTimeDeltaPartialTick(Minecraft.getInstance().level.tickRateManager().isEntityFrozen(e)));
        // first person world parts
        MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
        avatar.firstPersonWorldRender(e, bufferSource, stack, camera, tickDelta);

        // first person matrices
        if (!Configs.FIRST_PERSON_MATRICES.value)
            return;

        Avatar.firstPerson = true;

        int size = ((PoseStackAccessor)stack).getPoseStack().size();
        stack.pushPose();

        Vec3 offset = entityRenderer.getRenderOffset(state);
        Vec3 cam = camera.getPosition();

        stack.translate(
                Mth.lerp(tickDelta, livingEntity.xOld, livingEntity.getX()) - cam.x() + offset.x(),
                Mth.lerp(tickDelta, livingEntity.yOld, livingEntity.getY()) - cam.y() + offset.y(),
                Mth.lerp(tickDelta, livingEntity.zOld, livingEntity.getZ()) - cam.z() + offset.z()
        );


        entityRenderer.render(state, stack, bufferSource, LightTexture.FULL_BRIGHT);

        do {
            stack.popPose();
        } while(((PoseStackAccessor)stack).getPoseStack().size() > size);

        Avatar.firstPerson = false;
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRenderLevel(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        AvatarManager.executeAll("worldRender", avatar -> avatar.render(deltaTracker.getGameTimeDeltaPartialTick(false)));
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void afterRenderLevel(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        AvatarManager.executeAll("postWorldRender", avatar -> avatar.postWorldRenderEvent(deltaTracker.getGameTimeDeltaPartialTick(false)));
    }

    @ModifyArg(method = "renderHitOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShapeRenderer;renderShape(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/phys/shapes/VoxelShape;DDDI)V"), index = 6)
    private int renderHitOutline(int colorInt) {
        Avatar avatar = AvatarManager.getAvatar(this.minecraft.getCameraEntity());
        FiguraVec4 color;

        if (avatar == null || avatar.luaRuntime == null || (color = avatar.luaRuntime.renderer.blockOutlineColor) == null)
            return colorInt;

        return ARGB.colorFromFloat((float) color.x, (float) color.y, (float) color.z, (float) color.w);
    }

    @Inject(method =  {"method_62214", "lambda$addMainPass$2", "lambda$addMainPass$1"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"))
    public void applyFiguraNormals(FogParameters fogParameters, DeltaTracker tracker, Camera camera, ProfilerFiller profiler, Matrix4f matrix4f, Matrix4f matrix4f2, ResourceHandle resourceHandle, ResourceHandle resourceHandle2, ResourceHandle resourceHandle3, ResourceHandle resourceHandle4, boolean bl, Frustum frustum, ResourceHandle resourceHandle5, CallbackInfo ci, @Local PoseStack poseStack) {
        Avatar avatar = AvatarManager.getAvatar(this.minecraft.getCameraEntity() == null ? this.minecraft.player : this.minecraft.getCameraEntity());
        if (!RenderUtils.vanillaModelAndScript(avatar)) return;

        FiguraMat3 normal = avatar.luaRuntime.renderer.cameraNormal;
        if (normal != null)
            poseStack.last().normal().set(normal.toMatrix3f());
    }
}