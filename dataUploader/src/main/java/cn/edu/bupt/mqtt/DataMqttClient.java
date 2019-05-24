package cn.edu.bupt.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class DataMqttClient {
    public static MqttClient client;

    static{
        try{
            client = new MqttClient(Config.HOST, "data", new MemoryPersistence());
        }catch(Exception e){
            e.printStackTrace();
        }

    }
    public static synchronized void publishData(String token,String data) throws  Exception{
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(token);
        options.setConnectionTimeout(10);
        client.connect(options);
        MqttMessage msg = new MqttMessage(data.getBytes());
        msg.setRetained(false);
        msg.setQos(0);
        client.publish(Config.datatopic, msg);
        client.disconnect();
    }

    public static synchronized  void publishAttribute(String token,String data)throws  Exception{
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(token);
        options.setConnectionTimeout(10);
        client.connect(options);

        MqttMessage msg = new MqttMessage(data.getBytes());
        msg.setRetained(false);
        msg.setQos(0);
        client.publish(Config.attributetopic, msg);
        client.disconnect();
    }
}
