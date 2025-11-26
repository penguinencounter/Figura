package org.figuramc.figura.sound;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.shorts.ShortConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class OutputConcat implements ShortConsumer {
    private final List<ByteBuffer> buffers = Lists.newArrayList();
    private final int size;
    private int currentBufferSize = 0;
    private ByteBuffer buffer;

    public OutputConcat(int size) {
        this.size = (size + 1) & ~1;
        this.buffer = BufferUtils.createByteBuffer(this.size);
    }

    @Override
    public void accept(short value) {
        if (buffer.remaining() == 0) {
            buffer.flip();
            buffers.add(buffer);
            buffer = BufferUtils.createByteBuffer(this.size);
        }

        buffer.putShort(value);
        currentBufferSize += 2;
    }

    public void accept(short[] values) {
        for (short value : values) {
            accept(value);
        }
    }

    public ByteBuffer getBuffer() {
        buffer.flip();
        if (buffers.isEmpty()) {
            return buffer;
        } else {
            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(currentBufferSize);
            buffers.forEach(byteBuffer::put);
            byteBuffer.put(buffer);
            byteBuffer.flip();
            return byteBuffer;
        }
    }

    public int getCurrentBufferSize() {
        return currentBufferSize;
    }
}
