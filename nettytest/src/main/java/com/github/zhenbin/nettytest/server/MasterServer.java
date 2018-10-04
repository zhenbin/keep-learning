package com.github.zhenbin.nettytest.server;

import com.github.zhenbin.nettytest.dto.ProtocMsg;
import com.github.zhenbin.nettytest.handler.DispatcherHandler;
import com.github.zhenbin.nettytest.handler.ProtocDecoder;
import com.github.zhenbin.nettytest.handler.RegisterHandler;
import com.github.zhenbin.nettytest.processor.Processor;
import com.github.zhenbin.nettytest.processor.SayHiProcessor;
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

import java.util.HashMap;
import java.util.Map;

public class MasterServer {
    private static final Logger LOGGER = Logger.getLogger(MasterServer.class);

    private int port;
    private static final Map<Byte, Processor<ProtocMsg>> processors = new HashMap<Byte, Processor<ProtocMsg>>();

    private final ChannelMap channels = new ChannelMap();

    static {
        processors.put(Byte.valueOf("1"), new SayHiProcessor());
    }

    public MasterServer(int port) {
        this.port = port;
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
                                    .addLast(new RegisterHandler(channels))
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
