package org.figuramc.figura.neoforge;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;

import java.util.UUID;

public class FiguraServerForge extends FiguraModServer {
    @Override
    protected void sendPacketInternal(UUID receiver, Packet packet) {
        ServerPlayer player = getServer().getPlayerList().getPlayer(receiver);
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new PayloadWrapper(packet));
    }
}
