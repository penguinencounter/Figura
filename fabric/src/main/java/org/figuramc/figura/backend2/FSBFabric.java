package org.figuramc.figura.backend2;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

public class FSBFabric extends FSB {
    @Override
    public void sendPacket(Packet packet) {
        Identifier id = packet.getId();
        ResourceLocation resLoc = new ResourceLocation(id.namespace(), id.path());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(new FriendlyByteBufWrapper(buf));
        ClientPlayNetworking.send(resLoc, buf);
    }
}
