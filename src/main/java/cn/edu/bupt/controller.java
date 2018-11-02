package cn.edu.bupt;

import com.sun.jna.NativeLong;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static cn.edu.bupt.Main.hCNetSDK;

/**
 * Created by CZX on 2018/10/12.
 */
@RestController
public class controller {

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    private static Map<String,Future<String>> resultMap = new ConcurrentHashMap<>();

    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ResponseBody
    public boolean getStatus() throws Exception{
        Future<String> fs = resultMap.get("rtmp://localhost/oflaDemo/hello");
        return fs.isDone();
    }

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    public String convert(@RequestParam(required = false) String rtsp,
                          @RequestParam(required = false) String rtmp) throws Exception{
//        String rtmpPath = "rtmp://localhost/oflaDemo/hello";
//        String rtspPath = "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov";
        String rtmpPath = rtmp==null?"rtmp://localhost/oflaDemo/hello":rtmp;
        String rtspPath = rtsp==null?"rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov":rtsp;
        if(resultMap.containsKey(rtmpPath)){
            return "该rtmp地址已经存在！";
        }
        Future<String> future = executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                convert.begin(rtspPath,rtmpPath);
                return String.valueOf(System.currentTimeMillis());
            }
        });
        resultMap.put(rtmpPath,future);
        return "{rtsp:'"+rtspPath+"',"+"rtmp:'"+rtmpPath+"'}";
    }

    @RequestMapping(value = "/subscribe", method = RequestMethod.GET)
    @ResponseBody
    public boolean subscribe() throws Exception{
        return HikUtil.subscribe();
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    @ResponseBody
    public void logout() throws Exception{
        hCNetSDK.NET_DVR_Logout(HikUtil.lUserID);
        hCNetSDK.NET_DVR_Cleanup();
    }

    @RequestMapping(value = "/control", method = RequestMethod.GET)
    @ResponseBody
    public boolean control(@RequestParam String cmd,
                           @RequestParam int status) throws Exception{
        int command = 0;
        switch(cmd){
            case "up":
                command=21;
                break;
            case "down":
                command=22;
                break;
            case "left":
                command=23;
                break;
            case "right":
                command=24;
                break;
            default:
                break;
        }
        if(command==0){
            return false;
        }
        NativeLong nativeLong = new NativeLong(1L);
        return hCNetSDK.NET_DVR_PTZControl_Other(HikUtil.lUserID,nativeLong,command,status);
    }



//        HCNetSDK.NET_DVR_WORKSTATE_V30 devwork=new HCNetSDK.NET_DVR_WORKSTATE_V30();
//        if(!hCNetSDK.NET_DVR_GetDVRWorkState_V30(lUserID, devwork)){
//            //返回Boolean值，判断是否获取设备能力
//            System.out.println("返回设备状态失败");
//        }
}
