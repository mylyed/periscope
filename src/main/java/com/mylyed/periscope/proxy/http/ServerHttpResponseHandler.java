/*
 * Copyright 2012 The Netty Project
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
package com.mylyed.periscope.proxy.http;

import com.mylyed.periscope.common.ChannelUtil;
import io.netty.channel.*;
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
    public void channelActive(ChannelHandlerContext ctx) {
        //手动读取响应
        ctx.read();
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
        //TODO 记录响应内容
        logger.debug("响应内容：{}", msg);
        clientChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ctx.channel().read();
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
