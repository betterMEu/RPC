package com.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yls91
 */

@Getter
@AllArgsConstructor
public enum PropertiesEnum {
    /**
     *
     */
    APPLICATION("application.yml"),
    ZK_ADDRESS("zk.address");

    private final String value;
}
