package cn.edu.bupt.server.handler;

import cn.edu.bupt.server.scanners.Scanner;
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
public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private Scanner scanner = Scanner.getInstance();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        String res = (String)scanner.invokeMethod(msg);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(res.getBytes()));
        response.headers().add("Access-Control-Allow-Origin", "*").add("Access-Control-Allow-Method", "POST,GET");
        ctx.writeAndFlush(response);
        ctx.channel().close();
    }
}
