package com.github.zhenbin.nettytest.handler.channel;

import com.github.zhenbin.nettytest.dto.ProtocMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.nio.ByteOrder;

public class ProtocDecoder extends LengthFieldBasedFrameDecoder
{
    private static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;
    private static final int MAX_FRAME_LENGTH = 1024 * 1024;
    private static final int LENGTH_FIELD_OFFSET = 1;
    private static final int LENGTH_FIELD_LENGTH = 4;
    private static final int LENGTH_ADJUSTMENT = 0;
    private static final int INITIAL_BYTES_TO_STRIP = 0;
    private static final boolean FAIL_FAST = false;

    public ProtocDecoder() {
        super(BYTE_ORDER, MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP, FAIL_FAST);
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        in = (ByteBuf) super.decode(ctx, in);
        byte type = in.readByte();
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readBytes(bytes, 0, length);
        System.out.println(String.format("type: %d, body: %s", type, new String(bytes)));
        return new ProtocMsg(type, bytes);
    }
}