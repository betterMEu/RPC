package com.rpccenter.remote.dto;

import lombok.*;

/**真正在通道中的消息 内容是Request或Response之一，同时表明序列化方式和压缩方式
 * @author yls91
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class RpcMessage {
    private byte messageType;
    private byte code;
    private byte compress;
    private int requestId;
    private Object data;
}
