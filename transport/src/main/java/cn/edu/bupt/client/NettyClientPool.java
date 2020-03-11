package cn.edu.bupt.client;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class NettyClientPool extends GenericObjectPool<NettyClient> {

    public NettyClientPool(PooledObjectFactory<NettyClient> factory) {
        super(factory);
    }

    @Override
    public NettyClient borrowObject() throws Exception {
        NettyClient nettyClient = super.borrowObject();
        nettyClient.setClientPool(this);
        return nettyClient;
    }
}
