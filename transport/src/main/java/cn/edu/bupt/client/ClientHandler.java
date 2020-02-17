package cn.edu.bupt.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;

public class ClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private NettyClient nettyClient;

    public ClientHandler(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpResponse httpResponse) {
        Long id = Long.valueOf(httpResponse.headers().get("id"));
        Promise response = nettyClient.getPromiseMap().get(id);
        nettyClient.getPromiseMap().remove(id);
        if (response == null) {
            // log
            return;
        }
        response.onComplete(httpResponse);
    }
}
