package com.github.zhenbin.nettytest.dto;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;

import java.util.Map;

public class Response {
    public static final int SUCCESS = 1;
    public static final int RESPONSE_TIME_OUT = 2;
    public static final int TYPE_UNSUPPORTED = 3;
    public static final int SEND_FAILURE = 4;
    public static final int INTERRUPTED = 5;
    public static final int SYSTEM_ERROR = 6;
    public static final int RESPONSE_QUEUE_FULL = 7;
    public static final int TASK_EXISTED = 8;

    private int status;
    public final int need_count;
    public final Map<ChannelId, byte[]> taskResult; // 确保map的修改在其它线程可见

    public Response(int need_count, Map<ChannelId, byte[]> taskResult) {
        this.need_count = need_count;
        this.taskResult = taskResult;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
