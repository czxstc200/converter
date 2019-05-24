package cn.edu.bupt.util;

import cn.edu.bupt.data.CameraInfo;
import cn.edu.bupt.mqtt.DataMqttClient;
import com.google.gson.Gson;
import org.json.simple.JSONObject;

public class Publish {

    /*
   发送属性
    */
    public static void sendAttributes(CameraInfo cameraInfo, String token) {
        // TODO Auto-generated method stub
        try{
            Gson gson = new Gson();
            String deviceDataStr = gson.toJson(cameraInfo);
            System.out.println(deviceDataStr);
            //进行发送
            DataMqttClient.publishAttribute(token,deviceDataStr);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    /*
    发送遥测
     */
    public static void sendTelemetries(String token,String key,String value){
        try {
            JSONObject info = new JSONObject();
            info.put(key,value);
            String data = info.toString();
            DataMqttClient.publishData(token,data);
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}
