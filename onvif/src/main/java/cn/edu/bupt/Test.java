//package cn.edu.bupt;
//
//import be.teletask.onvif.DiscoveryManager;
//import be.teletask.onvif.listeners.DiscoveryListener;
//import be.teletask.onvif.models.Device;
//
//import java.util.List;
//
///**
// * @Description: Test
// * @Author: czx
// * @CreateDate: 2019-06-08 20:17
// * @Version: 1.0
// */
//public class Test {
//    public static void main(String[] args) {
//        DiscoveryManager manager = new DiscoveryManager();
//        manager.setDiscoveryTimeout(10000);
//        manager.discover(new DiscoveryListener() {
//            @Override
//            public void onDiscoveryStarted() {
//                System.out.println("Discovery started");
//            }
//
//            @Override
//            public void onDevicesFound(List<Device> devices) {
//                for (Device device : devices)
//                    System.out.println("Devices found: " + device.getHostName());
//            }
//        });
//    }
//}
