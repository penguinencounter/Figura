package org.figuramc.figura.backend2;

import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;

public class FSBForge extends FSB {
    @Override
    public void sendPacket(Packet packet) {
        PacketDistributor.SERVER.noArg().send(new ServerboundCustomPayloadPacket(new PayloadWrapper(packet)));
    }
}
