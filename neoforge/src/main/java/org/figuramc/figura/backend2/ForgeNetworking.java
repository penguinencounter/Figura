package org.figuramc.figura.backend2;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.IPlayPayloadHandler;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import org.figuramc.figura.neoforge.FiguraModClientNeoForge;
import org.figuramc.figura.neoforge.FiguraModServerForge;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.Packets;
import org.figuramc.figura.server.packets.Side;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

@Mod.EventBusSubscriber(modid = FiguraModServer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeNetworking {
    public static void init() {}

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlerEvent event) {
        final IPayloadRegistrar registrar = event.registrar(FiguraModServer.MOD_ID);
        final IPlayPayloadHandler<PayloadWrapper> currentHandler = getCurrentHandler();
        Packets.forEachPacket(((id, desc) -> {
            ResourceLocation resLoc = new ResourceLocation(id.namespace(), id.path());
            registrar.play(resLoc, new PayloadWrapperInitializer<>(desc.constructor()), currentHandler);
        }));
    }

    public static IPlayPayloadHandler<PayloadWrapper> getCurrentHandler() {
        return currentSide() == Side.CLIENT ? FiguraModClientNeoForge::handlePayload : FiguraModServerForge::handlePayload;
    }

    public static Side currentSide() {
        if (FMLEnvironment.dist == Dist.CLIENT) return Side.CLIENT;
        else return Side.SERVER;
    }

    public static class PayloadWrapperInitializer<P extends Packet> implements FriendlyByteBuf.Reader<PayloadWrapper> {
        private final Packet.Deserializer<P> deserializer;

        public PayloadWrapperInitializer(Packet.Deserializer<P> deserializer) {
            this.deserializer = deserializer;
        }

        @Override
        public PayloadWrapper apply(FriendlyByteBuf buf) {
            return new PayloadWrapper(deserializer.read(new FriendlyByteBufWrapper(buf)));
        }
    }
}
