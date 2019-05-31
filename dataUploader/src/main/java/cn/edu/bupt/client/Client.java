package cn.edu.bupt.client;

import cn.edu.bupt.data.CameraInfo;

public interface Client {

    void sendTelemetries(String cameraName,String key,String value);

    void sendAttributes(CameraInfo cameraInfo);
}
