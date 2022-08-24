package com.rpccenter.remote.Netty;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author yls91
 */
public class ChannelProvider {

    private final Map<String, Channel> channelMap;
    public ChannelProvider() {
        channelMap = new ConcurrentHashMap<>();
    }

    public Channel get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        if(channelMap.containsKey(key)) {
            Channel channel = channelMap.get(key);
            if(channel != null && channel.isActive()) {
                return channel;
            }else {
                channelMap.remove(key);
            }
        }
        return null;
    }

    public void put(InetSocketAddress inetSocketAddress, Channel channel) {
        String key = inetSocketAddress.toString();
        channelMap.put(key, channel);
    }

}
