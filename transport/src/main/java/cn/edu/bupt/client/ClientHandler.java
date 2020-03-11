package cn.edu.bupt.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;

public class ClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private NettyClient nettyClient;

    private NettyClientPool clientPool;

    public ClientHandler(NettyClient nettyClient, NettyClientPool clientPool) {
        this.nettyClient = nettyClient;
        this.clientPool = clientPool;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpResponse httpResponse) {
        Long id = Long.valueOf(httpResponse.headers().get("id"));
        Promise response = nettyClient.getPromiseMap().get(id);
        if (response == null) {
            // log
            return;
        }
        nettyClient.getPromiseMap().remove(id);
        response.onComplete(httpResponse);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (clientPool != null) {
            clientPool.invalidateObject(nettyClient);
        }
        super.channelInactive(ctx);
    }
}
