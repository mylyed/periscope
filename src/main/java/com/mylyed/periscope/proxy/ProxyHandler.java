package com.mylyed.periscope.proxy;

import com.mylyed.periscope.PrepareHandler;
import com.mylyed.periscope.common.ChannelUtil;
import com.mylyed.periscope.proxy.http.ClientHttpRequestHandler;
import com.mylyed.periscope.proxy.http.ServerHttpResponseHandler;
import com.mylyed.periscope.proxy.https.ClientHttpsRequestHandler;
import com.mylyed.periscope.proxy.https.ServerHttpsResponseHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

/**
 * @author lilei
 * @create 2020-11-02
 **/
public class ProxyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    static final String LOGGER_NAME = "代理";
    static final Logger loggerHttp = LoggerFactory.getLogger(LOGGER_NAME + "HTTP");
    static final Logger loggerHttps = LoggerFactory.getLogger(LOGGER_NAME + "HTTPS");
    static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private String host;
    private int port;


    public ProxyHandler() {
        //不能自动释放
        super(false);
    }

    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        //开始干活
        logger.debug("代理请求信息：{}", request);

        //TODO 验证账户信息

        setHostPort(ctx, request);
        MDC.put("host", host + ":" + port + "|" + request.uri());

        request.headers().remove(HttpHeaderNames.PROXY_AUTHORIZATION);
        String proxyConnection = request.headers().get(HttpHeaderNames.PROXY_CONNECTION);
        if (Objects.nonNull(proxyConnection)) {
            request.headers().set(HttpHeaderNames.CONNECTION, proxyConnection);
            request.headers().remove(HttpHeaderNames.PROXY_CONNECTION);
        }

        if (request.method().equals(HttpMethod.CONNECT)) {
            //代理https请求
            proxyHttps(ctx, request);
        } else {
            //代理http请求
            proxyHttp(ctx, request);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void setHostPort(ChannelHandlerContext ctx, FullHttpRequest request) {
        String hostAndPortStr = request.headers().get(HttpHeaderNames.HOST);
        if (hostAndPortStr == null) {
            responseError(ctx, request);
            return;
        }
        String[] hostPortArray = hostAndPortStr.split(":");
        host = hostPortArray[0];
        String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "80";
        port = Integer.parseInt(portStr);
    }

    private void proxyHttps(final ChannelHandlerContext ctx, FullHttpRequest request) {


        final Channel clientChannel = ctx.channel();

        ChannelFuture responseFuture = clientChannel.writeAndFlush(
                new DefaultHttpResponse(
                        request.protocolVersion(),
                        new HttpResponseStatus(200, "Connection Established")
                ));

        responseFuture.addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                loggerHttps.debug("CONNECT 响应客户端成功");
                //写成功了
                //建立服务端连接
                Bootstrap serverBootstrap = new Bootstrap();
                serverBootstrap.group(ctx.channel().eventLoop())
                        .channel(ctx.channel().getClass())
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new LoggingHandler("Proxy(HTTPS) <==> Server", LogLevel.TRACE));
                                pipeline.addLast(new ServerHttpsResponseHandler(clientChannel));
                            }
                        })
                        //超时时间10S
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000);

                serverBootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        loggerHttps.debug("和真正的服务器连接好了，准备发送报文");
                        List<String> names = ctx.pipeline().names();
                        try {
                            //移除几个关键性处理器
                            //FIXME 这里有点问题
                            DefaultChannelPipeline pipeline = (DefaultChannelPipeline) ctx.pipeline();
                            pipeline.removeIfExists(HttpServerCodec.class);
                            pipeline.removeIfExists(HttpObjectAggregator.class);
                            pipeline.removeIfExists(PrepareHandler.class);
                        } catch (NoSuchElementException e) {
                            logger.error("names:{}", names);
                            logger.error("调整处理器出错", e);
                            responseError(ctx, request);
                            return;
                        }
                        //加入处理https请求处理器
                        ctx.pipeline().addLast(new ClientHttpsRequestHandler(future.channel()));
                        //客户端自动读
                        clientChannel.config().setAutoRead(true);
                    } else {
                        loggerHttps.debug("服务端建立连接失败：{}:{}", host, port);
                        future.channel().close();
                        ChannelUtil.closeOnFlush(clientChannel);
                    }
                });
            } else {
                responseError(ctx, request);
            }
        });
    }

    /**
     * 代理http请求
     *
     * @param ctx
     * @param request
     */
    private void proxyHttp(ChannelHandlerContext ctx, FullHttpRequest request) {
        final Channel clientChannel = ctx.channel();
        Bootstrap serverBootstrap = new Bootstrap();
        serverBootstrap.group(ctx.channel().eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LoggingHandler("Proxy(HTTP) <==> Server", LogLevel.TRACE));
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpContentDecompressor());
                        pipeline.addLast(new HttpObjectAggregator(Constant.HTTP_OBJECT_AGGREGATOR_MAX_CONTENT_LENGTH));
                        pipeline.addLast(new ServerHttpResponseHandler(clientChannel));
                    }
                })
                //超时时间10秒
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                //不能自动读取
                .option(ChannelOption.AUTO_READ, false);
        serverBootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                loggerHttp.debug("和真正的服务器连接好了，准备发送报文");
                clientChannel.pipeline().addLast(new ClientHttpRequestHandler(future.channel()));
                ctx.fireChannelRead(request);
            } else {
                loggerHttp.debug("连接服务端失败:{}:{}", host, port);
                clientChannel.close();
                future.channel().close();
            }
        });
    }

    private void responseError(ChannelHandlerContext ctx, FullHttpRequest request) {
        ctx.channel().write(
                new DefaultHttpResponse(request.protocolVersion(), INTERNAL_SERVER_ERROR)
        );
        ChannelUtil.closeOnFlush(ctx.channel());
    }
}
