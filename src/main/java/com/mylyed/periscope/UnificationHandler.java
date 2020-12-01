
package com.mylyed.periscope;

import com.mylyed.periscope.proxy.Constant;
import com.mylyed.periscope.proxy.socks.Socks4Handler;
import com.mylyed.periscope.proxy.socks.Socks5Handler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v4.Socks4ServerDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ServerEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.List;

/**
 *
 */
public class UnificationHandler extends ByteToMessageDecoder {

    protected static final Logger logger = LoggerFactory.getLogger(UnificationHandler.class);


    static SslContext sslCtx = null;

    static {
        boolean ssl = System.getProperty("ssl") != null;
        ssl = false;
        if (ssl) {
            try {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } catch (CertificateException | SSLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        final int readerIndex = in.readerIndex();
        if (in.writerIndex() == readerIndex) {
            return;
        }

        ChannelPipeline pipeline = ctx.pipeline();
        final byte versionVal = in.getByte(readerIndex);

        SocksVersion version = SocksVersion.valueOf(versionVal);

        switch (version) {
            case SOCKS4a:
                logKnownVersion(ctx, version);
                pipeline.addLast(Socks4ServerEncoder.INSTANCE);
                pipeline.addLast(new Socks4ServerDecoder());
                pipeline.addLast(new Socks4Handler());
                break;
            case SOCKS5:
                logKnownVersion(ctx, version);
                pipeline.addLast(Socks5ServerEncoder.DEFAULT);
                pipeline.addLast(new Socks5InitialRequestDecoder());
                pipeline.addLast(new Socks5Handler());
                break;
            //http或https请求 这里可能有问题
            default:
                logHttp(ctx, versionVal);
                //保留
                out.add(in.retain());
                //https
                if (sslCtx != null) {
                    pipeline.addLast(sslCtx.newHandler(ctx.alloc()));
                }
                //http 请求解码 以及响应编码
                pipeline.addLast(Constant.HTTP_HANDLER_NAME_PREFIX + "HttpServerCodec", new HttpServerCodec());
                //解决压缩问题
                pipeline.addLast(Constant.HTTP_HANDLER_NAME_PREFIX + "HttpContentDecompressor", new HttpContentDecompressor());
                //大数据块
                pipeline.addLast(Constant.HTTP_HANDLER_NAME_PREFIX + "ChunkedWriteHandler", new ChunkedWriteHandler());
                //聚合 http请求
                pipeline.addLast(Constant.HTTP_HANDLER_NAME_PREFIX + "HttpObjectAggregator", new HttpObjectAggregator(Constant.HTTP_OBJECT_AGGREGATOR_MAX_CONTENT_LENGTH));

                pipeline.addLast(new HttpPrepareHandler());
                //TODO
                break;
        }

        pipeline.remove(this);
    }

    private static void logKnownVersion(ChannelHandlerContext ctx, SocksVersion version) {
        logger.debug("{} SOCKS协议版本: {}", ctx.channel(), version);
    }

    private static void logHttp(ChannelHandlerContext ctx, byte versionVal) {
        logger.debug("{} http/https 第一个字节: {}", ctx.channel(), versionVal & 0xFF);

    }
}
