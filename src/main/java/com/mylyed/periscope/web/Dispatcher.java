package com.mylyed.periscope.web;

import com.mylyed.periscope.common.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class Dispatcher {

    private static Map<String, BiFunction<ChannelHandlerContext, FullHttpRequest, FullHttpResponse>> handler =
            new HashMap<String, BiFunction<ChannelHandlerContext, FullHttpRequest, FullHttpResponse>>() {{
                put("/favicon.ico", Dispatcher::favicon);
                put("/", Dispatcher::index);
            }};


    static final HttpRequestEncoderWrapper httpRequestEncoderWrapper = new HttpRequestEncoderWrapper();

    public static FullHttpResponse handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        return handler.getOrDefault(request.uri(), Dispatcher::notFound).apply(ctx, request);
    }

    private static FullHttpResponse notFound(ChannelHandlerContext ctx, HttpRequest request) {
        String notFound = "404 not found";
        ByteBuf buffer = ByteBufUtil.wrappedBuffer(ctx, notFound.getBytes());
        FullHttpResponse response = new DefaultFullHttpResponse(
                request.protocolVersion(), NOT_FOUND, buffer);
        response.headers().set(CONTENT_TYPE, "text/plain;charset=utf-8");
        return response;
    }

    private static FullHttpResponse index(ChannelHandlerContext ctx, FullHttpRequest request) {

        List<Object> out = new ArrayList<>();
        httpRequestEncoderWrapper.encode(ctx, request, out);

        ByteBuf buffer = ctx.alloc().buffer();
        buffer.writeBytes("欢迎使用\n".getBytes());
        for (Object object : out) {
            buffer.writeBytes((ByteBuf) object);
        }
        FullHttpResponse response = new DefaultFullHttpResponse(
                request.protocolVersion(), OK, buffer);
        response.headers().set(CONTENT_TYPE, "text/plain;charset=utf-8");
        return response;
    }

    public static final int HTTP_CACHE_SECONDS = 3600;

    /**
     * 网站图标
     *
     * @param request
     * @return
     */
    private static FullHttpResponse favicon(ChannelHandlerContext ctx, FullHttpRequest request) {
        final FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK, ByteBufUtil.wrappedBuffer(ctx, Favicon.favicon()));
        response.headers().add(CONTENT_TYPE, " image/x-icon");
        response.headers().add(HttpHeaderNames.CACHE_CONTROL, "max-age=" + HTTP_CACHE_SECONDS);
        return response;
    }


}
