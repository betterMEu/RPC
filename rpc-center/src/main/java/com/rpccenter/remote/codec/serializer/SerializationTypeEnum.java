package com.rpccenter.remote.codec.serializer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yls91
 */
@AllArgsConstructor
@Getter
public enum SerializationTypeEnum {

    //
    KRYO((byte) 0x01, "kryo");

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        SerializationTypeEnum[] values = SerializationTypeEnum.values();
        for (SerializationTypeEnum c : values) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}
