package org.figuramc.figura.backend2;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.figuramc.figura.neoforge.FiguraModClientNeoForge;
import org.figuramc.figura.neoforge.FiguraModServerForge;
import org.figuramc.figura.server.FSBCodec;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.Packets;
import org.figuramc.figura.server.packets.Side;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

@EventBusSubscriber(modid = FiguraModServer.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ForgeNetworking {
    public static void init() {}

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(FiguraModServer.MOD_ID);
        final IPayloadHandler<PayloadWrapper<?>> currentHandler = getCurrentHandler();
        Packets.forEachPacket(((id, desc) -> {
            ResourceLocation resLoc = new ResourceLocation(id.namespace(), id.path());
            final CustomPacketPayload.Type<PayloadWrapper<?>> type = new CustomPacketPayload.Type<>(resLoc);
            registrar.playBidirectional(type, new FSBCodec(desc.constructor()), currentHandler);
        }));
    }

    public static IPayloadHandler<PayloadWrapper<?>> getCurrentHandler() {
        return currentSide() == Side.CLIENT ? FiguraModClientNeoForge::handlePayload : FiguraModServerForge::handlePayload;
    }

    public static Side currentSide() {
        if (FMLEnvironment.dist == Dist.CLIENT) return Side.CLIENT;
        else return Side.SERVER;
    }
}
