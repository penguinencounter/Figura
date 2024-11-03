package org.figuramc.figura.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.EventNetworkChannel;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.c2s.C2SPacketHandler;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = FiguraModServer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.DEDICATED_SERVER)
public class FiguraModServerForge {
    private static FiguraServerForge fsbInstance;

    static void initServer() {
        fsbInstance = new FiguraServerForge();
        fsbInstance.init();
        NeoForge.EVENT_BUS.addListener(FiguraModServerForge::onInitializeServer);
        NeoForge.EVENT_BUS.addListener(FiguraModServerForge::onTick);
        NeoForge.EVENT_BUS.addListener(FiguraModServerForge::onServerStop);
    }

    public static void registerPacketListener(Identifier id, EventNetworkChannel channel) {
        var handler = fsbInstance.getPacketHandler(id);
        if (handler != null) channel.addListener(new ForgeNetworkListener<>(id, handler));
    }

    public static void onInitializeServer(ServerStartedEvent event) {
        fsbInstance.finishInitialization(event.getServer());
    }

    public static void onServerStop(ServerStoppingEvent event) {
        fsbInstance.close();
    }

    public static void onTick(TickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.type == TickEvent.Type.SERVER) {
            if (fsbInstance != null) fsbInstance.tick();
        }
    }

    private static final class ForgeNetworkListener<P extends Packet> implements Consumer<NetworkEvent> {
        private final Identifier id;
        private final C2SPacketHandler<P> handler;

        private ForgeNetworkListener(Identifier id, C2SPacketHandler<P> handler) {
            this.id = id;
            this.handler = handler;
        }

        @Override
        public void accept(NetworkEvent event) {
            if (event.getPayload() == null) return;
            var ctx = event.getSource();
            if (ctx.getDirection().getReceptionSide() == LogicalSide.SERVER) {
                try {
                    P packet = handler.serialize(new FriendlyByteBufWrapper(event.getPayload()));
                    ServerPlayer sender = ctx.getSender();
                    handler.handle(sender.getUUID(), packet);
                    ctx.setPacketHandled(true);
                }
                catch (Exception e) {
                    fsbInstance.logError("Failed to handle packet %s".formatted(id), e);
                }
            }
        }
    }
}
