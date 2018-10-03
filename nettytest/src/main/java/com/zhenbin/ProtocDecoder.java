package com.zhenbin;

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

    private static final int HEADER_SIZE = LENGTH_FIELD_OFFSET + LENGTH_FIELD_LENGTH;

    public ProtocDecoder() {
        super(BYTE_ORDER, MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP, FAIL_FAST);
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
//        super.decode(ctx, in);
        System.out.println("start decoding ...");
//        if (in.readableBytes() < HEADER_SIZE)
//        {
//            System.out.println("too small");
//            throw new Exception("too small");
//        }
        byte type = in.readByte();
        // 如果是Little Endian则使用readIntLE
        int length = in.readInt();
//        if (in.readableBytes() < length)
//        {
//            System.out.println("too short");
//            throw new Exception("too short");
//        }
        byte[] bytes = new byte[length];
        in.readBytes(bytes, 0, length);
        System.out.println(type + " " + length + " " + new String(bytes));
        return new ProtocMsg(type, bytes);
    }
}