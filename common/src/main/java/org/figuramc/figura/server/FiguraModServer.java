package org.figuramc.figura.server;

import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;


import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.UUID;

public abstract class FiguraModServer extends FiguraServer {
    public static final String MOD_ID = "figura";
    public static final Logger LOGGER = LogManager.getLogger("Figura");
    private MinecraftServer server;

    @Override
    public Path getFiguraFolder() {
        return FileSystems.getDefault().getPath("fsb");
    }

    @Override
    public void logInfo(String text) {
        LOGGER.info(text);
    }

    @Override
    public void logError(String text) {
        LOGGER.error(text);
    }

    @Override
    public void logError(String text, Throwable err) {
        LOGGER.error(text, err);
    }

    @Override
    public void logDebug(String text) {
        LOGGER.debug(text);
    }

    public static FiguraModServer getInstance() {
        return (FiguraModServer) INSTANCE;
    }

    @Override
    public void sendMessage(UUID receiver, JsonObject component) {
        ServerPlayer player = getServer().getPlayerList().getPlayer(receiver);
        if (player != null) player.sendMessage(Component.Serializer.fromJson(component), Util.NIL_UUID);
    }

    protected MinecraftServer getServer() {
        return server;
    }

    @Override
    public void close() {
        server = null;
        super.close();
    }

    public final void finishInitialization(MinecraftServer server) {
        if (this.server != null) throw new IllegalStateException("Server already initialized");
        this.server = server;
    }
}
