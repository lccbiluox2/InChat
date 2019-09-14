package com.myself.nettychat.handler;

import com.myself.nettychat.channel.ChannelManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 处理二进制消息
 *
 * @author huan.fu
 * @date 2018/11/8 - 14:37
 */
public class BinaryWebSocketFrameHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {
    private static final Logger log = LoggerFactory.getLogger(BinaryWebSocketFrameHandler.class);

    /**
     * 存储每一个客户端接入进来时的channel对象
     */
    public ChannelGroup channels = ChannelManager.channels;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame msg) throws InterruptedException {
        log.info("服务器接收到二进制消息,消息长度:[{}]", msg.content().capacity());
        ByteBuf byteBuf = Unpooled.directBuffer(msg.content().capacity());
        byteBuf.writeBytes(msg.content());

        for (Channel channel : channels) {
            ctx.writeAndFlush(new BinaryWebSocketFrame(byteBuf));
        }
    }
}