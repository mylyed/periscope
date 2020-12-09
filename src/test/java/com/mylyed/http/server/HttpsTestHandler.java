
package com.mylyed.http.server;

import com.mylyed.periscope.cert.CertUtil;
import com.mylyed.periscope.proxy.http.HttpProxyHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class HttpsTestHandler extends ChannelInboundHandlerAdapter {
    protected static final Logger log = LoggerFactory.getLogger(HttpsTestHandler.class);

    private static final AsciiString CONTENT_TYPE = new AsciiString("Content-Type");
    private static final AsciiString CONTENT_LENGTH = new AsciiString("Content-Length");
    private static final AsciiString CONNECTION = new AsciiString("Connection");
    private static final AsciiString KEEP_ALIVE = new AsciiString("keep-alive");

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private static final byte[] CONTENT = "love you!!!".getBytes();

    PrivateKey caPriKey;
    String subject;
    Date notBefore;
    Date notAfter;
    KeyPair keyPair;
    PrivateKey serverPriKey;
    PublicKey serverPubKey;

    public HttpsTestHandler() {

        try {
            //根证书
            X509Certificate caCert = CertUtil.loadCert(HttpProxyHandler.class.getClassLoader().getResourceAsStream("ca.crt"));
            caPriKey = CertUtil.loadPriKey(HttpProxyHandler.class.getClassLoader().getResourceAsStream("ca_private.der"));

            //读取CA证书使用者信息
            subject = CertUtil.getSubject(caCert);
            //读取CA证书有效时段(server证书有效期超出CA证书的，在手机上会提示证书不安全)
            notBefore = caCert.getNotBefore();
            notAfter = caCert.getNotAfter();
            //CA私钥用于给动态生成的网站SSL证书签证
            //生产一对随机公私钥用于网站SSL证书动态创建
            keyPair = CertUtil.genKeyPair();
            serverPriKey = keyPair.getPrivate();
            serverPubKey = keyPair.getPublic();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;


            if (request.method().equals(HttpMethod.CONNECT)) {

                String hostAndPortStr = request.headers().get(HttpHeaderNames.HOST);
                if (hostAndPortStr == null) {
                    ctx.close();
                    return;
                }
                String[] hostPortArray = hostAndPortStr.split(":");
                String host = hostPortArray[0];
                String portStr = hostPortArray.length == 2 ? hostPortArray[1] : "443";
                int port = Integer.parseInt(portStr);

                ctx.writeAndFlush(
                        new DefaultFullHttpResponse(
                                request.protocolVersion(),
                                new HttpResponseStatus(200, "Connection Established")
                        )).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            log.debug("响应到客户端完成");
                            //颁布假证书
                            X509Certificate x509Certificate = CertUtil.genCert(subject, caPriKey, notBefore, notAfter, serverPubKey, host);
                            log.debug("假证书：{}", host);
                            ChannelPipeline pipeline = future.channel().pipeline();
                            SslContext sslCtx = SslContextBuilder
                                    .forServer(serverPriKey, x509Certificate).build();
                            pipeline.addFirst("ssl", sslCtx.newHandler(ctx.alloc()));
                            pipeline.addFirst("log", new LoggingHandler("https日志"));
                            log.debug("pipeline:{}", pipeline.toMap());
                        }
                    }
                });

            } else {
                ChannelPipeline pipeline = ctx.pipeline();
                log.debug("pipeline2:{}", pipeline.toMap());
                log.debug("请求：{}", request);
                HttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), OK, Unpooled.wrappedBuffer(CONTENT));
                response.headers().set(CONTENT_TYPE, "text/plain");
                response.headers().setInt(CONTENT_LENGTH, CONTENT.length);
                log.debug("构造响应完成：{}", response);
                ctx.write(response).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        log.error("写到客户端完成:{}", future.isSuccess());
                        if (!future.isSuccess()) {
                            Throwable cause = future.cause();
                            cause.printStackTrace();
                        }
                    }
                }).addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            log.debug("msg:{}", msg);
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }



}
