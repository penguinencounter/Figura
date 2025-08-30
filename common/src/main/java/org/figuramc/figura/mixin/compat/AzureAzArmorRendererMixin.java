package org.figuramc.figura.mixin.compat;

import mod.azure.azurelibarmor.rewrite.render.AzProvider;
import mod.azure.azurelibarmor.rewrite.render.armor.AzArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.AzureAzArmorAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = AzArmorRenderer.class, remap = false)
public abstract class AzureAzArmorRendererMixin implements AzureAzArmorAccessor {
    @Unique
    private Avatar figura$avatar;

    @Inject(method = "prepForRender", at = @At(value = "HEAD"))
    private void figura$prepAvatar(Entity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> baseModel, CallbackInfo ci){
        if (entity != null)
            figura$avatar = AvatarManager.getAvatar(entity);
        else {
            figura$avatar = null;
        }
    }

    @Override
    @Unique
    public Avatar figura$getAvatar() {
        return figura$avatar;
    }

    @Override
    @Accessor("provider")
    public abstract AzProvider<ItemStack> figura$getProvider();
}