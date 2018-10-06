package com.github.zhenbin.nettytest.util;

import com.github.zhenbin.nettytest.dto.Response;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelId;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

public class ChannelMap {

    public class Resp {
        public final Response response;
        // used for check if all response done.
        public final BlockingQueue<Boolean> done;

        public Resp(Response response) {
            this.response = response;
            done = new ArrayBlockingQueue<>(1);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(ChannelMap.class);

    private final Map<ChannelId, Channel> channelMap = new HashMap<>();
    private final ConcurrentMap<Integer, Resp> taskRespMap = new ConcurrentHashMap<>();
    private final Map<ChannelId, Map<Byte, BlockingQueue<Resp>>> respMap = new HashMap<>();
    // TODO 读写锁要用写的优先级最高的那种
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Byte, Lock> typeLocks = new HashMap<>();

    public ConcurrentMap<Integer, Resp> getTaskRespMap() {
        return taskRespMap;
    }

    public ChannelMap(Byte... types) {
        // todo types是否可以是null
        for (Byte type : types) {
            // 是不是用ReentrantLock待定
            typeLocks.put(type, new ReentrantLock());
        }
    }

    public Channel put(Channel channel) {
        lock.writeLock().lock();
        Channel ch;
        try {
            ch = channelMap.put(channel.id(), channel);
            // 初始化respMap
            Map<Byte, BlockingQueue<Resp>> typeMap = new HashMap<>();
            for (Byte type : typeLocks.keySet()) {
                // todo arrayBlockingQueue是ring的吗？
                //TODO queue的大小要大于业务线程池的大小，才能保证所有线程并发时够用
                typeMap.put(type, new ArrayBlockingQueue<>(100));
            }
            respMap.put(channel.id(), typeMap);
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
            respMap.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
        return ch;
    }

    /**
     * 加上taskId的好处：
     * 1. 防止重复下发；
     * 2. 错误容易追溯，便于排查问题；
     * 3. 设计上看简单，系统更稳定，代码更好维护
     */

    public Response taskDispatch(ByteBuf cmd, int taskId, long timeout) {
        Resp resp;
        lock.readLock().lock();
        try {
            // 获取size，所以要先lock
            Response response = new Response(channelMap.size(), new HashMap<>());
            resp = new Resp(response);
            Resp previousResp = taskRespMap.putIfAbsent(taskId, resp);
            if (null != previousResp) {
                LOGGER.warn("当前任务已经存在，正在运行, taskId: " + taskId);
                response.setStatus(Response.TASK_EXISTED);
                taskRespMap.remove(taskId);
                return response;
            }

            for (Channel channel : channelMap.values()) {
                ByteBuf tmpBuf = cmd.copy();
                ChannelFuture channelFuture = channel.writeAndFlush(tmpBuf);
                channelFuture.addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        LOGGER.error("数据写失败, taskId: " + taskId);
                        future.cause().printStackTrace();
                        // todo 这里对resp的引用，在for循环里没问题吧？
                        synchronized (resp) {
                            // TODO 这里的处理方式是有结点写失败了就返回失败，这样做合理不？？
                            // 还是换成wait/notify, await/signal 这种形式吧
                            try {
                                resp.done.add(true);
                            } catch (IllegalStateException e) {
                            }
                        }
                    }
                });
            }
        } finally {
            lock.readLock().unlock();
        }

        try {
            Boolean done = resp.done.poll(timeout, TimeUnit.MILLISECONDS);
            // 在此处统一设置status，防止多线程覆盖写
            synchronized (resp) {
                if (null == done) {
                    resp.response.setStatus(Response.RESPONSE_TIME_OUT);
                } else if (resp.response.need_count == resp.response.taskResult.size()) {
                    resp.response.setStatus(Response.SUCCESS);
                } else {
                    resp.response.setStatus(Response.SYSTEM_ERROR);
                }
            }
        } catch (InterruptedException e) {
            resp.response.setStatus(Response.INTERRUPTED);
            e.printStackTrace();
        }
        taskRespMap.remove(taskId);
        return resp.response;
    }


    // todo 把cmd/type 这些弄成一个OutputMsg implements Type。然后在OutBoundHandler encode它。
    // todo 这里的type是指response的type，而不是当前请求的type，必要时得弄一个映射
    // TODO 要求一下在master下发任务的时候加上taskID，并让返回消息带回taskID，这样可以大大简化设计复杂性，也提高了系统可维护性。
    public Response dispatch(ByteBuf cmd, byte type, long timeout, TimeUnit unit) {
        Response response = new Response(respMap.size(), new HashMap<>());
        Resp resp = new Resp(response);
        // 获取当前这个type的写锁！这样才能保证对map批量写的原子性。
        Lock typeLock = typeLocks.get(type);
        if (null == typeLock) {
            LOGGER.warn("4XX. 不支持的操作类型，type: " + type);
            response.setStatus(Response.TYPE_UNSUPPORTED);
            return response;
        }

        typeLock.lock();
        try {
            lock.readLock().lock();
            try {
                for (ChannelId channelId : channelMap.keySet()) {
                    Map<Byte, BlockingQueue<Resp>> typeMap = respMap.get(channelId);
                    BlockingQueue blockingQueue = typeMap.get(type);
                    try {
                        //TODO 这种情况一般是该节点出问题了，所以一直没收到返回数据，所以还是立即失败好了
                        blockingQueue.add(resp);
                    } catch (IllegalStateException e) {
                        // TODO 设置一个小队列测试一下是不是会到达这个状态
                        // TODO 给各个失败日志打印远程IP:PORT，有助于排查问题。
                        // TODO 出现节点有问题的情况，是要继续给其它节点发呢还是直接返回失败呢？
                        LOGGER.warn("response queue已经满了！ type: " + type + ", channelId = " + channelId);
                        response.setStatus(Response.RESPONSE_QUEUE_FULL);
                        return response;
                    }
                    Channel channel = channelMap.get(channelId);
                    // TODO 这个writeAndFlush是怎么写的？是由某一个线程真正写吗？
                    // TODO 多个线程写的话，写线程是不是会按大家的调用顺序去写？
                    // TODO listener监听是真正把数据传出去的时候写吗？
                    // TODO 如果远程挂了没通知服务端，那还能不能正常的写出去？
                    // TODO writeAndFlush会不会一个write成功了flush不成功？如果是的话数据会不会等下还被发出去？
                    ChannelFuture channelFuture = channel.writeAndFlush(cmd);
                    channelFuture.addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {
                            LOGGER.error("数据写失败，");
                            // todo 这里对blockingQueue和resp的引用，在for循环里没问题吧？
                            // 没有成功写出去，则不会接收到返回，所以要把接收数据的请求从接收队列里删除。
                            blockingQueue.remove(resp);
                            future.cause().printStackTrace();
                        }
                    });
                }
            } finally {
                lock.readLock().unlock();
            }
        } finally {
            typeLock.unlock();
        }

        // 数据成功发出去后在外面等，尽快释放锁
        try {
            Boolean done = resp.done.poll(timeout, unit);
            synchronized (resp) {
                if (null != done) {
                    resp.response.setStatus(Response.SUCCESS);
                } else {
                    resp.response.setStatus(Response.RESPONSE_TIME_OUT);
                }
            }
        } catch (InterruptedException e) {
            resp.response.setStatus(Response.INTERRUPTED);
            e.printStackTrace();
        }
        return resp.response;
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
            ByteBuf byteBuf = Unpooled.buffer(1 + 4 + length);
            byteBuf.writeByte(3);
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
