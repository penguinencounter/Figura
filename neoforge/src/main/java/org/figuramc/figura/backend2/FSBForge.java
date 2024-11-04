package org.figuramc.figura.backend2;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.INetworkDirection;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.PlayNetworkDirection;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

public class FSBForge extends FSB {
    @Override
    public void sendPacket(Packet packet) {
        var id = packet.getId();
        var channel = ForgeNetworking.getChannel(id);
        if (channel == null) return;
        PacketDistributor.SERVER.noArg().send(new ServerboundCustomPayloadPacket(new PayloadWrapper(packet)));
    }
}
