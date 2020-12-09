package com.mylyed.periscope.proxy.http;

import com.mylyed.periscope.common.ChannelUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    final Channel serverChannel;

    protected static final Logger logger = LoggerFactory.getLogger("Client(HTTP) <==> Proxy");

    public ClientHttpRequestHandler(Channel serverChannel) {
        super(false);
        this.serverChannel = serverChannel;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg) {
        //一次请求用一次
        logger.debug("转发HTTP请求：{}", msg);
        //TODO 记录请求信息
        serverChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                logger.debug("写到服务端成功");
            } else {
                future.channel().close();
                ChannelUtil.closeOnFlush(serverChannel);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("出现异常");
        ctx.close();
        ChannelUtil.closeOnFlush(serverChannel);
    }
}
