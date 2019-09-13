package com.myself.nettychat.config;

import com.myself.nettychat.common.properties.InitNetty;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Author:UncleCatMySelf
 * @Email：zhupeijie_java@126.com
 * @QQ:1341933031
 * @Date:Created in 11:00 2018\8\14 0014
 */
@Component
public class NettyConfig {

    private static final Logger logger = LoggerFactory.getLogger(NettyConfig.class);

    @Autowired
    private InitNetty nettyAccountConfig;

    @Autowired
    @Qualifier("somethingChannelInitializer")
    private NettyWebSocketChannelInitializer nettyWebSocketChannelInitializer;


    @Bean(name = "bossGroup", destroyMethod = "shutdownGracefully")
    public NioEventLoopGroup bossGroup(){
        int number = nettyAccountConfig.getBossThread();
        logger.info("初始化bossGroup，线程数：{}",number);
        return new NioEventLoopGroup(number);
    }

    @Bean(name = "workerGroup", destroyMethod = "shutdownGracefully")
    public NioEventLoopGroup workerGroup(){
        int number = nettyAccountConfig.getWorkerThread();
        logger.info("初始化 workerGroup，线程数：{}",number);
        return new NioEventLoopGroup(number);
    }

    @Bean(name = "webSocketAddress")
    public InetSocketAddress tcpPost(){
        int port = nettyAccountConfig.getWebport();
        logger.info("初始化 webSocketAddress，端口：{}",port);
        return new InetSocketAddress(port);
    }

    @Bean(name = "tcpChannelOptions")
    public Map<ChannelOption<?>, Object> tcpChannelOptions(){
        Map<ChannelOption<?>, Object> options = new HashMap<ChannelOption<?>, Object>();
        options.put(ChannelOption.TCP_NODELAY,nettyAccountConfig.isNodelay());
        options.put(ChannelOption.SO_KEEPALIVE, nettyAccountConfig.isKeepalive());
        options.put(ChannelOption.SO_BACKLOG, nettyAccountConfig.getBacklog());
        options.put(ChannelOption.SO_REUSEADDR,nettyAccountConfig.isReuseaddr());
        logger.info("设置channel属性");
        return options;
    }

    @Bean(name = "serverBootstrap")
    public ServerBootstrap bootstrap(){
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup(), workerGroup())
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(nettyWebSocketChannelInitializer);
        Map<ChannelOption<?>, Object> tcpChannelOptions = tcpChannelOptions();
        Set<ChannelOption<?>> keySet = tcpChannelOptions.keySet();
        for (@SuppressWarnings("rawtypes") ChannelOption option : keySet) {
            b.option(option, tcpChannelOptions.get(option));
        }
        logger.info("初始化 serverBootstrap");
        return b;
    }
}
