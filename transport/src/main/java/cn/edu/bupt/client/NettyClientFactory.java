package cn.edu.bupt.client;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class NettyClientFactory extends BasePooledObjectFactory<NettyClient> {

    private final String HOST;

    private final int PORT;

    public NettyClientFactory(String HOST, int PORT) {
        this.HOST = HOST;
        this.PORT = PORT;
    }

    @Override
    public NettyClient create() {
        return new NettyClient(HOST, PORT, null);
    }

    @Override
    public PooledObject<NettyClient> wrap(NettyClient nettyClient) {
        return new DefaultPooledObject<>(nettyClient);
    }
}
