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
package com.mylyed.periscope;

import com.mylyed.periscope.proxy.Constant;
import com.mylyed.periscope.proxy.PrepareHandler;
import com.mylyed.periscope.web.WebRequestHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

public class PeriscopeInitializer extends ChannelInitializer<SocketChannel> {

    Logger logger = LoggerFactory.getLogger("代理初始化器");


    @Override
    public void initChannel(SocketChannel ch) {
        logger.debug("initChannel");
        //目的是为了https代理的时候移除
        final Set<ChannelHandler> channelHandlers = new LinkedHashSet<>();
        channelHandlers.add(new LoggingHandler(LogLevel.TRACE));
        //http 请求解码 以及响应编码
        channelHandlers.add(new HttpServerCodec());
        channelHandlers.add(new HttpServerExpectContinueHandler());
        //解决压缩问题
        channelHandlers.add(new HttpContentDecompressor());
        channelHandlers.add(new ChunkedWriteHandler());
        //聚合 http请求
        channelHandlers.add(new HttpObjectAggregator(Constant.HTTP_OBJECT_AGGREGATOR_MAX_CONTENT_LENGTH));
        //
        channelHandlers.add(AutoReadHandler.INSTANCE);
        //web请求
        channelHandlers.add(new WebRequestHandler());
        //代理
        channelHandlers.add(new PrepareHandler(channelHandlers));

        ChannelPipeline pipeline = ch.pipeline();
        for (ChannelHandler channelHandler : channelHandlers) {
            pipeline.addLast(channelHandler);
        }
    }
}
