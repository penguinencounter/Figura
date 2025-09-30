package org.figuramc.figura.mixin.neoforge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.Packets;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerboundCustomPayloadPacket.class)
// TODO REMOVE THIS IN 1.20.4
public class ServerboundCustomPayloadMixin {
    @Inject(method = "readPayload", at = @At("HEAD"), cancellable = true)
    private static void onRead(ResourceLocation id, FriendlyByteBuf buf, CallbackInfoReturnable<CustomPacketPayload> cir) {
        Identifier ident = new Identifier(id.getNamespace(), id.getPath());
        Packets.PacketDescriptor<?> descriptor = Packets.getPacketDescriptor(ident);
        if (descriptor != null) {
            Packet p = descriptor.constructor().read(new FriendlyByteBufWrapper(buf));
            cir.setReturnValue(new PayloadWrapper(p));
            cir.cancel();
        }
    }
}
