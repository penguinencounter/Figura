package org.figuramc.figura.neoforge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.INetworkDirection;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.PlayNetworkDirection;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import org.figuramc.figura.backend2.ForgeNetworking;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.PayloadWrapper;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

import java.util.UUID;

public class FiguraServerForge extends FiguraModServer {
    @Override
    protected void sendPacketInternal(UUID receiver, Packet packet) {
        ServerPlayer player = getServer().getPlayerList().getPlayer(receiver);
        if (player == null) return;
        var id = packet.getId();
        var channel = ForgeNetworking.getChannel(id);
        if (channel == null) return;
        PacketDistributor.PLAYER.with(() -> player).send(new ClientboundCustomPayloadPacket(new PayloadWrapper(packet)));
    }

    @Override
    public boolean getPermission(UUID player, String permission) {
        ServerPlayer pl = getServer().getPlayerList().getPlayer(player);
        PermissionNode<Boolean> perm = FiguraForgePermissions.getPermission(permission);
        return pl != null && perm != null && PermissionAPI.getPermission(pl, perm);
    }
}
