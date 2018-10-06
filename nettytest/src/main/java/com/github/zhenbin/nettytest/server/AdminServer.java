package com.github.zhenbin.nettytest.server;

import com.github.zhenbin.nettytest.dto.ProtocMsg;
import com.github.zhenbin.nettytest.handler.DispatcherHandler;
import com.github.zhenbin.nettytest.handler.ProtocDecoder;
import com.github.zhenbin.nettytest.handler.RegisterHandler;
import com.github.zhenbin.nettytest.processor.Processor;
import com.github.zhenbin.nettytest.processor.ResponseProcessor;
import com.github.zhenbin.nettytest.processor.SayHiProcessor;
import com.github.zhenbin.nettytest.processor.SayHiToExecutorProcessor;
import com.github.zhenbin.nettytest.util.ChannelMap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class AdminServer {
    private static final Logger LOGGER = Logger.getLogger(AdminServer.class);

    private static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;
    private static final int MAX_FRAME_LENGTH = 1024 * 1024;
    private static final int LENGTH_FIELD_OFFSET = 1;
    private static final int LENGTH_FIELD_LENGTH = 4;
    private static final int LENGTH_ADJUSTMENT = 0;
    private static final int INITIAL_BYTES_TO_STRIP = 0;
    private static final boolean FAIL_FAST = false;

    private int port;
    private final Map<Byte, Processor<ProtocMsg>> processors = new HashMap<Byte, Processor<ProtocMsg>>();

    public AdminServer(int port, MasterServer masterServer) {
        this.port = port;
        processors.put(Byte.valueOf("1"), new SayHiToExecutorProcessor(masterServer.getChannels()));
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new ProtocDecoder())
                                    .addLast(new DispatcherHandler<>(processors))
                            ;
                            LOGGER.info("finish init channel");
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();
            LOGGER.info("start server, listen port: " + port);
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
