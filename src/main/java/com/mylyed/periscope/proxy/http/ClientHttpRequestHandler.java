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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHttpRequestHandler extends ChannelInboundHandlerAdapter {

    final Channel serverChannel;

    protected static final Logger logger = LoggerFactory.getLogger("Client(HTTP) <==> Proxy <==> Server");

    public ClientHttpRequestHandler(Channel serverChannel) {
        this.serverChannel = serverChannel;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        //一次请求用一次
        logger.debug("转发HTTP请求：{}", msg);
        //TODO 记录请求信息
        serverChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ctx.channel().read();
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
