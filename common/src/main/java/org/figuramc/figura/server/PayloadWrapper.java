package org.figuramc.figura.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

public class PayloadWrapper<P extends Packet> implements CustomPacketPayload {
    private final P source;

    public PayloadWrapper(P source) {
        this.source = source;
    }


    public void write(FriendlyByteBuf buf) {
        source.write(new FriendlyByteBufWrapper(buf));
    }

    public P source() {
        return source;
    }

    public ResourceLocation id() {
        var id = source.getId();
        return new ResourceLocation(id.namespace(), id.path());
    }

    @Override
    public Type<PayloadWrapper<P>> type() {
        return new Type<>(id());
    }
}
