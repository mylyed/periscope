package com.mylyed.periscope.proxy.socks;

import com.mylyed.periscope.common.ChannelUtil;
import com.mylyed.periscope.proxy.RelayHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lilei
 * @create 2020-12-01
 **/
public class Socks5Handler extends SimpleChannelInboundHandler<Socks5Message> {

    static Logger logger = LoggerFactory.getLogger(Socks5Handler.class);
    private final Bootstrap bootstrap = new Bootstrap();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5Message socksRequest) throws Exception {

        logger.debug("处理Socks5请求{}", socksRequest);

        if (socksRequest instanceof Socks5InitialRequest) {
            logger.debug("初始化请求");
            // TODO auth support example
//            ctx.pipeline().addFirst(new Socks5PasswordAuthRequestDecoder());
//            ctx.write(new Socks5AuthMethod(Socks5AuthMethod.PASSWORD.byteValue()));
            ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
            ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
        } else if (socksRequest instanceof Socks5PasswordAuthRequest) {
            Socks5PasswordAuthRequest socks5PasswordAuthRequest = (Socks5PasswordAuthRequest) socksRequest;
            //TODO
            logger.debug("验证密码 {}:{}", socks5PasswordAuthRequest.username(), socks5PasswordAuthRequest.password());
            ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
            ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
        } else if (socksRequest instanceof Socks5CommandRequest) {
            Socks5CommandRequest socks5CmdRequest = (Socks5CommandRequest) socksRequest;
            if (socks5CmdRequest.type() == Socks5CommandType.CONNECT) {
                //
                connect(ctx, socks5CmdRequest);
                ctx.pipeline().remove(this);
            } else {
                ctx.close();
            }
        } else {
            ctx.close();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 连接目标服务器
     *
     * @param ctx
     * @param request
     * @throws Exception
     */
    protected void connect(ChannelHandlerContext ctx, Socks5CommandRequest request) throws Exception {

        final Channel clientChannel = ctx.channel();

        bootstrap.group(clientChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new RelayHandler(clientChannel));

        logger.debug("代理:{}", request);

        bootstrap.connect(
                request.dstAddr(), request.dstPort())
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        logger.debug("连接服务器成功:{}", request);
                        final Channel serverChannel = future.channel();

                        DefaultSocks5CommandResponse success = new DefaultSocks5CommandResponse(
                                Socks5CommandStatus.SUCCESS,
                                request.dstAddrType(),
                                request.dstAddr(),
                                request.dstPort());

                        //连接服务器成功
                        ChannelFuture responseFuture =
                                clientChannel
                                        .writeAndFlush(success);
                        //响应成功
                        responseFuture.addListener((ChannelFutureListener) channelFuture -> {
                            //响应客户端成功
                            //初始化通道
                            //后续连接直接由中继处理器处理
                            ctx.pipeline().addLast(new RelayHandler(serverChannel));
                        });
                    } else {
                        logger.debug("连接服务器失败:{}", request);
                        ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                                Socks5CommandStatus.FAILURE, request.dstAddrType()));
                        ChannelUtil.closeOnFlush(ctx.channel());
                    }
                });
    }
}
