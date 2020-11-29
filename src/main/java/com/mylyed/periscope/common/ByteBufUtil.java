package com.mylyed.periscope.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author lilei
 * @create 2020-11-29
 **/
public class ByteBufUtil {

    public static ByteBuf wrappedBuffer(ChannelHandlerContext ctx, byte[] data) {
        //todo 检查参数
        return ctx.alloc().buffer(data.length).writeBytes(data);
    }
}
