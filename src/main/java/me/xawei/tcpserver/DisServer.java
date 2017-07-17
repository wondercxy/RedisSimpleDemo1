package me.xawei.tcpserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.InetSocketAddress;

/**
 * Created by wei on 2017/7/17.
 */
public class DisServer {
    private int port;

    public DisServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        new DisServer(8000).start();
    }

    public void start() {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup(20);
        final EventExecutorGroup executorGroup = new DefaultEventExecutorGroup(2);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.config().setReceiveBufferSize(2048); //set  buf size here
                            socketChannel.pipeline().addLast(executorGroup, new RedisServerHandler());
                        }
                    });

            // 绑定服务器，并等待绑定完成。
            ChannelFuture future = bootstrap.bind().sync();
            // 阻塞直到服务器的Channel关闭。
            future.channel().closeFuture().sync();
        } catch (Exception e){
            e.printStackTrace();
        }
        finally {
//            关闭EventLoopGroup,并且释放所有的资源，包括被创建的线程。
//            group.shutdownGracefully().sync();
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
