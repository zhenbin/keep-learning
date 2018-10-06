package com.github.zhenbin.nettytest.processor;

import io.netty.channel.Channel;

public interface Processor<T> {
    byte[] process(Channel channel, T msg);
}
