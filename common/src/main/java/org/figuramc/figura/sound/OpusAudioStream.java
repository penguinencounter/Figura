package org.figuramc.figura.sound;

import net.minecraft.client.sounds.FiniteAudioStream;
import org.chenliang.oggus.opus.AudioDataPacket;
import org.chenliang.oggus.opus.IdHeader;
import org.chenliang.oggus.opus.OggOpusStream;
import org.chenliang.oggus.opus.OpusPacket;
import org.concentus.CodecHelpers;
import org.concentus.OpusDecoder;
import org.concentus.OpusException;
import org.concentus.OpusPacketInfo;
import org.figuramc.figura.FiguraMod;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.util.*;

public class OpusAudioStream implements FiniteAudioStream {
    private final OggOpusStream opusStream;
    private final int sampleRate;
    private final int channels;
    private final AudioFormat format;
    private final OpusDecoder decoder;
    private final Queue<OpusPacket> packetQueue = new LinkedList<>();
    private final InputStream in;
    private boolean endOfStream = false;

    public OpusAudioStream(InputStream inputStream) throws IOException {
        this.in = inputStream;
        this.opusStream = OggOpusStream.from(inputStream);
        IdHeader idHeader = opusStream.getIdHeader();
        this.sampleRate = (int) idHeader.getInputSampleRate();
        this.channels = idHeader.getChannelCount();
        this.format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                (float) sampleRate,
                16,
                channels,
                channels * 2,
                (float) sampleRate,
                false
        );
        try {
            this.decoder = new OpusDecoder(sampleRate, channels);
        } catch (OpusException e) {
            throw new IOException("Failed to create Opus decoder", e);
        }
    }

    private OpusPacket readPacket() throws IOException {
        if (!packetQueue.isEmpty()) {
            return packetQueue.poll();
        }

        if (endOfStream) {
            return null;
        }

        AudioDataPacket audioPacket = opusStream.readAudioPacket();
        if (audioPacket == null) {
            endOfStream = true;
            return null;
        }

        List<OpusPacket> packets = audioPacket.getOpusPackets();
        packetQueue.addAll(packets);

        return packetQueue.isEmpty() ? null : packetQueue.poll();
    }

    private List<OpusPacket> readPackets(int maxPackets) throws IOException {
        List<OpusPacket> result = new ArrayList<>(maxPackets);
        for (int i = 0; i < maxPackets; i++) {
            OpusPacket packet = readPacket();
            if (packet == null) {
                break;
            }
            result.add(packet);
        }
        return result;
    }

    private short[] decodeNextBatch(int maxPackets) throws IOException, OpusException {
        List<OpusPacket> packets = readPackets(maxPackets);
        if (packets.isEmpty()) {
            return null;
        }

        byte[] firstPacket = packets.get(0).dumpToStandardFormat();
        int samplesPerFrame = OpusPacketInfo.getNumSamplesPerFrame(firstPacket, 0, sampleRate);
        int totalSamples = samplesPerFrame * packets.size() * channels;

        short[] decoded = new short[totalSamples];
        int sampleOffset = 0;

        for (OpusPacket packet : packets) {
            byte[] encodedData = packet.dumpToStandardFormat();
            int code = decoder.decode(encodedData, 0, encodedData.length, decoded, sampleOffset, samplesPerFrame, false);

            if (code < 0) {
                FiguraMod.debug("Opus decoding error: " + CodecHelpers.opus_strerror(code));
                continue;
            }

            sampleOffset += code * channels;
        }

        if (sampleOffset < totalSamples) {
            return Arrays.copyOf(decoded, sampleOffset);
        }

        return decoded;
    }

    @Override
    public ByteBuffer read(int size) {
        OutputConcat output = new OutputConcat(16384);
        short[] decoded;
        try {
            decoded = decodeNextBatch(size);
        } catch (IOException | OpusException e) {
            e.printStackTrace();
            return null;
        }

        if (decoded != null && decoded.length > 0) {
            output.accept(decoded);
        }

        return output.getBuffer();
    }

    @Override
    public void close() throws IOException {
        this.in.close();
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public ByteBuffer readAll() {
        OutputConcat output = new OutputConcat(16384);
        final int BATCH_SIZE = 256;

        while (true) {
            short[] decoded;
            try {
                decoded = decodeNextBatch(BATCH_SIZE);
            } catch (IOException | OpusException e) {
                e.printStackTrace();
                break;
            }

            if (decoded == null || decoded.length == 0) {
                break;
            } else {
                output.accept(decoded);
            }
        }

        return output.getBuffer();
    }
    // not every InputStream supports mark() so yea
    public static InputStream extractHeader(byte[] buf, InputStream stream) throws IOException {
        int skipAmount = 0x1C;
        int totalPeek = skipAmount + buf.length;

        byte[] peekBuffer = new byte[totalPeek];
        int bytesRead = stream.read(peekBuffer);

        if (bytesRead < totalPeek) {
            return stream;
        }

        System.arraycopy(peekBuffer, skipAmount, buf, 0, buf.length);

        return new SequenceInputStream(
                new ByteArrayInputStream(peekBuffer, 0, bytesRead),
                stream
        );
    }
    // but some polite ones do <3
    public static boolean hasOpusHeader(ByteArrayInputStream stream) {
        byte[] buf = new byte[8];
        stream.mark(0x1C + buf.length);
        try {
            if (stream.skip(0x1C) < 0x1C || stream.read(buf) < 8) {
                return false;
            }
            return new String(buf).equals("OpusHead");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            stream.reset();
        }
    }
}

