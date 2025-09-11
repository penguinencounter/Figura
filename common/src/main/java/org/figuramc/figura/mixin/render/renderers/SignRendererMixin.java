package org.figuramc.figura.mixin.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.font.Emojis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignRenderer.class)
public class SignRendererMixin {

    @Unique private SignBlockEntity figura$signEntity;

    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/SignBlockEntity;getRenderMessages(ZLjava/util/function/Function;)[Lnet/minecraft/util/FormattedCharSequence;", shift = At.Shift.BEFORE))
    private void captureSignText(SignBlockEntity sign, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, int j, CallbackInfo ci) {
        figura$signEntity = sign;
    }

    // method_32159 corresponds to fabric intermediary, lambda$renderSignText$2 is the unmapped OF name, m_244733_ is the SRG name for Forge
    @ModifyArg(method = {"m_hnarhlun", "method_32159", "lambda$render$2", "m_173652_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;", remap = true), remap = false)
    private FormattedText modifyText(FormattedText charSequence) {
        if (!(Configs.EMOJIS.value > 0 && charSequence instanceof Component text)) return charSequence;

        MutableComponent test = MutableComponent.create(text.getContents());
        if (figura$signEntity.getColor() == DyeColor.BLACK) {
            test = test.withStyle(Style.EMPTY);
        } else {
            test = test.withStyle(Style.EMPTY.withColor(figura$signEntity.getColor().getTextColor()));
        }


        return Emojis.applyEmojis(test);
    }
}
