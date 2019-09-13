package com.myself.nettychat.config;

import com.myself.nettychat.common.utils.StringUtil;
import com.myself.nettychat.constont.LikeRedisTemplate;
import com.myself.nettychat.constont.LikeSomeCacheTemplate;
import com.myself.nettychat.task.MsgAsyncTesk;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Description:
 *
 * @author lcc
 * @version 1.0
 * @date 2019-09-13 13:29
 **/
@Component
@Qualifier("myWebSocketServerHandler")
@ChannelHandler.Sharable
public class MyWebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(MyWebSocketServerHandler.class);

    private static final String WEBSOCKET_PATH = "/websocket";

    private WebSocketServerHandshaker handshaker;

    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Autowired
    private LikeRedisTemplate redisTemplate;
    @Autowired
    private LikeSomeCacheTemplate cacheTemplate;
    @Autowired
    private MsgAsyncTesk msgAsyncTesk;



    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if(msg instanceof TextWebSocketFrame){
            logger.info("读取到文本类型的web数据");
            textWebSocketFrame(ctx, (TextWebSocketFrame) msg);
        } else if (msg instanceof WebSocketFrame) {
            //websocket帧类型 已连接
            logger.info("读取到WebSocketFrame类型的web数据");
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }


    /**
     * 处理数据
     * @param ctx
     * @param msg
     */
    private void textWebSocketFrame(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        Channel incoming = ctx.channel();
        String rName = StringUtil.getName(msg.text());
        String rMsg = StringUtil.getMsg(msg.text());
        if (rMsg.isEmpty()){
            return;
        }
        //用户登录判断
        if (redisTemplate.check(incoming.id(),rName)){
            //临时存储聊天数据
            cacheTemplate.save(rName,rMsg);
            //存储随机链接ID与对应登录用户名
            redisTemplate.save(incoming.id(),rName);
            //存储登录用户名与链接实例，方便API调用链接实例
            redisTemplate.saveChannel(rName,incoming);
        }else{
            incoming.writeAndFlush(new TextWebSocketFrame("存在二次登陆，系统已为你自动断开本次链接"));
            channels.remove(ctx.channel());
            ctx.close();
            return;
        }
        for (Channel channel : channels) {
            //将当前每个聊天内容进行存储
            if (channel != incoming){
                channel.writeAndFlush(new TextWebSocketFrame( "[" + rName + "]" + rMsg));
            } else {
                channel.writeAndFlush(new TextWebSocketFrame(rMsg + "[" + rName + "]" ));
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // Handle a bad request.
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, Unpooled.EMPTY_BUFFER));
            return;
        }

        // Allow only GET methods.
        if (!GET.equals(req.method())) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN, Unpooled.EMPTY_BUFFER));
            return;
        }

        // Send the demo page and favicon.ico
        if ("/".equals(req.uri())) {
            ByteBuf content = WebSocketServerBenchmarkPage.getContent(getWebSocketLocation(req));
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);

            res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
            HttpUtil.setContentLength(res, content.readableBytes());

            sendHttpResponse(ctx, req, res);
            return;
        }
        if ("/favicon.ico".equals(req.uri())) {
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, Unpooled.EMPTY_BUFFER);
            sendHttpResponse(ctx, req, res);
            return;
        }

        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), null, true, 5 * 1024 * 1024);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }


    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (frame instanceof TextWebSocketFrame) {
            // Echo the frame
            ctx.write(frame.retain());
            return;
        }
        if (frame instanceof BinaryWebSocketFrame) {
            // Echo the frame
            ctx.write(frame.retain());
        }
        if(frame instanceof BinaryWebSocketFrame){
            //返回客户端
            BinaryWebSocketFrame imgBack= (BinaryWebSocketFrame) frame.copy();
            for (Channel channel : channels){
                channel.writeAndFlush(imgBack.retain());
            }
            //保存服务器
            BinaryWebSocketFrame img= (BinaryWebSocketFrame) frame;
            ByteBuf byteBuf=img.content();
            try {
                FileOutputStream outputStream=new FileOutputStream("D:\\a.jpg");
                byteBuf.readBytes(outputStream,byteBuf.capacity());
                byteBuf.clear();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static void sendHttpResponse(
            ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            // Tell the client we're going to close the connection.
            res.headers().set(CONNECTION, CLOSE);
            ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
        } else {
            if (req.protocolVersion().equals(HTTP_1_0)) {
                res.headers().set(CONNECTION, KEEP_ALIVE);
            }
            ctx.writeAndFlush(res);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        String location =  req.headers().get(HttpHeaderNames.HOST) + WEBSOCKET_PATH;
        if (WebSocketServer.SSL) {
            return "wss://" + location;
        } else {
            return "ws://" + location;
        }
    }
}
