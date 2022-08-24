package com.rpccenter.remote.Netty.Client;


import com.common.factory.SingletonFactory;
import com.rpccenter.remote.codec.compress.CompressTypeEnum;
import com.rpccenter.remote.dto.RpcConstants;
import com.rpccenter.remote.dto.RpcMessage;
import com.rpccenter.remote.dto.RpcResponse;
import com.rpccenter.remote.codec.serializer.SerializationTypeEnum;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * Client处理者：主要处理Server的回应
 *
 * 继承自 SimpleChannelInboundHandler 的话就不要考虑 ByteBuf 的释放,但这里不是
 * @author yls91
 */
@Slf4j
public class ClientHandler extends ChannelInboundHandlerAdapter {
    private final UnprocessedRequests unprocessedRequests;
    private final NettyClient nettyClient;


    public ClientHandler() {
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nettyClient = SingletonFactory.getInstance(NettyClient.class);
    }

    //处理Server的Response
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            log.info("server回应: [{}]", msg);
            if (msg instanceof RpcMessage) {
                RpcMessage rpcMessage = (RpcMessage) msg;
                byte messageType = rpcMessage.getMessageType();

                if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                    log.info("心跳 [{}]", rpcMessage.getData());
                } else if (messageType == RpcConstants.RESPONSE) {
                    RpcResponse<Object> rpcResponse = (RpcResponse<Object>) rpcMessage.getData();
                    unprocessedRequests.complete(rpcResponse);
                    log.info("执行结果{}",rpcResponse.getResult());
                }
            }
        }finally {
            //否则内存泄露问题
            ReferenceCountUtil.release(msg);
        }
    }

    //当Channel空闲时，发送心跳消息的格式定义
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                log.info("channel空闲 [{}]", ctx.channel().remoteAddress());
                Channel channel = nettyClient.getChannel((InetSocketAddress) ctx.channel().remoteAddress());
                RpcMessage rpcMessage = RpcMessage.builder()
                        .code(SerializationTypeEnum.KRYO.getCode())
                        .compress(CompressTypeEnum.GZIP.getCode())
                        .messageType(RpcConstants.HEARTBEAT_REQUEST_TYPE)
                        .data(RpcConstants.PING)
                        .build();
                channel.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }


    /**
     * 在处理客户端消息时发生异常时调用
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("客户端捕获异常：", cause);
        cause.printStackTrace();
        ctx.close();
    }

}
