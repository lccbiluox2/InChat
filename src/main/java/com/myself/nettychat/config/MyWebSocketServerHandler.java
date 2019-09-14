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
import lombok.experimental.var;
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
@Deprecated
public class MyWebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(MyWebSocketServerHandler.class);

    private static final String WEBSOCKET_PATH = "/wa";

    private WebSocketServerHandshaker handshaker;

    /**
     * 存储每一个客户端接入进来时的channel对象
     */
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
        } else if (msg instanceof WebSocketFrame) {
            //websocket帧类型 已连接
            logger.info("读取到WebSocketFrame类型的web数据");
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }


    /**
     * 处理数据
     *
     * @param ctx
     * @param msg
     */
    private void textWebSocketFrame(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
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
        for (Channel channel : channels) {
            //将当前每个聊天内容进行存储
            if (channel != incoming) {
                channel.writeAndFlush(new TextWebSocketFrame("[" + rName + "]" + rMsg));
            } else {
                channel.writeAndFlush(new TextWebSocketFrame(rMsg + "[" + rName + "]"));
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

        // 判断是否为协议升级
        HttpHeaders headers = req.headers();
        boolean flag1 = headers.containsValue(CONNECTION, HttpHeaderValues.UPGRADE, true);
        String upgrade = headers.get(HttpHeaderNames.UPGRADE);
        boolean flag2 = HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgrade);
        if (!flag1 && !flag2) {
            return;
        }

        // Handshake
        //注意，这条地址别被误导了，其实这里填写什么都无所谓，WS协议消息的接收不受这
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                getWebSocketLocation(req), null, true, 5 * 1024 * 1024);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            // 通过它构造握手响应消息返回给客户端，
            // 同时将WebSocket相关的编码和解码类动态添加到ChannelPipeline中，用于WebSocket消息的编解码，
            // 添加WebSocketEncoder和WebSocketDecoder之后，服务端就可以自动对WebSocket消息进行编解码了
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
            logger.info("读取到文本类型的web数据");
            textWebSocketFrame(ctx, (TextWebSocketFrame) frame);
            // Echo the frame
//            ctx.write(frame.retain());
            return;
        }
        if (frame instanceof BinaryWebSocketFrame) {
            // Echo the frame
            logger.info("读取到二进制数据");
            binaryWebSocketFrame(ctx, (BinaryWebSocketFrame) frame);
            ctx.write(frame.retain());
        }
        if (frame instanceof BinaryWebSocketFrame) {
            //返回客户端
            BinaryWebSocketFrame imgBack = (BinaryWebSocketFrame) frame.copy();
            for (Channel channel : channels) {
                channel.writeAndFlush(imgBack.retain());
            }
            //保存服务器
            BinaryWebSocketFrame img = (BinaryWebSocketFrame) frame;
            ByteBuf byteBuf = img.content();
            try {
                FileOutputStream outputStream = new FileOutputStream("D:\\a.jpg");
                byteBuf.readBytes(outputStream, byteBuf.capacity());
                byteBuf.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void binaryWebSocketFrame(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        System.out.println("二进制数据接收");
        ByteBuf buf = frame.content();

        for (int i = 0; i < buf.capacity(); i++){
            byte b = buf.getByte(i);
            System.out.println("byte:"+b);
        }
    }

    private static void sendHttpResponse(
            ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        // 如果是非Keep-Alive，关闭连接
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


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        String location = req.headers().get(HttpHeaderNames.HOST) + WEBSOCKET_PATH;
        if (WebSocketServer.SSL) {
            return "wss://" + location;
        } else {
            return "ws://" + location;
        }
    }
}
