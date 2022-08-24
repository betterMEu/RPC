package com.rpccenter.remote.codec.compress;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yls91
 */

@AllArgsConstructor
@Getter
public enum CompressTypeEnum {

    /**
     *
     */
    GZIP((byte) 0x01, "gzip");

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        CompressTypeEnum[] values = CompressTypeEnum.values();
        for (CompressTypeEnum c : values) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}
