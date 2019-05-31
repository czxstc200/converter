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
        clientConnect(token);
        client.publish(Config.datatopic, newMsg(data));
        client.disconnect();
    }

    public static synchronized  void publishAttribute(String token,String data)throws  Exception{
        clientConnect(token);
        client.publish(Config.attributetopic, newMsg(data));
        client.disconnect();
    }

    private static void clientConnect(String token) throws Exception{
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(token);
        options.setConnectionTimeout(10);
        client.setCallback(new DataMessageCallBack());
        client.connect(options);
    }

    private static MqttMessage newMsg(String data){
        MqttMessage msg = new MqttMessage(data.getBytes());
        msg.setRetained(false);
        msg.setQos(0);
        return msg;
    }
}
