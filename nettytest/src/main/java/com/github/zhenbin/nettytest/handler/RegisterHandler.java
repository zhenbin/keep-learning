package com.github.zhenbin.nettytest.handler;

import com.github.zhenbin.nettytest.util.ChannelMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;

public class RegisterHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = Logger.getLogger(RegisterHandler.class);

    private final ChannelMap channelMap;

    public RegisterHandler(ChannelMap channels) {
        this.channelMap = channels;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ChannelId id = channel.id();
        if (null == id) {
            return;
        }
        LOGGER.info("channel active: " + id);
        channelMap.put(channel);
        brocastNodeAddrs();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ChannelId id = channel.id();
        if (null == id) {
            return;
        }
        LOGGER.info("channel inactive: " + id);
        channelMap.remove(id);
        brocastNodeAddrs();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void brocastNodeAddrs() {
        ByteBuf list = Unpooled.buffer();
        channelMap.brocast(list, (channelId, ch) -> {
            list.writeBytes((channelId + ": " + ch.remoteAddress().toString() + "\n").getBytes());
        });
    }
}
