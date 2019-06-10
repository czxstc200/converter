//package cn.edu.bupt;
//
//import be.teletask.onvif.DiscoveryManager;
//import be.teletask.onvif.OnvifManager;
//import be.teletask.onvif.listeners.DiscoveryListener;
//import be.teletask.onvif.listeners.OnvifServicesListener;
//import be.teletask.onvif.models.OnvifDevice;
//import be.teletask.onvif.models.OnvifServices;
//
//import java.util.List;
//
///**
// * @Description: ONVIF
// * @Author: czx
// * @CreateDate: 2019-06-06 19:19
// * @Version: 1.0
// */
//public class ONVIF {
//    public static void main(String[] args) {
//
//        OnvifManager onvifManager = new OnvifManager();
//        OnvifDevice device = new OnvifDevice("10.112.234.40", "czx", "zx19950529");
//        onvifManager.getServices(device, new OnvifServicesListener() {
//            @Override
//            public void onServicesReceived(OnvifDevice onvifDevice, OnvifServices services) {
//                System.out.println("1");
//            }
//        });
//    }
//}
