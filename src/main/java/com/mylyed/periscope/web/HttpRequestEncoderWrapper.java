package com.mylyed.periscope.web;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequestEncoder;

import java.util.List;

/**
 * 用于暴露HttpRequestEncoder的编码方法
 *
 * @author lilei
 * @create 2020-11-28
 **/
public class HttpRequestEncoderWrapper extends HttpRequestEncoder {
    @Override
    public void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) {
        try {
            super.encode(ctx, msg, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
