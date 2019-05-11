package cn.edu.bupt.hikVision.linux;


import com.sun.jna.NativeLong;

/**
 * @Description: 海康视频控制
 * @Author: CZX
 * @CreateDate: 2018/11/30 15:55
 * @Version: 1.0
 */
public class HikUtil {
    public static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;

    public static PlayCtrl playControl = PlayCtrl.INSTANCE;

    public static HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo;

    public static NativeLong lUserID;

    public static NativeLong lPreviewHandle;

    public static boolean subscribe(){
        boolean initSuc = hCNetSDK.NET_DVR_Init();
        if (initSuc != true)
        {
            System.out.println("初始化失败");
            return false;
        }

        m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
        lUserID = hCNetSDK.NET_DVR_Login_V30("10.112.239.157",(short)8000,"admin","LITFYL",m_strDeviceInfo);
        if (lUserID.intValue() == -1)
        {
            System.out.println("注册失败");
            return false;
        }
        System.out.println("lUserID : " + lUserID.intValue());
        System.out.println("SerialNumber : "+new String(m_strDeviceInfo.sSerialNumber));
        System.out.println("Res1 : "+new String(m_strDeviceInfo.byRes1));
        System.out.println("toString : "+m_strDeviceInfo.toString());
        return true;
    }

    public static boolean subscribe(String ip, int port){
        boolean initSuc = hCNetSDK.NET_DVR_Init();
        if (initSuc != true)
        {
            System.out.println("初始化失败");
            return false;
        }

        m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
        lUserID = hCNetSDK.NET_DVR_Login_V30(ip,(short)port,"admin","LITFYL",m_strDeviceInfo);
        if (lUserID.intValue() == -1)
        {
            System.out.println("注册失败");
            return false;
        }
        System.out.println("SerialNumber : "+new String(m_strDeviceInfo.sSerialNumber));
        System.out.println("Res1 : "+new String(m_strDeviceInfo.byRes1));
        System.out.println("toString : "+m_strDeviceInfo.toString());
        return true;
    }

    public static boolean setEffect(int channel,int bright, int contrast,int saturation,int hue){
        hCNetSDK.NET_DVR_SetVideoEffect(lUserID,new NativeLong(channel),bright,contrast,saturation,hue);
        System.out.println("control2 res code : "+hCNetSDK.NET_DVR_GetLastError());
        return true;
    }

    public static void main(String[] args) {
        System.out.println(HikUtil.subscribe());
    }
}
