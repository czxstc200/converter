package cn.edu.bupt.client;

import cn.edu.bupt.mqtt.Config;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttClientFactory extends BasePooledObjectFactory<MqttClient> {

    @Override
    public MqttClient create() throws Exception {
        return new MqttClient(Config.HOST, "data", new MemoryPersistence());
    }

    @Override
    public PooledObject<MqttClient> wrap(MqttClient mqttClient) {
        return new DefaultPooledObject<>(mqttClient);
    }
}
