package cn.edu.bupt.client;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.eclipse.paho.client.mqttv3.MqttClient;

public class MqttClientPool extends GenericObjectPool<MqttClient> {

    public MqttClientPool(PooledObjectFactory<MqttClient> factory) {
        super(factory);
    }
}
