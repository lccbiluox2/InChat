package com.myself.nettychat.handler;

import com.myself.nettychat.channel.ChannelManager;
import com.myself.nettychat.common.utils.StringUtil;
import com.myself.nettychat.constont.LikeRedisTemplate;
import com.myself.nettychat.constont.LikeSomeCacheTemplate;
import com.myself.nettychat.task.MsgAsyncTesk;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Description:
 *
 * @author lcc
 * @version 1.0
 * @date 2019-09-14 12:21
 **/
@Component
@Qualifier("textWebSocketHandler")
@ChannelHandler.Sharable
public class TextWebSocketHandler  extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(TextWebSocketHandler.class);

    private static final String WEBSOCKET_PATH = "/wa";

    private WebSocketServerHandshaker handshaker;

    /**
     * 存储每一个客户端接入进来时的channel对象
     */
    public  ChannelGroup channels = ChannelManager.channels;

    @Autowired
    private LikeRedisTemplate redisTemplate;
    @Autowired
    private LikeSomeCacheTemplate cacheTemplate;
    @Autowired
    private MsgAsyncTesk msgAsyncTesk;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        logger.info("接收到客户端的消息:[{}]", msg.text());
        Channel incoming = ctx.channel();
        String rName = StringUtil.getName(msg.text());
        String rMsg = StringUtil.getMsg(msg.text());
        if (rMsg.isEmpty()) {
            return;
        }
        //用户登录判断
        if (redisTemplate.check(incoming.id(), rName)) {
            //临时存储聊天数据
            cacheTemplate.save(rName, rMsg);
            //存储随机链接ID与对应登录用户名
            redisTemplate.save(incoming.id(), rName);
            //存储登录用户名与链接实例，方便API调用链接实例
            redisTemplate.saveChannel(rName, incoming);
        } else {
            incoming.writeAndFlush(new TextWebSocketFrame("存在二次登陆，系统已为你自动断开本次链接"));
            channels.remove(ctx.channel());
            ctx.close();
            return;
        }

        InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String ip = inetSocketAddress.getHostName();
        for (Channel channel : channels) {
            //将当前每个聊天内容进行存储
            if (channel != incoming) {
                String send = "[" + rName +" "+ ip+ "] [" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+ "]"  + rMsg;
                channel.writeAndFlush(new TextWebSocketFrame(send));
            } else {
                channel.writeAndFlush(new TextWebSocketFrame(rMsg + "[" + rName + "]"));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        logger.error("服务器发生了异常:", cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            logger.info("web socket 握手成功。");
            WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String requestUri = handshakeComplete.requestUri();
            logger.info("requestUri:[{}]", requestUri);
            String subproTocol = handshakeComplete.selectedSubprotocol();
            logger.info("subproTocol:[{}]", subproTocol);
            handshakeComplete.requestHeaders().forEach(entry -> logger.info("header key:[{}] value:[{}]", entry.getKey(), entry.getValue()));
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        logger.info("handler加入{}", ctx.channel().remoteAddress());
        channels.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        //删除存储池对应实例
        String name = (String) redisTemplate.getName(ctx.channel().id());
        if (name == null) {
            return;
        }
        logger.info("删除对应的handler");
        redisTemplate.deleteChannel(name);
        //删除默认存储对应关系
        redisTemplate.delete(ctx.channel().id());
        channels.remove(ctx.channel());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //在线
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //掉线
        msgAsyncTesk.saveChatMsgTask();
    }




}