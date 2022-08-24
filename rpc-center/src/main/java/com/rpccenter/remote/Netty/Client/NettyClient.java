package com.rpccenter.remote.Netty.Client;

import com.common.extension.ExtensionLoader;
import com.common.factory.SingletonFactory;
import com.rpccenter.remote.codec.compress.CompressTypeEnum;
import com.rpccenter.registry.ServiceDiscovery;
import com.rpccenter.remote.Netty.ChannelProvider;
import com.rpccenter.remote.codec.Decoder;
import com.rpccenter.remote.codec.Encoder;
import com.rpccenter.remote.dto.RpcConstants;
import com.rpccenter.remote.dto.RpcMessage;
import com.rpccenter.remote.dto.RpcRequest;
import com.rpccenter.remote.dto.RpcResponse;
import com.rpccenter.remote.codec.serializer.SerializationTypeEnum;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author yls91
 */
@Slf4j

public class NettyClient implements RpcRequestTransport {
    private final ChannelProvider channelProvider;
    private final UnprocessedRequests unprocessedRequests;
    private final ServiceDiscovery serviceDiscovery;

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;

    public NettyClient() {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000)
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline cp = ch.pipeline();
                        /* channel空闲时，前三个参数为0都代表禁用
                         * readerIdleTime：指定时间内不读触发
                         * writerIdleTime: 指定时间内不写触发
                         * allIdleTime：指定时间内不读不写触发
                         * TimeUnit：时间单位（时分秒）
                         * */
                        cp.addLast(new IdleStateHandler(0,31,0, TimeUnit.SECONDS));
                        cp.addLast(new Encoder());
                        cp.addLast(new Decoder());
                        cp.addLast(new ClientHandler());
                    }
                });

        channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
        unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("discovery");
    }

    @SneakyThrows
    public Channel connect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if(future.isSuccess()) {
                System.out.println("client连接到了" + inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            }else {
                log.info("连接[{}]失败，或许是server没打开此端口", inetSocketAddress.toString());
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }

    @Override
    public CompletableFuture<RpcResponse<Object>> sendRpcRequest(RpcRequest rpcRequest) {
        CompletableFuture<RpcResponse<Object>> result = new CompletableFuture<>();
        //服务器地址
        InetSocketAddress inetSocketAddress = serviceDiscovery.getIpByRequest(rpcRequest);

        Channel channel = getChannel(inetSocketAddress);
        if(channel.isActive()) {
            unprocessedRequests.put(rpcRequest.getId(),result);

            //此处决定序列化方式 和压缩方式
            RpcMessage rpcMessage = RpcMessage.builder()
                    .data(rpcRequest)
                    .code(SerializationTypeEnum.KRYO.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST)
                    .build();
            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("请求发送成功: [{}]", rpcMessage);
                } else {
                    future.channel().close();
                    result.completeExceptionally(future.cause());
                    log.error("请求发送失败: [{ }]", future.cause());
                }
            });
        } else {
            throw new IllegalStateException("获取channel失败");
        }

        return result;
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if(channel == null) {
            channel = connect(inetSocketAddress);
            channelProvider.put(inetSocketAddress,channel);
        }
        return channel;
    }

    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}
