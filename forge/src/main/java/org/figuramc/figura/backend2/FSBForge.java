package org.figuramc.figura.backend2;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.PacketDistributor;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

public class FSBForge extends FSB {
    @Override
    public void sendPacket(Packet packet) {
        var id = packet.getId();
        var channel = ForgeNetworking.getChannel(id);
        if (channel == null) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(new FriendlyByteBufWrapper(buf));
        channel.send(buf, PacketDistributor.SERVER.noArg());
    }
}
