package cn.edu.bupt;

import com.sun.jna.NativeLong;

public class Main {

    public Main(){
        lUserID = new NativeLong(-1);
        lPreviewHandle = new NativeLong(-1);
        g_lVoiceHandle = new NativeLong(-1);
    }

    static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;

    static PlayCtrl playControl = PlayCtrl.INSTANCE;

    public static NativeLong g_lVoiceHandle;//全局的语音对讲句柄

    static boolean bRealPlay;//是否在预览.

    static HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo;

    static NativeLong lUserID;

    static NativeLong lPreviewHandle;

    public static void main(String[] args) {

        boolean initSuc = hCNetSDK.NET_DVR_Init();
        if (initSuc != true)
        {
            System.out.println("初始化失败");
        }

        m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
        lUserID = hCNetSDK.NET_DVR_Login_V30("10.112.239.157",(short)8000,"admin","ydslab215",m_strDeviceInfo);
        if (lUserID.intValue() == -1)
        {
            System.out.println("注册失败");
        }

        boolean startRecord = hCNetSDK.NET_DVR_StartDVRRecord(lUserID, new NativeLong(0xffff), new NativeLong(0L));
        System.out.println("startRecord : "+startRecord);
        try {
            Thread.sleep(2000);
        }catch (Exception e){

        }
        boolean stopRecord = hCNetSDK.NET_DVR_StopDVRRecord(lUserID,new NativeLong(0xffff));
        System.out.println("stopRecord : " + stopRecord);
//        控制
//        NativeLong nativeLong = new NativeLong(1L);
//        hCNetSDK.NET_DVR_PTZControl_Other(lUserID,nativeLong,24,0);
//        try {
//            Thread.sleep(1000);
//        }catch (Exception e){
//
//        }
//        hCNetSDK.NET_DVR_PTZControl_Other(lUserID,nativeLong,24,1);

//        HCNetSDK.NET_DVR_WORKSTATE_V30 devwork=new HCNetSDK.NET_DVR_WORKSTATE_V30();
//        if(!hCNetSDK.NET_DVR_GetDVRWorkState_V30(lUserID, devwork)){
//            //返回Boolean值，判断是否获取设备能力
//            System.out.println("返回设备状态失败");
//        }






//        IntByReference ibrBytesReturned = new IntByReference(0);//获取IP接入配置参数
//        HCNetSDK.NET_DVR_IPPARACFG ipcfg=new HCNetSDK.NET_DVR_IPPARACFG();
//        ipcfg.write();
//        Pointer lpIpParaConfig =ipcfg.getPointer();
//        hCNetSDK.NET_DVR_GetDVRConfig(lUserID,hCNetSDK.NET_DVR_GET_IPPARACFG,new NativeLong(0),lpIpParaConfig,ipcfg.size(),ibrBytesReturned);
//        ipcfg.read();
//        System.out.println("|设备状态："+devwork.dwDeviceStatic);

//        lPreviewHandle = hCNetSDK.NET_DVR_RealPlay_V30(lUserID,
//                null, null, null, true);
//        System.out.println("lPreviewHandle : "+lPreviewHandle);
//        System.out.println(hCNetSDK.NET_DVR_GetLastError());

//        for(int i=0;i< m_strDeviceInfo.byChanNum;i++){
//            System.out.print("Camera"+i+1);//模拟通道号名称
//            System.out.print("|是否录像:"+devwork.struChanStatic[i].byRecordStatic);//0不录像，不录像
//            System.out.print("|信号状态:"+devwork.struChanStatic[i].bySignalStatic);//0正常，1信号丢失
//            System.out.println("|硬件状态:"+devwork.struChanStatic[i].byHardwareStatic);//0正常，1异常
//        }
        hCNetSDK.NET_DVR_Logout(lUserID);
        hCNetSDK.NET_DVR_Cleanup();
    }
}
