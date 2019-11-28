package cn.edu.bupt.mqtt;

import cn.edu.bupt.client.MqttClientFactory;
import cn.edu.bupt.client.MqttClientPool;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

@Slf4j
public class DataMqttClient {

    private static MqttClientPool mqttClientPool = new MqttClientPool(new MqttClientFactory());

    public static void publishData(String token, String data) throws Exception {
        MqttClient client = clientConnect(token);
        client.publish(Config.TELEMETRY_TOPIC, newMsg(data));
        client.disconnect();
    }

    public static void publishAttribute(String token, String data) throws Exception {
        MqttClient client = clientConnect(token);
        client.publish(Config.ATTRIBUTE_TOPIC, newMsg(data));
        client.disconnect();
    }

    private static MqttClient clientConnect(String token) throws Exception {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(token);
        options.setConnectionTimeout(10);
        MqttClient client = getClient();
        client.setCallback(new DataMessageCallBack());
        client.connect(options);
        return client;
    }

    private static MqttMessage newMsg(String data) {
        MqttMessage msg = new MqttMessage(data.getBytes());
        msg.setRetained(false);
        msg.setQos(0);
        return msg;
    }

    private static MqttClient getClient() throws Exception {
        return mqttClientPool.borrowObject();
    }
}
