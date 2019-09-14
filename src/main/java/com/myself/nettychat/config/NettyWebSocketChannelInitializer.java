package com.myself.nettychat.config;

import com.myself.nettychat.handler.BinaryWebSocketFrameHandler;
import com.myself.nettychat.handler.TextWebSocketHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


/**
 * @Author:UncleCatMySelf
 * @Email：zhupeijie_java@126.com
 * @QQ:1341933031
 * @Date:Created in 11:00 2018\8\14 0014
 */
@Component
@Qualifier("somethingChannelInitializer")
public class NettyWebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger logger = LoggerFactory.getLogger(NettyWebSocketChannelInitializer.class);

    @Autowired
    private MyWebSocketServerHandler myWebSocketServerHandler;
    @Autowired
    private TextWebSocketHandler textWebSocketHandler;

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // HttpRequestDecoder和HttpResponseEncoder的一个组合，针对http协议进行编解码
        pipeline.addLast(new HttpServerCodec());
        // 分块向客户端写数据，防止发送大文件时导致内存溢出， channel.write(new ChunkedFile(new File("bigFile.mkv")))
        pipeline.addLast(new ChunkedWriteHandler());
        // 将HttpMessage和HttpContents聚合到一个完成的 FullHttpRequest或FullHttpResponse中,具体是FullHttpRequest对象还是FullHttpResponse对象取决于是请求还是响应
        // 需要放到HttpServerCodec这个处理器后面
        pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));

        // webSocket 数据压缩扩展，当添加这个的时候WebSocketServerProtocolHandler的第三个参数需要设置成true
        pipeline.addLast(new WebSocketServerCompressionHandler());
        // 聚合 websocket 的数据帧，因为客户端可能分段向服务器端发送数据
        // https://github.com/netty/netty/issues/1112 https://github.com/netty/netty/pull/1207
        pipeline.addLast(new WebSocketFrameAggregator(10 * 1024 * 1024));
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws",null, true, Integer.MAX_VALUE, false));
        // 自定义处理器 - 处理 web socket 文本消息
        pipeline.addLast(textWebSocketHandler);
        // 自定义处理器 - 处理 web socket 二进制消息
        pipeline.addLast(new BinaryWebSocketFrameHandler());

        logger.info("初始化MyWebSocketServerHandler");
        //这里不能使用new，不然在handler中不能注入依赖
//        pipeline.addLast(myWebSocketServerHandler);

    }

}
