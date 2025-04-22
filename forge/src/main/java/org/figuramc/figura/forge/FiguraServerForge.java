package org.figuramc.figura.forge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.figuramc.figura.backend2.ForgeNetworking;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import org.figuramc.figura.server.FiguraModServer;
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
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(new FriendlyByteBufWrapper(buf));
        channel.send(buf, PacketDistributor.PLAYER.with(player));
    }

    @Override
    public boolean getPermission(UUID player, String permission) {
        ServerPlayer pl = getServer().getPlayerList().getPlayer(player);
        PermissionNode<Boolean> perm = FiguraForgePermissions.getPermission(permission);
        return pl != null && perm != null && PermissionAPI.getPermission(pl, perm);
    }
}
