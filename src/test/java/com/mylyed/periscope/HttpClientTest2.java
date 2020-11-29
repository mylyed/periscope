package com.mylyed.periscope;

import com.mylyed.periscope.proxy.Constant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author lilei
 * @create 2020-11-26
 **/
public class HttpClientTest2 extends SimpleChannelInboundHandler<HttpObject> {
    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        doGet("http://192.168.21.60:35063/user-center-client/ip?ip=127.0.0.1");
    }

    public static void doGet(String httpurl) throws URISyntaxException, InterruptedException {
        URI uriSimple = new URI(httpurl);
        String host = uriSimple.getHost();
        int port = uriSimple.getPort();
        Bootstrap b = new Bootstrap();
        EventLoopGroup group = new NioEventLoopGroup();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("log", new LoggingHandler(LogLevel.INFO));
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpContentDecompressor());//这里要添加解压，不然打印时会乱码
                        pipeline.addLast(new HttpObjectAggregator(Constant.HTTP_OBJECT_AGGREGATOR_MAX_CONTENT_LENGTH));//添加HttpObjectAggregator， HttpClientMsgHandler才会收到FullHttpResponse
                        pipeline.addLast(new HttpClientTest2());
                    }
                });
        System.out.println(String.format("host:%s:%s", host, port));
        Channel channel = b.connect(host, port).sync().channel();
        // Prepare the HTTP request.
        System.out.println("开始构造请求");
        URI uriGet = new URI(httpurl);
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uriGet.getRawPath());
        request.headers().set(HttpHeaderNames.HOST, host);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
        HttpHeaders headers = request.headers();
        headers.set(
                HttpHeaderNames.COOKIE, ClientCookieEncoder.encode(
                        new DefaultCookie("my-cookie", "foo"),
                        new DefaultCookie("another-cookie", "bar"))
        );

        // send request
        channel.writeAndFlush(request);
        // Wait for the server to close the connection.
        channel.closeFuture().sync();
        // Shut down executor threads to exit.
        group.shutdownGracefully();
    }

    private boolean readingChunks;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        System.out.println(msg.getClass());
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;

            System.err.println("STATUS: " + response.status());
            System.err.println("VERSION: " + response.protocolVersion());

            if (!response.headers().isEmpty()) {
                for (CharSequence name : response.headers().names()) {
                    for (CharSequence value : response.headers().getAll(name)) {
                        System.err.println("HEADER: " + name + " = " + value);
                    }
                }
            }

            if (response.status().code() == 200 && HttpUtil.isTransferEncodingChunked(response)) {
                readingChunks = true;
                System.err.println("CHUNKED CONTENT {");
            } else {
                System.err.println("CONTENT {");
            }
        }
        if (msg instanceof HttpContent) {
            HttpContent chunk = (HttpContent) msg;
            System.err.println(chunk.content().toString(CharsetUtil.UTF_8));

            if (chunk instanceof LastHttpContent) {
                if (readingChunks) {
                    System.err.println("} END OF CHUNKED CONTENT");
                } else {
                    System.err.println("} END OF CONTENT");
                }
                readingChunks = false;
            } else {
                System.err.println(chunk.content().toString(CharsetUtil.UTF_8));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.channel().close();
    }
}
