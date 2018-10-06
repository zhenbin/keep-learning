package com.github.zhenbin.nettytest.handler;

import com.github.zhenbin.nettytest.dto.Type;
import com.github.zhenbin.nettytest.processor.Processor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Map;

public class DispatcherHandler<T extends Type> extends ChannelInboundHandlerAdapter {

    private Map<Byte, Processor<T>> processors;

    public DispatcherHandler(Map<Byte, Processor<T>> processors) {
        this.processors = processors;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        T typedMsg = (T) msg;
        Processor<T> handler = processors.get(typedMsg.getType());
        byte[] resp = "cannot find handler".getBytes();

        if (null != handler) {
            resp = handler.process(ctx.channel(), typedMsg);
        }
        if (null != resp) {
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeByte(typedMsg.getType());
            byteBuf.writeInt(resp.length);
            byteBuf.writeBytes(resp);
            ctx.writeAndFlush(byteBuf);
        }
        //todo release msg
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}