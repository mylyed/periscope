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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeriscopeInitializer extends ChannelInitializer<SocketChannel> {

    static Logger logger = LoggerFactory.getLogger(PeriscopeInitializer.class);
    private final SslContext sslCtx;

    public PeriscopeInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        logger.debug("initChannel");
        ChannelPipeline pipeline = ch.pipeline();
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }
        pipeline.addLast(new LoggingHandler(LogLevel.TRACE));
        //http 请求解码 以及响应编码
        pipeline.addLast(new HttpServerCodec());
        //解决压缩问题
        pipeline.addLast(new HttpContentDecompressor());
        pipeline.addLast(new ChunkedWriteHandler());
        //聚合 http请求
        pipeline.addLast(new HttpObjectAggregator(Constant.HTTP_OBJECT_AGGREGATOR_MAX_CONTENT_LENGTH));
        pipeline.addLast(new PrepareHandler());
    }
}
