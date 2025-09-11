package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.server.packets.s2c.S2CPingErrorPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.utils.FiguraText;

public class S2CPingErrorPacketHandler extends ConnectedPacketHandler<S2CPingErrorPacket> {
    @Override
    protected void handlePacket(S2CPingErrorPacket packet) {
        switch (packet.error()) {
            case PING_SIZE:
                FiguraToast.sendToast(new FiguraText("backend.warning"), new FiguraText("backend.ping_size"), FiguraToast.ToastType.ERROR);
                break;
            case RATE_LIMIT:
                FiguraToast.sendToast(new FiguraText("backend.warning"), new FiguraText("backend.ping_rate"), FiguraToast.ToastType.ERROR);
                break;
            case UNKNOWN:
                break;
        }
    }

    @Override
    public S2CPingErrorPacket serialize(IFriendlyByteBuf byteBuf) {
        return new S2CPingErrorPacket(byteBuf);
    }
}
