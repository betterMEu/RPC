package com.rpccenter.registry.zk;

import com.common.enums.PropertiesEnum;
import com.common.util.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author yls91
 */
@Slf4j
public class CuratorUtil {

    public static final String ZK_ROOT = "/ROOT";

    /**
     * 根据serviceName为key 寻找响应服务器地址 会有多个响应的服务器
     * */
    public static final Map<String, List<String>> SERVICE_SERVERS_ADDRESSES = new ConcurrentHashMap<>();

    /**
     * 已经注册的节点
     * */
    public static final Set<String> REGISTRY_NODE = ConcurrentHashMap.newKeySet();

    /**
     * 如果没有配置使用此默认 zk服务器地址端口
     * */
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "120.78.172.214:2181";


    /**
     * 连接zk平台的配置
     */
    private static final int ATTEMPTS = 3;
    private static final int BASE_SLEEP_TIME = 1000;
    private static CuratorFramework zkClient;

    private CuratorUtil() {}

    /**
     * 获取zk客户端
     * */
    public static CuratorFramework getZkClient() {
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }

        //获取配置文件
        Properties properties = PropertiesUtil.readProperties(PropertiesEnum.APPLICATION.getValue());
        String zkAddress;
        if(properties.getProperty(PropertiesEnum.ZK_ADDRESS.getValue()) == null) {
            log.info("没有配置zk服务地址，使用默认" + CuratorUtil.DEFAULT_ZOOKEEPER_ADDRESS);
            zkAddress = CuratorUtil.DEFAULT_ZOOKEEPER_ADDRESS;
        }else {
            zkAddress = properties.getProperty(PropertiesEnum.ZK_ADDRESS.getValue());
        }

        //重试策略
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, ATTEMPTS);
        zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();

        try {
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("连接zk超时");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zkClient;
    }

    /**
     * 服务注册时为服务创建一个服务节点
     * */
    public static void createNode(CuratorFramework zkClient,String path) {
        try{
            if(REGISTRY_NODE.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("server已注册此服务[{}]",path);
            }else {
                //eg: /ROOT/service.FirstServiceGroup2Version2/10.215.55.75:9980
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("服务注册成功。路径：[{}]",path);
            }
            REGISTRY_NODE.add(path);
        } catch (Exception e) {
            log.info("创建节点[{}]失败,请重新注册",path);
        }
    }

    public static List<String> getServiceIP(CuratorFramework zkClient, String serviceName) {
        if(SERVICE_SERVERS_ADDRESSES.containsKey(serviceName)) {
            return SERVICE_SERVERS_ADDRESSES.get(serviceName);
        }

        List<String> result = null;
        String servicePath = ZK_ROOT + "/" + serviceName;
        try {
            result = zkClient.getChildren().forPath(servicePath);
            SERVICE_SERVERS_ADDRESSES.put(serviceName, result);
            registerWatcher(serviceName, zkClient);
        } catch (Exception e) {
            log.error("获取子节点失败[{}]", servicePath);
        }
        return result;
    }



    private static void registerWatcher(String serviceName, CuratorFramework zkClient) throws Exception {
        String servicePath = ZK_ROOT + "/" + serviceName;
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);

        PathChildrenCacheListener pathChildrenCacheListener = (curatorFramework, pathChildrenCacheEvent) -> {
            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
            SERVICE_SERVERS_ADDRESSES.put(serviceName, serviceAddresses);
        };
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
    }

    /**
     * 清空所有注册服务的数据
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        REGISTRY_NODE.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("清理注册服务 [{}] 失败", p);
            }
        });
        log.info("所有注册服务已被清除[{}]", REGISTRY_NODE);
    }

}
