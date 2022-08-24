package com.rpccenter.remote.Netty.Server;

import com.common.factory.SingletonFactory;
import com.common.util.RuntimeUtil;
import com.common.util.ThreadPoolFactoryUtil;
import com.rpccenter.remote.dto.RpcServiceConfig;
import com.rpccenter.remote.ServiceProvider.Impl.ServiceProviderImpl;
import com.rpccenter.remote.ServiceProvider.ServiceProvider;
import com.rpccenter.remote.codec.Decoder;
import com.rpccenter.remote.codec.Encoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author yls91
 * 一个服务器节点
 * @Component 交由spring管理 可以实现由注解完成服务发布
 */
@Slf4j
@Component
public class NettyServer {

    public static final int PORT = 9981;

    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ServiceProviderImpl.class);

    public void registerService(RpcServiceConfig rpcService) {
        serviceProvider.publishService(rpcService);
    }

    @SneakyThrows
    public void start() {
        String host = InetAddress.getLocalHost().getHostAddress();

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(RuntimeUtil.getCpuNum() * 2,
                ThreadPoolFactoryUtil.createThreadFactory("rpc_Server_Thread_Pool_1",false));
        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    //发送大数据快，减少网络传输
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    //开启tcp心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    //临时存放已完成三次握手的请求的队列的最大长度
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))

                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline cp = ch.pipeline();
                            // 30 秒之内没有收到客户端请求的话就关闭连接
                            cp.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            cp.addLast(new Encoder());
                            cp.addLast(new Decoder());
                            cp.addLast(serviceHandlerGroup,new ServerHandler());
                        }
                    });
            ChannelFuture future = serverBootstrap.bind(host,PORT).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("连接server发生异常", e);
        }finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }

}
