package org.figuramc.figura.mixin.neoforge;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ServerPacketListener;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.c2s.C2SPacketHandler;
import org.figuramc.figura.server.packets.handlers.s2c.Handlers;
import org.figuramc.figura.server.packets.handlers.s2c.S2CPacketHandler;
import org.figuramc.figura.server.utils.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
// TODO REMOVE THIS IN 1.20.4
public abstract class ServerPacketListenerMixin {
    @Shadow protected abstract GameProfile playerProfile();

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onPayload(ServerboundCustomPayloadPacket par1, CallbackInfo ci) {
        if (par1.payload() instanceof PayloadWrapper wrapper) {
            Packet source = wrapper.source();
            Identifier id = source.getId();
            C2SPacketHandler<Packet> handler = FiguraServer.getInstance().getPacketHandler(id);
            if (handler != null) {
                handler.handle(playerProfile().getId(), source);
            }
        }
    }
}
