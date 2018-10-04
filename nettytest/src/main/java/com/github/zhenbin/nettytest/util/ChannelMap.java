package com.github.zhenbin.nettytest.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

public class ChannelMap {

    private final Map<ChannelId, Channel> channelMap = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Channel put(Channel channel) {
        lock.writeLock().lock();
        Channel ch;
        try {
            ch = channelMap.put(channel.id(), channel);
        } finally {
            lock.writeLock().unlock();
        }
        return ch;
    }

    public Channel remove(ChannelId id) {
        lock.writeLock().lock();
        Channel ch;
        try {
            ch = channelMap.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
        return ch;
    }

    public void brocast(ByteBuf result) {
        brocast(result, null);
    }

    public void brocast(ByteBuf result, BiConsumer<ChannelId, Channel> prepareResult) {
        lock.readLock().lock();
        try {
            if (null != prepareResult) {
                channelMap.forEach(prepareResult);
            }

            // todo remove me
            int length = result.readableBytes();
            ByteBuf byteBuf = Unpooled.buffer(length + 4);
            byteBuf.writeInt(length);
            byteBuf.writeBytes(result);

            channelMap.forEach((channelId, ch) -> {
                ByteBuf tmpBuf = byteBuf.copy();
                ch.writeAndFlush(tmpBuf);
            });
        } finally {
            lock.readLock().unlock();
        }
    }
}
