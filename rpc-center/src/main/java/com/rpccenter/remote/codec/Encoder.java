package com.rpccenter.remote.codec;

import com.common.extension.ExtensionLoader;
import com.rpccenter.remote.codec.compress.Compress;
import com.rpccenter.remote.codec.compress.CompressTypeEnum;
import com.rpccenter.remote.dto.RpcConstants;
import com.rpccenter.remote.dto.RpcMessage;
import com.rpccenter.remote.codec.serializer.SerializationTypeEnum;
import com.rpccenter.remote.codec.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义协议编码
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
@Slf4j
public class Encoder extends MessageToByteEncoder<RpcMessage> {
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) throws Exception {
        try {
            out.writeBytes(RpcConstants.MAGIC_CODE);
            out.writeByte(RpcConstants.VERSION);

            //调整writerIndex位置 留4byte 后面写入数据长度
            out.writerIndex(out.writerIndex() + 4);

            //单独拿出来 需要检测是否是心跳消息
            Byte messageType = msg.getMessageType();

            out.writeByte(messageType);
            out.writeByte(msg.getCode());
            out.writeByte(msg.getCompress());

            //请求id
            out.writeInt(ATOMIC_INTEGER.getAndIncrement());

            //fullLength=头部长度+身体长度
            int fullLength = RpcConstants.HEAD_LENGTH;
            byte[] bodyBytes;

            if (messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE
                    && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {

                //序列化
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(SerializationTypeEnum.getName(msg.getCode()));
                //压缩
                Compress compressor = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(CompressTypeEnum.getName(msg.getCompress()));
                bodyBytes = compressor.compress(serializer.serialize(msg.getData()));

                fullLength += bodyBytes.length;
                out.writeBytes(bodyBytes);
            }


            //获取当前writerIndex的位置
            int writeIndex = out.writerIndex();

            //调整writerIndex的位置，回到前面预留空间的位置
            out.writerIndex(writeIndex - fullLength + RpcConstants.MAGIC_CODE.length + RpcConstants.VERSION);
            //写下 fullLength=数据头+数据长度
            out.writeInt(fullLength);

            out.writerIndex(writeIndex);
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
    }
}
