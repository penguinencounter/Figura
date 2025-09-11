package org.figuramc.figura.backend2;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.network.PacketDistributor;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

public class FSBForge extends FSB {
    @Override
    public void sendPacket(Packet packet) {
        Identifier id = packet.getId();
        ResourceLocation resLoc = new ResourceLocation(id.namespace(), id.path());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(new FriendlyByteBufWrapper(buf));
        PacketDistributor.SERVER.noArg().send(new ServerboundCustomPayloadPacket(resLoc, buf));
    }
}
