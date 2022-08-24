package com.common.util;

/**
 * @author yls91
 */
public class RuntimeUtil {

    /**
     * @return 虚拟机可用的最大处理器数;永远不会小于1
     * */
    public static int getCpuNum() {
        return Runtime.getRuntime().availableProcessors();
    }
}
