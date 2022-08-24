package com.rpccenter.loadbalance.Impl;

import com.rpccenter.loadbalance.AbstractLoadBalance;
import com.rpccenter.remote.dto.RpcRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yls91
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> addressList, RpcRequest rpcRequest) {
        // 生成服务器调用列表hashCode
        int addressListHashCode = System.identityHashCode(addressList);

        String rpcServiceName = rpcRequest.getServiceName();

        //根据服务名获取一致性hash选择器
        ConsistentHashSelector selector = selectors.get(rpcServiceName);

        //若不存在则创建新的选择器
        if (selector == null || selector.identityHashCode != addressListHashCode) {
            selectors.put(rpcServiceName, new ConsistentHashSelector(addressList, 160, addressListHashCode));
            selector = selectors.get(rpcServiceName);
        }
        //service.AddServiceThree1.0java.util.stream.ReferencePipeline$Head@ad498c
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }

    static class ConsistentHashSelector {
        private final TreeMap<Long, String> virtualInvokers;

        private final int identityHashCode;

        ConsistentHashSelector(List<String> addressList, int replicaNumber, int addressListHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = addressListHashCode;

            //为每一个address生成replicaNumber个虚拟结点
            for (String address : addressList) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(address + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualInvokers.put(m, address);
                    }
                }
            }
        }

        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            return md.digest();
        }

        //一致性hash生成的hashCode的范围是在 0 - MAX_VALUE之间
        //因此为正整数，所以这里要强制转换为long类型，避免出现负数  0xFFFFFFFFL
        static long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 0xFF) << 24 |
                    (long) (digest[2 + idx * 4] & 0xFF) << 16 |
                    (long) (digest[1 + idx * 4] & 0xFF) << 8 |
                    (long) (digest[idx * 4] & 0xFF)) & 4294967295L;
        }

        public String select(String rpcServiceKey) {
            //根据服务生成摘要
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest, 0));
        }

        //根据hashCode选择结点
        public String selectForKey(long hashCode) {
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();
            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }
            return entry.getValue();
        }
    }

}
