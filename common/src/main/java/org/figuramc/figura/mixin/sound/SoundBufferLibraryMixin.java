package org.figuramc.figura.mixin.sound;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.sound.OpusAudioStream;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;

@Mixin(SoundBufferLibrary.class)
public class SoundBufferLibraryMixin {

    @ModifyExpressionValue(
            method = "method_19747",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ResourceProvider;open(Lnet/minecraft/resources/ResourceLocation;)Ljava/io/InputStream;"
            )
    )
    private InputStream checkHeader$static(InputStream stream, @Share("opus") LocalBooleanRef opus) throws IOException {
        return figura$checkHeader(stream, opus);
    }

    @Unique
    @NotNull
    private InputStream figura$checkHeader(InputStream stream, @Share("opus") LocalBooleanRef opus) throws IOException {
        byte[] buffer = new byte[8];
        InputStream restored = OpusAudioStream.extractHeader(buffer, stream);
        opus.set(new String(buffer).equals("OpusHead"));
        return restored;
    }

    @WrapOperation(
            method = "method_19747",
            at = @At(
                    value = "NEW",
                    target = "(Ljava/io/InputStream;)Lnet/minecraft/client/sounds/JOrbisAudioStream;"
            )
    )
    private JOrbisAudioStream skipCall(InputStream stream, Operation<JOrbisAudioStream> original, @Share("opus") LocalBooleanRef opus) {
        return opus.get() ? null : original.call(stream);
    }

    @ModifyVariable(
            method = "method_19747",
            at = @At(
                    value = "STORE",
                    ordinal = 0
            )
    )
    private FiniteAudioStream replace(FiniteAudioStream value, @Local InputStream stream, @Share("opus") LocalBooleanRef opus) throws IOException {
        return opus.get() ? new OpusAudioStream(stream) : value;
    }

    @ModifyExpressionValue(
            method = "method_19745",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ResourceProvider;open(Lnet/minecraft/resources/ResourceLocation;)Ljava/io/InputStream;"
            )
    )
    private InputStream checkHeader$streamed(InputStream stream, @Share("opus") LocalBooleanRef opus) throws IOException {
        return figura$checkHeader(stream, opus);
    }

    @Inject(
            method = "method_19745",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ResourceProvider;open(Lnet/minecraft/resources/ResourceLocation;)Ljava/io/InputStream;",
                    shift = At.Shift.BY,
                    by = 2
            ),
            cancellable = true
    )
    private void replace(
            ResourceLocation identifier,
            boolean bl,
            CallbackInfoReturnable<AudioStream> cir,
            @Local InputStream stream,
            @Local(argsOnly = true) boolean repeatInstantly,
            @Share("opus") LocalBooleanRef opus
    ) throws IOException {
        if (opus.get()) {
            cir.setReturnValue(repeatInstantly ? new LoopingAudioStream(OpusAudioStream::new, stream) : new OpusAudioStream(stream));
        }
    }
}