package cn.edu.bupt;

import be.teletask.onvif.DiscoveryManager;
import be.teletask.onvif.OnvifManager;
import be.teletask.onvif.listeners.DiscoveryListener;
import be.teletask.onvif.listeners.OnvifDeviceInformationListener;
import be.teletask.onvif.listeners.OnvifServicesListener;
import be.teletask.onvif.models.Device;
import be.teletask.onvif.models.OnvifDevice;
import be.teletask.onvif.models.OnvifDeviceInformation;
import be.teletask.onvif.models.OnvifServices;

import java.util.List;

/**
 * @Description: ONVIF
 * @Author: czx
 * @CreateDate: 2019-05-28 16:21
 * @Version: 1.0
 */
public class ONVIF {
    public static void main(String[] args) {
        DiscoveryManager manager = new DiscoveryManager();
        manager.setDiscoveryTimeout(10000);
        manager.discover(new DiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {
                System.out.println("Discovery started");
            }

            @Override
            public void onDevicesFound(List<Device> devices) {
                for (Device device : devices)
                    System.out.println("Devices found: " + device.getHostName());
            }
        });

    }
}
