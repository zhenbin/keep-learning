package com.github.zhenbin.nettytest.processor;

import com.github.zhenbin.nettytest.dto.ProtocMsg;
import io.netty.channel.Channel;
import org.apache.log4j.Logger;

public class SayHiProcessor implements Processor<ProtocMsg> {
    private static final Logger LOGGER = Logger.getLogger(SayHiProcessor.class);

    public byte[] process(Channel channel, ProtocMsg msg) {
        LOGGER.info("Hi, I am SayHi processor!");
        return msg.msg;
    }
}
