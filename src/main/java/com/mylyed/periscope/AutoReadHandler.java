package com.mylyed.periscope;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author lilei
 * @create 2020-11-28
 **/
@ChannelHandler.Sharable
public class AutoReadHandler extends ChannelInboundHandlerAdapter {

    public static final AutoReadHandler INSTANCE = new AutoReadHandler();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

}
