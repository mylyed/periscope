package com.mylyed.periscope.proxy.http;

import com.mylyed.periscope.common.ChannelUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    Logger logger = LoggerFactory.getLogger("HTTP代理响应执行");

    private final Channel clientChannel;

    public ServerHttpResponseHandler(Channel inboundChannel) {
        super(false);
        this.clientChannel = inboundChannel;
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
        //TODO 记录响应内容
        logger.debug("响应信息：{}", msg);
        clientChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                logger.debug("写到客户端成功");
            } else {
                future.channel().close();
                ChannelUtil.closeOnFlush(clientChannel);
            }
        });
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ChannelUtil.closeOnFlush(clientChannel);
        ctx.close();
    }
}
