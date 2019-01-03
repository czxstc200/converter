package cn.edu.bupt.controller;

import cn.edu.bupt.stream.adapter.RtspVideoAdapter;
import cn.edu.bupt.stream.adapter.VideoAdapterManagement;
import com.sun.jna.NativeLong;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static cn.edu.bupt.hikVision.linux.HikUtil.hCNetSDK;
import cn.edu.bupt.hikVision.linux.HikUtil;

import javax.servlet.http.HttpServletResponse;

/**
 * @Description: 视频的controller
 * @Author: CZX
 * @CreateDate: 2018/11/30 10:48
 * @Version: 1.0
 */
@RestController
@CrossOrigin
public class VideoController {

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    private static Map<String,Future<String>> resultMap = new ConcurrentHashMap<>();

    @Autowired
    private HttpServletResponse response;

    @ApiOperation("查看推流状态")
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    @ResponseBody
    public boolean getStatus(@RequestParam String rtmp) throws Exception{
        try {
            setHeader(response);
            return VideoAdapterManagement.getAdapterStatus(rtmp);
        }catch (Exception e){
            throw new Exception("该rtmp地址不存在");
        }

    }

//    @ApiOperation("查看推流状态")
//    @RequestMapping(value = "/status", method = RequestMethod.GET)
//    @ResponseBody
//    public boolean getStatus(@RequestParam String rtmp) throws Exception{
//        try {
//            Future<String> fs = resultMap.get(rtmp);
//            return fs.isDone();
//        }catch (Exception e){
//            throw new Exception("该rtmp地址不存在");
//        }
//
//    }

    @ApiOperation(value = "对视频进行推流")
    @RequestMapping(value = "/convert", method = RequestMethod.GET)
    @ResponseBody
    public String convert(@RequestParam(required = false) String rtsp,
                          @RequestParam(required = false) String rtmp,
                          @RequestParam(required = false) Boolean save ) throws Exception{
//        String rtspPath = "rtsp://admin:ydslab215@10.112.239.157:554/h264/ch1/main/av_stream";
        String rtmpPath = rtmp==null?"rtmp://localhost/oflaDemo/haikang1":rtmp;
        String rtspPath = rtsp==null?"rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov":rtsp;
        boolean saveVideo = save==null?false:save;
        VideoAdapterManagement.startAdapter(new RtspVideoAdapter(rtspPath,rtmpPath,saveVideo));
        setHeader(response);
        return "{rtsp:'"+rtspPath+"',"+"rtmp:'"+rtmpPath+"',"+",saveVideo:"+saveVideo+"}";
    }

//    @ApiOperation(value = "对视频进行推流")
//    @RequestMapping(value = "/convert", method = RequestMethod.GET)
//    @ResponseBody
//    public String convert(@RequestParam(required = false) String rtsp,
//                          @RequestParam(required = false) String rtmp,
//                          @RequestParam(required = false) Integer audio,
//                          @RequestParam(required = false) Boolean save ) throws Exception{
////        String rtspPath = "rtsp://admin:ydslab215@10.112.239.157:554/h264/ch1/main/av_stream";
//        String rtmpPath = rtmp==null?"rtmp://10.112.17.185/oflaDemo/haikang1":rtmp;
//        String rtspPath = rtsp==null?"rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov":rtsp;
//        int audioRecord = audio==null?1:audio;
//        boolean saveVideo = save==null?false:save;
//        if(resultMap.containsKey(rtmpPath)){
//            throw new Exception("该rtmp地址已经存在!");
//        }
//        Future<String> future = executorService.submit(new Callable<String>() {
//            @Override
//            public String call() throws Exception {
//                VideoConverter.convert(rtspPath,rtmpPath,audioRecord,saveVideo);
//                return String.valueOf(System.currentTimeMillis());
//            }
//        });
//        resultMap.put(rtmpPath,future);
//        return "{rtsp:'"+rtspPath+"',"+"rtmp:'"+rtmpPath+"',"+"audio:"+String.valueOf(audioRecord)+",saveVideo:"+String.valueOf(saveVideo)+"}";
//    }

    @ApiOperation("视频录制")
    @RequestMapping(value = "/record", method = RequestMethod.GET)
    @ResponseBody
    public String record(@RequestParam String rtmp) throws Exception{
        RtspVideoAdapter videoAdapter = (RtspVideoAdapter)VideoAdapterManagement.getVideoAdapter(rtmp);
        boolean isRecording = videoAdapter.isRecording();
        setHeader(response);
        if(isRecording){
            videoAdapter.stopRecording();
            return "停止录制";
        }else {
            videoAdapter.startRecording();
            return "开始录制";
        }
    }

//    @ApiOperation("视频录制")
//    @RequestMapping(value = "/record", method = RequestMethod.GET)
//    @ResponseBody
//    public String record(@RequestParam String rtmp) throws Exception{
//        return VideoConverter.record(rtmp);
//    }

    @ApiOperation("停止视频推流")
    @RequestMapping(value = "/stopConvert", method = RequestMethod.GET)
    @ResponseBody
    public void stopConvert(@RequestParam String rtmp){
        setHeader(response);
        VideoAdapterManagement.stopAdapter(VideoAdapterManagement.getVideoAdapter(rtmp));
    }

//    @ApiOperation("停止视频推流")
//    @RequestMapping(value = "/stopConvert", method = RequestMethod.GET)
//    @ResponseBody
//    public void stopConvert(@RequestParam String rtmp) throws Exception{
//        VideoConverter.stopConvert(rtmp);
//        resultMap.remove(rtmp);
//    }

    @ApiOperation("获取录像")
    @RequestMapping(value = "/records", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getRecords(@RequestParam String rtmp) throws Exception{
        setHeader(response);
        RtspVideoAdapter videoAdapter = (RtspVideoAdapter)VideoAdapterManagement.getVideoAdapter(rtmp);
        return videoAdapter.getFiles(rtmp);
    }

//    @ApiOperation("获取录像")
//    @RequestMapping(value = "/records", method = RequestMethod.GET)
//    @ResponseBody
//    public List<String> getRecords(@RequestParam String rtmp) throws Exception{
//        return VideoConverter.getFiles(rtmp);
//    }

    //以下是视频控制

    @RequestMapping(value = "/subscribe", method = RequestMethod.GET)
    @ResponseBody
    public boolean subscribe() throws Exception{
        setHeader(response);
        return HikUtil.subscribe();
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    @ResponseBody
    public void logout() throws Exception{
        setHeader(response);
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
        hCNetSDK.NET_DVR_PTZControl_Other(HikUtil.lUserID,nativeLong,command,0);
        Thread.sleep(2000);
        hCNetSDK.NET_DVR_PTZControl_Other(HikUtil.lUserID,nativeLong,command,1);
        setHeader(response);
        return true;
    }

    public void setHeader(HttpServletResponse response){
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Method", "POST,GET");
    }
}
