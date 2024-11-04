package org.figuramc.figura.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.c2s.C2SPacketHandler;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = FiguraModServer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
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
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.type == TickEvent.Type.SERVER) {
            if (fsbInstance != null) fsbInstance.tick();
        }
    }

    public static void handlePayload(PayloadWrapper wrapper, PlayPayloadContext playPayloadContext) {
        Optional<Player> pl = playPayloadContext.player();
        if (pl.isPresent() && pl.get() instanceof ServerPlayer player) {
            Packet source = wrapper.source();
            C2SPacketHandler<Packet> handler = fsbInstance.getPacketHandler(source.getId());
            if (handler != null) {
                handler.handle(player.getUUID(), source);
            }
        }

    }
}
