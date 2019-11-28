package cn.edu.bupt.util;

import cn.edu.bupt.data.CameraInfo;
import cn.edu.bupt.mqtt.DataMqttClient;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

@Slf4j
public class Publish {

    /*
   发送属性
    */
    public static void sendAttributes(CameraInfo cameraInfo, String token) {
        try {
            Gson gson = new Gson();
            //进行发送
            DataMqttClient.publishAttribute(token, gson.toJson(cameraInfo));
        } catch (Exception e) {
            log.error("Send attributes failed, e:", e);
        }
    }

    /*
    发送遥测
     */
    public static void sendTelemetries(String token, String key, String value) {
        try {
            JSONObject info = new JSONObject();
            info.put(key, value);
            DataMqttClient.publishData(token, info.toString());
        } catch (Exception e) {
            log.error("Send telemetries failed, e:", e);
        }

    }
}
