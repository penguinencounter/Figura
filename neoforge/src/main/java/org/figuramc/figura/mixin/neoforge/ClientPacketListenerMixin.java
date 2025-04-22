package org.figuramc.figura.mixin.neoforge;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.s2c.Handlers;
import org.figuramc.figura.server.packets.handlers.s2c.S2CPacketHandler;
import org.figuramc.figura.server.utils.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
// TODO REMOVE THIS IN 1.20.4
public class ClientPacketListenerMixin {
    @Inject(method = "handleUnknownCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onPayload(ClientboundCustomPayloadPacket arg, CustomPacketPayload arg2, CallbackInfo ci) {
        if (arg2 instanceof PayloadWrapper wrapper) {
            Packet source = wrapper.source();
            Identifier id = source.getId();
            S2CPacketHandler<Packet> handler = Handlers.getHandler(id);
            if (handler != null) {
                handler.handle(source);
                ci.cancel();
            }
        }
    }
}
