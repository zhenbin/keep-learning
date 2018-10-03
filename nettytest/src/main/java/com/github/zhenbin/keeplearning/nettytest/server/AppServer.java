package com.github.zhenbin.keeplearning.nettytest.server;

import com.github.zhenbin.keeplearning.nettytest.dto.ProtocMsg;
import com.github.zhenbin.keeplearning.nettytest.handler.business.EchoHandler;
import com.github.zhenbin.keeplearning.nettytest.handler.business.Handler;
import com.github.zhenbin.keeplearning.nettytest.handler.channel.ProtocDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AppServer {
    private int port;
    private static final Map<Byte, Handler> handlers = new HashMap<Byte, Handler>();
    private static final ConcurrentMap<ChannelId, Channel> channels = new ConcurrentHashMap<ChannelId, Channel>();

    private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup("ChannelGroups", GlobalEventExecutor.INSTANCE);

    static {
        handlers.put(Byte.valueOf("1"), new EchoHandler());
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
                                    .addLast(new AppServerHandler())
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

    static class AppServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            ChannelId id = channel.id();
            if (null != id) {
                System.out.println("got channel: " + id);
                channels.putIfAbsent(id, channel);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            ChannelId id = channel.id();
            if (null != id) {
                System.out.println("start cancelling channel: " + id);
                channels.remove(id);
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
            ProtocMsg protocMsg = (ProtocMsg) msg;
            Handler handler = handlers.get(protocMsg.type);
            byte[] resp = "cannot find handler".getBytes();
            if (null != handler) {
                resp = handler.process(protocMsg.msg);
            }
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeInt(resp.length);
            byteBuf.writeBytes(resp);
            ctx.writeAndFlush(byteBuf);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
            // Close the connection when an exception is raised.
            cause.printStackTrace();
            ctx.close();
        }
    }

    public static void main(String[] args) throws Exception {
        new AppServer(12354).start();
    }
}
