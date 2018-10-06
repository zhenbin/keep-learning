package com.github.zhenbin.nettytest.processor;

import com.github.zhenbin.nettytest.dto.ProtocMsg;
import com.github.zhenbin.nettytest.util.ChannelMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentMap;

public class ResponseProcessor implements Processor<ProtocMsg> {
    private static final Logger LOGGER = Logger.getLogger(ResponseProcessor.class);

    private final ConcurrentMap<Integer, ChannelMap.Resp> taskRespMap;

    public ResponseProcessor(ConcurrentMap<Integer, ChannelMap.Resp> taskRespMap) {
        this.taskRespMap = taskRespMap;
    }

    public byte[] process(Channel channel, ProtocMsg msg) {
        byte[] bytes = msg.msg;
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeBytes(bytes);
        int taskId = byteBuf.readInt();
        ChannelMap.Resp resp = taskRespMap.get(taskId);
        if (null == resp) {
            LOGGER.warn("已经被删除, taskId: " + taskId);
            return null;
        }
        synchronized (resp) {
            resp.response.taskResult.put(channel.id(), bytes);
            if (resp.response.taskResult.size() == resp.response.need_count) {
                try {
                    resp.done.add(true);
                } catch (IllegalStateException e) {
                }
            }
        }
        return null;
    }
}
