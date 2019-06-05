package cn.edu.bupt.handler;

import cn.edu.bupt.scanners.Scanner;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

/**
 * @Description: HttpHandler
 * @Author: czx
 * @CreateDate: 2019-05-30 22:33
 * @Version: 1.0
 */
public class HttpHandler extends SimpleChannelInboundHandler<DefaultFullHttpRequest> {

    private Scanner scanner = Scanner.getInstance();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultFullHttpRequest msg) throws Exception {
        String uri = msg.uri();
        System.out.println();
        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(uri.getBytes())));
        ctx.channel().close();
//        String method = msg.method().name();
    }
}
