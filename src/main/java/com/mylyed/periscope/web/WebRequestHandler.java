package com.mylyed.periscope.web;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

public class WebRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    protected static final Logger log = LoggerFactory.getLogger(WebRequestHandler.class);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        log(ctx, request);
        DecoderResult decoderResult = request.decoderResult();
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        FullHttpResponse response;
        if (decoderResult.isFailure()) {
            //请求有问题
            keepAlive = false;
            response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.BAD_REQUEST);
        } else {
            response = Dispatcher.handle(ctx, request);
        }
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
        if (keepAlive) {
            if (!request.protocolVersion().isKeepAliveDefault()) {
                response.headers().set(CONNECTION, KEEP_ALIVE);
            }
        } else {
            response.headers().set(CONNECTION, CLOSE);
        }
        ChannelFuture f = ctx.write(response);
        if (!keepAlive) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 记录日志
     *
     * @param ctx
     * @param request
     */
    private static void log(ChannelHandlerContext ctx, HttpRequest request) {
        //获取Host和port
        String hostAndPortStr = request.headers().get(HttpHeaderNames.HOST);
        if (hostAndPortStr == null || hostAndPortStr.trim().length() == 0) {
            log.warn("请求头没有Host信息:{}", request);
            return;
        }
        String[] hostPortArray = hostAndPortStr.split(":");
        String host = hostPortArray[0];
        String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
        int port = Integer.parseInt(portStr);
        String clientHostname = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        log.info("clientHostname:{} method:{} uri:{} hostPort:{}", clientHostname, request.method(), request.uri(), String.format("{%s:%s}", host, port));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
