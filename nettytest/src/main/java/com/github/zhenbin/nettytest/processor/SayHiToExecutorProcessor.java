package com.github.zhenbin.nettytest.processor;

import com.github.zhenbin.nettytest.dto.ProtocMsg;
import com.github.zhenbin.nettytest.dto.Response;
import com.github.zhenbin.nettytest.util.ChannelMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.apache.log4j.Logger;

public class SayHiToExecutorProcessor implements Processor<ProtocMsg> {
    private static final Logger LOGGER = Logger.getLogger(SayHiToExecutorProcessor.class);
    private final ChannelMap channelMap;

    public SayHiToExecutorProcessor(ChannelMap channelMap) {
        this.channelMap = channelMap;
    }

    public byte[] process(Channel channel, ProtocMsg msg) {
        LOGGER.info("get message: " + new String(msg.msg));
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(2); // 类型
        byteBuf.writeInt(4 + msg.msg.length); // task id and msg
        byteBuf.writeInt(1); // task id
        byteBuf.writeBytes(msg.msg);
        Response response = channelMap.taskDispatch(byteBuf, 1, 1000);
        LOGGER.info("reponse status: " + response.getStatus());
        for (byte[] bytes : response.taskResult.values()) {
            LOGGER.info("from client: " + new String(bytes));
        }
        return ("status: " + response.getStatus() + ", size: " + response.taskResult.size()).getBytes();
    }
}
