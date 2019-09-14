package com.myself.nettychat.channel;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * Description:
 *
 * @author lcc
 * @version 1.0
 * @date 2019-09-14 12:56
 **/
public class ChannelManager {

    /**
     * 存储每一个客户端接入进来时的channel对象
     */
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


}
