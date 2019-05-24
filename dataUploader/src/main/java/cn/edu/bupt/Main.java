package cn.edu.bupt;

import cn.edu.bupt.client.ClientImpl;
import cn.edu.bupt.data.CameraInfo;

public class Main {
    public static void main(String args[]){
        CameraInfo cameraInfo = new CameraInfo("C6Ciiiiiii","rtsp-00");

        ClientImpl client = new ClientImpl();
        client.sendAttributes(cameraInfo);

        client.sendTelemetries(cameraInfo,"key","value11111");

    }
}
