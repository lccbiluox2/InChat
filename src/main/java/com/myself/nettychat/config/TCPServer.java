package com.myself.nettychat.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

/**
 * @Author:UncleCatMySelf
 * @Email：zhupeijie_java@126.com
 * @QQ:1341933031
 * @Date:Created in 11:00 2018\8\14 0014
 */
@Data
@Component
public class TCPServer {

    @Autowired
    @Qualifier("serverBootstrap")
    private ServerBootstrap serverBootstrap;

    @Autowired
    @Qualifier("tcpServerBootstrap")
    private ServerBootstrap tcpServerBootstrap;

    @Autowired
    @Qualifier("webSocketAddress")
    private InetSocketAddress webPort;

    @Autowired
    @Qualifier("tcpSocketAddress")
    private InetSocketAddress tcpTcpPort;

    private Channel serverChannel;

    private Channel tcpServerChannel;

    public void startWeb() throws Exception {
        serverChannel =  serverBootstrap.bind(webPort).sync().channel().closeFuture().sync().channel();
        String url = "ws://192.168.0.7:8090/ws";
    }

    public void startTcp() throws Exception {
        tcpServerChannel = tcpServerBootstrap.bind(tcpTcpPort).sync().channel().closeFuture().sync().channel();
    }

    @PreDestroy
    public void stop() throws Exception {
        serverChannel.close();
        serverChannel.parent().close();
        tcpServerChannel.close();
        tcpServerChannel.parent().close();
    }
}
