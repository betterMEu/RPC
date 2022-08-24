package com.rpccenter.remote.Netty.Server;

import com.common.factory.SingletonFactory;
import com.rpccenter.remote.codec.compress.CompressTypeEnum;
import com.rpccenter.remote.dto.RpcConstants;
import com.rpccenter.remote.dto.RpcMessage;
import com.rpccenter.remote.dto.RpcRequest;
import com.rpccenter.remote.dto.RpcResponse;
import com.rpccenter.remote.codec.serializer.SerializationTypeEnum;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yls91
 */
@Slf4j
public class ServerHandler extends ChannelInboundHandlerAdapter {
    private final RequestInvocationHandler requestInvocationHandler;

    public ServerHandler() {
        this.requestInvocationHandler = SingletonFactory.getInstance(RequestInvocationHandler.class);
    }

    /**
     * 读取Channel中client请求，调用执行后发送回应
     * */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof RpcMessage) {
                log.info("client请求: [{}] ", msg);
                byte messageType = ((RpcMessage) msg).getMessageType();
                RpcMessage rpcMessage = RpcMessage.builder()
                                .code(SerializationTypeEnum.KRYO.getCode())
                                .compress(CompressTypeEnum.GZIP.getCode())
                                .build();

                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                    rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    rpcMessage.setData(RpcConstants.PONG);
                } else {
                    rpcMessage.setMessageType(RpcConstants.RESPONSE);
                    //执行request请求里的服务
                    RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();
                    Object result = requestInvocationHandler.handle(rpcRequest);
                    log.info(String.format("执行结果: %s", result.toString()));

                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getId());
                        rpcMessage.setData(rpcResponse);
                    } else {
                        RpcResponse<Object> rpcResponse = RpcResponse.fail();
                        rpcMessage.setData(rpcResponse);
                        log.error("现在不能写，消息掉了");
                    }
                }
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {
            //避免bytebuf内存泄漏
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                log.info("空闲检查发生，所以关闭连接");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("服务器捕获异常");
        cause.printStackTrace();
        ctx.close();
    }

}
