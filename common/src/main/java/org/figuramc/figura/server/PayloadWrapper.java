package org.figuramc.figura.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

import java.util.function.Function;

public class PayloadWrapper implements CustomPacketPayload {
    private final Packet source;
    public static final Function<ResourceLocation,Type<PayloadWrapper>> TYPE = id -> CustomPacketPayload.createType(id.toString());

    public PayloadWrapper(Packet source) {
        this.source = source;
    }

    public void write(FriendlyByteBuf buf) {
        source.write(new FriendlyByteBufWrapper(buf));
    }

    public Packet source() {
        return source;
    }

    public ResourceLocation id() {
        var id = source.getId();
        return new ResourceLocation(id.namespace(), id.path());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.apply(this.id());
    }
}
