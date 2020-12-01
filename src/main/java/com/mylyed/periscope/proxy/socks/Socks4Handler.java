package com.mylyed.periscope.proxy.socks;

import com.mylyed.periscope.common.ChannelUtil;
import com.mylyed.periscope.proxy.RelayHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v4.DefaultSocks4CommandResponse;
import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import io.netty.handler.codec.socksx.v4.Socks4CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lilei
 * @create 2020-12-01
 **/
public class Socks4Handler extends SimpleChannelInboundHandler<Socks4CommandRequest> {

    static Logger logger = LoggerFactory.getLogger(Socks4Handler.class);

    private final Bootstrap bootstrap = new Bootstrap();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks4CommandRequest request) throws Exception {

        Channel clientChannel = ctx.channel();

        bootstrap.group(clientChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new RelayHandler(clientChannel));

        logger.debug("代理 {}", request);

        bootstrap.connect(
                request.dstAddr(), request.dstPort())
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        logger.debug("连接服务器成功:{}", request);
                        final Channel outboundChannel = future.channel();
                        //连接服务器成功
                        ChannelFuture responseFuture =
                                clientChannel
                                        .writeAndFlush(new DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS));
                        //响应成功
                        responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                            //响应客户端成功
                            //初始化通道
                            ctx.pipeline().remove(Socks4Handler.this);
                            //后续连接直接由中继处理器处理
                            ctx.pipeline().addLast(new RelayHandler(outboundChannel));
                        });
                    } else {
                        logger.debug("连接服务器失败:{}", request);
                        ctx.channel().writeAndFlush(
                                new DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED)
                        );
                        ChannelUtil.closeOnFlush(ctx.channel());
                    }
                });
    }
}
