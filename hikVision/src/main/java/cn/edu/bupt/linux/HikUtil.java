package cn.edu.bupt.linux;


import cn.edu.bupt.client.ClientImpl;
import cn.edu.bupt.data.CameraInfo;
import com.sun.jna.NativeLong;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: 海康视频控制
 * @Author: CZX
 * @CreateDate: 2018/11/30 15:55
 * @Version: 1.0
 */
@Slf4j
public class HikUtil {
    public static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;

    public static PlayCtrl playControl = PlayCtrl.INSTANCE;

    public static HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo;

    public static Map<String,NativeLong> UserIDMap = new ConcurrentHashMap<>();

    public static boolean subscribe(){
        return subscribe("","10.112.239.157",8000);
    }

    public static boolean subscribe(String rtmp,String ip, int port){
        boolean initSuc = hCNetSDK.NET_DVR_Init();
        if (initSuc != true)
        {
            log.error("初始化失败");
            return false;
        }

        m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
        NativeLong lUserID = hCNetSDK.NET_DVR_Login_V30(ip,(short)port,"admin","LITFYL",m_strDeviceInfo);
        if (lUserID.intValue() == -1)
        {
            log.error("注册失败");
            return false;
        }
        log.info("SerialNumber : "+new String(m_strDeviceInfo.sSerialNumber));
        try {
            ClientImpl client = new ClientImpl();
            client.sendTelemetries(new CameraInfo(new String(m_strDeviceInfo.sSerialNumber), "rtsp"), "serialNumber", new String(m_strDeviceInfo.sSerialNumber));

        }catch (Exception e){
            System.out.println("Upload failed");
        }
        UserIDMap.put(rtmp,lUserID);
        return true;
    }

    public static boolean setEffect(String rtmp,int channel,int bright, int contrast,int saturation,int hue){
        hCNetSDK.NET_DVR_SetVideoEffect(getUserId(rtmp),new NativeLong(channel),bright,contrast,saturation,hue);
        log.info("SetEffect res code : "+hCNetSDK.NET_DVR_GetLastError());
        return true;
    }

    public static boolean capture(String rtmp,String filename){
        HCNetSDK.NET_DVR_JPEGPARA jpegpara = new HCNetSDK.NET_DVR_JPEGPARA();
        jpegpara.wPicQuality = 0;
        jpegpara.wPicSize = 0;
        boolean res = hCNetSDK.NET_DVR_CaptureJPEGPicture(getUserId(rtmp),getUserId(rtmp),jpegpara,filename);
        log.info("capture res code : "+hCNetSDK.NET_DVR_GetLastError());
        return res;
    }

    public static NativeLong getUserId(String rtmp){
        return UserIDMap.get(rtmp);
    }

    public static void control(String rtmp,int command){
        NativeLong nativeLong = new NativeLong(1L);
        hCNetSDK.NET_DVR_PTZControl_Other(HikUtil.getUserId(rtmp),nativeLong,command,0);
        System.out.println("res code : "+hCNetSDK.NET_DVR_GetLastError());
        try {
            Thread.sleep(2000);
        }catch (Exception e){

        }
        hCNetSDK.NET_DVR_PTZControl_Other(HikUtil.getUserId(rtmp),nativeLong,command,1);
        System.out.println("res code : "+hCNetSDK.NET_DVR_GetLastError());
    }

}
