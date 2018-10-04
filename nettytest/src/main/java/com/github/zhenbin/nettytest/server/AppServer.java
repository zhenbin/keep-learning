package com.github.zhenbin.nettytest.server;

import com.github.zhenbin.nettytest.dto.ProtocMsg;
import com.github.zhenbin.nettytest.handler.DispatcherHandler;
import com.github.zhenbin.nettytest.handler.ProtocDecoder;
import com.github.zhenbin.nettytest.processor.Processor;
import com.github.zhenbin.nettytest.processor.SayHiProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.HashMap;
import java.util.Map;

public class AppServer {
    private int port;
    private static final Map<Byte, Processor<ProtocMsg>> processors = new HashMap<Byte, Processor<ProtocMsg>>();

    private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup("ChannelGroups", GlobalEventExecutor.INSTANCE);

    static {
        processors.put(Byte.valueOf("1"), new SayHiProcessor());
    }

    public AppServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        String systemInfo = System.getProperty("os.name");
        System.out.println("system info: " + systemInfo);

        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;
        Class<? extends ServerChannel> serverChannelClass;
        /**
         * 当linux系统内核不低于2.5.44(2002)时，可使用epoll。
         * 此处默认当系统为linux时即有epoll特性。
         */
        if (systemInfo.toLowerCase().indexOf("linux") > 0) {
            bossGroup = new EpollEventLoopGroup();
            workerGroup = new EpollEventLoopGroup();
            serverChannelClass = EpollServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            serverChannelClass = NioServerSocketChannel.class;
        }

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(serverChannelClass)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new ProtocDecoder())
                                    .addLast(new DispatcherHandler<ProtocMsg>(processors))
                            ;
                            System.out.println("finish init channel");
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();

            System.out.println("start server, listen port: " + port);
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
