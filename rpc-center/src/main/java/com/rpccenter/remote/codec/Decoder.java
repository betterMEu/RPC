package com.rpccenter.remote.codec;

import com.common.extension.ExtensionLoader;
import com.rpccenter.remote.codec.compress.Compress;
import com.rpccenter.remote.codec.compress.CompressTypeEnum;
import com.rpccenter.remote.dto.RpcConstants;
import com.rpccenter.remote.dto.RpcMessage;
import com.rpccenter.remote.dto.RpcRequest;
import com.rpccenter.remote.dto.RpcResponse;
import com.rpccenter.remote.codec.serializer.SerializationTypeEnum;
import com.rpccenter.remote.codec.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import java.util.Arrays;

/**
 * 自定义协议编码器
 * <pre>
 *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 *   |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 * </pre>
 * @author yls91
 */
public class Decoder extends LengthFieldBasedFrameDecoder {


    public Decoder() {
        this(RpcConstants.MAX_LENGTH, 5, 4, -9, 0);
    }

    /**
     * @param maxFrameLength      可以接收的数据的最大长度。如果超过，数据将被丢弃
     *
     * @param lengthFieldOffset   长度字段偏移  长度字段=消息长度 包括包头协议  也就是字段长度的下标  根据我们协议 下标为5
     *
     * @param lengthFieldLength   长度字段数据类型的 字节数  这里是int 4个字节
     *
     * @param lengthAdjustment    要添加到长度字段值的补偿值
     *
     * @param initialBytesToStrip 跳过的字节数
     *                            如果需要接收所有协议数据头+正文数据，则该值为0
     *                            如果您只想接收正文数据，那么需要跳过标头消耗的字节数
     *                            通常需要检查magic Code和version，通常为0
     */
    public Decoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                             int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        //调用父类decode检验传送过来的数据是否符合自定义协议的规范 返回为ByteBuf说明符合
        Object decoded = super.decode(ctx,in);

        if(decoded instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decoded;

            if (frame.readableBytes() >= RpcConstants.HEAD_LENGTH) {
                try {
                    return decode(in);
                } finally {
                    frame.release();
                }
            }
        }
        return decoded;
    }

    private Object decode(ByteBuf in) {
        //不清楚原因 readIndex和writeIndex都在最后面  所以不调整readIndex会报错
        in.readerIndex(0);

        checkMagicNumber(in);
        checkVersion(in);
        int fullLength = in.readInt();
        byte messageType = in.readByte();
        byte codecType = in.readByte();
        byte compressType = in.readByte();
        int requestId = in.readInt();

        RpcMessage message = RpcMessage.builder()
                .messageType(messageType)
                .code(codecType)
                .compress(compressType)
                .requestId(requestId).build();

        //如果是心跳消息
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            message.setData(RpcConstants.PING);
            return message;
        }
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            message.setData(RpcConstants.PONG);
            return message;
        }

        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        if(bodyLength > 0) {
            byte[] bytes = new byte[bodyLength];
            in.readBytes(bytes);

            String compressName = CompressTypeEnum.getName(compressType);
            Compress compressor = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
            bytes = compressor.decompress(bytes);

            String codecName = SerializationTypeEnum.getName(codecType);
            Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
            if (messageType == RpcConstants.REQUEST) {
                RpcRequest tmpValue = serializer.deserialize(bytes, RpcRequest.class);
                message.setData(tmpValue);
            } else {
                RpcResponse tmpValue = serializer.deserialize(bytes, RpcResponse.class);
                message.setData(tmpValue);
            }
        }
        return message;
    }

    private void checkVersion(ByteBuf in) {
        byte version = in.readByte();
        if (version != RpcConstants.VERSION) {
            throw new RuntimeException("版本不匹配" + version);
        }
    }

    private void checkMagicNumber(ByteBuf in) {
        int len = RpcConstants.MAGIC_CODE.length;
        byte[] tmp = new byte[len];
        in.readBytes(tmp);
        for (int i = 0; i < len; i++) {
            if (tmp[i] != RpcConstants.MAGIC_CODE[i]) {
                throw new IllegalArgumentException("未知magic code: " + Arrays.toString(tmp));
            }
        }
    }


}
