package cn.edu.bupt;

import cn.edu.bupt.client.Client;
import cn.edu.bupt.client.ClientImpl;
import cn.edu.bupt.data.CameraInfo;

public class Main {
    public static void main(String args[]){
        CameraInfo cameraInfo = new CameraInfo("C6Ciiiiiii","rtsp-00","111");

        Client client = ClientImpl.getClient();
        client.sendAttributes(cameraInfo);

        client.sendTelemetries(cameraInfo.getName(),"key","value11111");

    }
}
