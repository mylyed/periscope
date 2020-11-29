/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mylyed.periscope.web;

import com.mylyed.periscope.common.ChannelUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

public class WebRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    protected static final Logger log = LoggerFactory.getLogger(WebRequestHandler.class);

    public WebRequestHandler() {
        //不要自动释放
        super(false);
    }

    private boolean isWebRequestHandler = true;



    @Override

    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        log.info("request.uri => {}", request.uri());
        if (!request.uri().startsWith("/")) {
            ctx.fireChannelRead(request);
            isWebRequestHandler = false;
            return;
        }
        log(ctx, request);
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        FullHttpResponse response = Dispatcher.handle(ctx, request);
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
        //手动释放
        ReferenceCountUtil.release(request);
    }

    private static void log(ChannelHandlerContext ctx, HttpRequest request) {
        //获取Host和port
        String hostAndPortStr = request.headers().get("Host");
        if (hostAndPortStr == null) {
            ChannelUtil.closeOnFlush(ctx.channel());
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
        if (isWebRequestHandler) {
            ctx.flush();
            //手动读取读取下一个请求
            ctx.read();
        } else {
            super.channelReadComplete(ctx);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (isWebRequestHandler) {
            ctx.close();
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }
}
