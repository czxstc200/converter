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
        lUserID = hCNetSDK.NET_DVR_Login_V30("10.112.239.157",(short)8000,"admin","ydslab215",m_strDeviceInfo);
        if (lUserID.intValue() == -1)
        {
            System.out.println("注册失败");
            return false;
        }
        return true;
    }
}
