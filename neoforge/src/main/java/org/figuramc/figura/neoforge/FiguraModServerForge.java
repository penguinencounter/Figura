package org.figuramc.figura.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.c2s.C2SPacketHandler;

import java.util.Optional;

@EventBusSubscriber(modid = FiguraModServer.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public class FiguraModServerForge {
    private static FiguraServerForge fsbInstance;

    static void initServer() {
        fsbInstance = new FiguraServerForge();
        fsbInstance.init();
        NeoForge.EVENT_BUS.addListener(FiguraModServerForge::onInitializeServer);
        NeoForge.EVENT_BUS.addListener(FiguraModServerForge::onTick);
        NeoForge.EVENT_BUS.addListener(FiguraModServerForge::onServerStop);
    }

    public static void onInitializeServer(ServerStartedEvent event) {
        fsbInstance.finishInitialization(event.getServer());
    }

    public static void onServerStop(ServerStoppingEvent event) {
        fsbInstance.close();
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (fsbInstance != null) fsbInstance.tick();
    }

    public static void handlePayload(PayloadWrapper wrapper, IPayloadContext playPayloadContext) {
        Player pl = playPayloadContext.player();
        if (pl instanceof ServerPlayer player) {
            Packet source = wrapper.source();
            C2SPacketHandler<Packet> handler = fsbInstance.getPacketHandler(source.getId());
            if (handler != null) {
                handler.handle(player.getUUID(), source);
            }
        }

    }
}
