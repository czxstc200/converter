package cn.edu.bupt.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import lombok.Data;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT_ENCODING;
import static io.netty.handler.codec.http.HttpHeaderValues.GZIP;
import static io.netty.handler.codec.stomp.StompHeaders.CONTENT_LENGTH;
import static io.netty.handler.codec.stomp.StompHeaders.CONTENT_TYPE;

@Data
public class NettyClient {

    private final Bootstrap bootstrap = new Bootstrap();

    private final NioEventLoopGroup group = new NioEventLoopGroup();

    private Channel channel;

    private final String HOST;

    private final int PORT;

    private final HashMap<Long, Promise> promiseMap = new HashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(0L);

    public NettyClient(String HOST, int PORT) {
        this.HOST = HOST;
        this.PORT = PORT;
        start();
    }

    private void start() {
        try {
            bootstrap.group(group)
                    .remoteAddress(new InetSocketAddress(HOST, PORT))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast(new HttpClientCodec());
                            channel.pipeline().addLast(new HttpObjectAggregator(1024 * 10 * 1024));
                            channel.pipeline().addLast(new HttpContentDecompressor());
                            channel.pipeline().addLast(new ClientHandler(NettyClient.this));
                        }
                    });
            channel = bootstrap.connect().sync().channel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public void post(String url, ByteBuf content, PromiseCallback<FullHttpResponse> promiseCallback) throws Exception {
        FullHttpRequest request = generateRequest(HttpMethod.POST, url, content, promiseCallback);
        channel.pipeline().writeAndFlush(request);

    }

    private FullHttpRequest generateRequest(HttpMethod method, String url, ByteBuf content, PromiseCallback<FullHttpResponse> promiseCallback) {

        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_0, method, url);
        if (content != null) {
            request.headers().set(CONTENT_TYPE, "text/plain");
            request.headers().set(ACCEPT_ENCODING, GZIP);
            request.headers().set(CONTENT_LENGTH, content.readableBytes());
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            request.content().clear().writeBytes(content);
        }
        long id = idGenerator.getAndIncrement();
        request.headers().set("id", id);
        promiseMap.put(id, new Promise(id, promiseCallback));
        return request;
    }

    public static void main(String[] args) throws Exception {
        NettyClient nettyClient = new NettyClient("127.0.0.1", 8080);
        nettyClient.post("/test?content=123",
                Unpooled.wrappedBuffer("hello".getBytes()),
                new PromiseCallback<FullHttpResponse>() {
            @Override
            public void onSuccess(FullHttpResponse response) {
                System.out.println(response.headers().get("id"));
                System.out.println("success");
            }

            @Override
            public void onFailed(FullHttpResponse response) {
                System.out.println("failure");
            }
        });
    }

}
