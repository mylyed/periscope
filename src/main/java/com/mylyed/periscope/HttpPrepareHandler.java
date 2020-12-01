package com.mylyed.periscope;

import com.mylyed.periscope.proxy.http.HttpProxyHandler;
import com.mylyed.periscope.web.WebRequestHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 判断是代理请求还是http请求
 *
 * @author lilei
 * @create 2020-11-30
 **/
public class HttpPrepareHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    protected static final Logger log = LoggerFactory.getLogger(HttpPrepareHandler.class);

    public HttpPrepareHandler() {
        //不要自动释放
        super(false);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        log.info("request.uri => {}", request.uri());
        if (!request.uri().startsWith("/")) {
            log.warn("需要代理的请求");
            ctx.pipeline().addLast(new HttpProxyHandler());
        } else {
            log.warn("web请求");
            ctx.pipeline().addLast(new WebRequestHandler());
        }
        ctx.fireChannelRead(request);
        ctx.pipeline().remove(this);
    }
}
