package org.figuramc.figura.fabric;

import io.netty.buffer.Unpooled;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.figuramc.figura.backend2.FabricNetworking;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.Side;
import org.figuramc.figura.server.packets.handlers.c2s.C2SPacketHandler;

import java.util.UUID;

public class FiguraServerFabric extends FiguraModServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        init();
        FabricNetworking.init(this::registerHandler, Side.SERVER);
    }

    public <P extends Packet> void registerHandler(CustomPacketPayload.Type<PayloadWrapper<P>> type) {
        C2SPacketHandler<P> handler = getPacketHandler(type.id());
        if (handler != null) {
            ServerPlayNetworking.registerGlobalReceiver(type, new FabricServerHandler<>(handler));
        }
    }

    @Override
    protected void sendPacketInternal(UUID receiver, Packet packet) {
        ServerPlayer player = getServer().getPlayerList().getPlayer(receiver);
        if (player != null) {
            ServerPlayNetworking.send(player, new PayloadWrapper<>(packet));
        }
    }

    @Override
    public boolean getPermission(UUID uuid, String permission) {
        ServerPlayer player = getServer().getPlayerList().getPlayer(uuid);
        return player != null && Permissions.check(player, permission);
    }

    private static class FabricServerHandler<P extends Packet> implements ServerPlayNetworking.PlayPayloadHandler<PayloadWrapper<P>> {

        private final C2SPacketHandler<P> parent;

        private FabricServerHandler(C2SPacketHandler<P> parent) {
            this.parent = parent;
        }

        @Override
        public void receive(PayloadWrapper<P> payload, ServerPlayNetworking.Context context) {
            P source = payload.source();
            parent.handle(context.player().getUUID(), source);
        }
    }
}
