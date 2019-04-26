package cn.edu.bupt.controller;

import cn.edu.bupt.stream.adapter.RtspVideoAdapter;
import cn.edu.bupt.stream.adapter.VideoAdapter;
import cn.edu.bupt.stream.adapter.VideoAdapterManagement;
import com.sun.jna.NativeLong;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
                          @RequestParam(required = false) Boolean save,
                          @RequestParam(required = false) Boolean usePacket ) throws Exception{
//        String rtspPath = "rtsp://admin:ydslab215@10.112.239.157:554/h264/ch1/main/av_stream";
        String rtmpPath = rtmp==null?"rtmp://localhost/oflaDemo/haikang1":rtmp;
        String rtspPath = rtsp==null?"rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov":rtsp;
        boolean saveVideo = save==null?false:save;
        boolean isUsePacket = usePacket==null?false:usePacket;
        VideoAdapterManagement.startAdapter(new RtspVideoAdapter(rtspPath,rtmpPath,saveVideo,isUsePacket));
        setHeader(response);
        return "{rtsp:'"+rtspPath+"',"+"rtmp:'"+rtmpPath+"',"+"saveVideo:"+saveVideo+",usePacket:"+isUsePacket+"}";
    }

    @ApiOperation(value = "TEST 对视频进行推流")
    @RequestMapping(value = "/convertAll", method = RequestMethod.GET)
    @ResponseBody
    public void converttest(@RequestParam(required = false) Boolean save) throws Exception{
//        String rtspPath = "rtsp://admin:ydslab215@10.112.239.157:554/h264/ch1/main/av_stream";
        boolean b = false;
        if(save!=null&&save==true){
            b = true;
        }
        boolean usePacket = false;
        for(int i = 1;i<=4;i++){
            convert("rtsp://admin:LITFYL@10.112.239.157:554/h264/ch1/main/av_stream","rtmp://10.112.217.199/live360p/test"+i,b,usePacket);
        }
        for(int i = 5;i<=8;i++){
            convert("rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov","rtmp://10.112.217.199/live360p/test"+i,b,usePacket);
        }
    }

    @ApiOperation(value = "TEST 对视频停止推流")
    @RequestMapping(value = "/stopAll", method = RequestMethod.GET)
    @ResponseBody
    public void stopAll() {
//        String rtspPath = "rtsp://admin:ydslab215@10.112.239.157:554/h264/ch1/main/av_stream";
        for(VideoAdapter videoAdapter : VideoAdapterManagement.map.values()){
            videoAdapter.stop();
        }
        VideoAdapterManagement.map = new ConcurrentHashMap<>();
    }



    @ApiOperation(value = "画面抓拍")
    @RequestMapping(value = "/capture", method = RequestMethod.GET)
    @ResponseBody
    public String capture(@RequestParam String rtmp) {
        RtspVideoAdapter videoAdapter = (RtspVideoAdapter)VideoAdapterManagement.getVideoAdapter(rtmp);
        if(videoAdapter.capture()){
            return "抓拍成功";
        }else{
            return "抓拍失败";
        }
    }


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

    @ApiOperation("停止视频推流")
    @RequestMapping(value = "/stopConvert", method = RequestMethod.GET)
    @ResponseBody
    public void stopConvert(@RequestParam String rtmp){
        setHeader(response);
        VideoAdapterManagement.stopAdapter(VideoAdapterManagement.getVideoAdapter(rtmp));
    }

    @ApiOperation("获取录像")
    @RequestMapping(value = "/records", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getRecords(@RequestParam String rtmp) throws Exception{
        setHeader(response);
        RtspVideoAdapter videoAdapter = (RtspVideoAdapter)VideoAdapterManagement.getVideoAdapter(rtmp);
        return videoAdapter.getFiles(rtmp);
    }

    @ApiOperation("获取抓拍")
    @RequestMapping(value = "/captures", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getCaptures(@RequestParam String rtmp) throws Exception{
        setHeader(response);
        RtspVideoAdapter videoAdapter = (RtspVideoAdapter)VideoAdapterManagement.getVideoAdapter(rtmp);
        return videoAdapter.getCaptures(rtmp);
    }

    @ApiOperation("获取所有视频源")
    @RequestMapping(value = "/feedback", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getFeedbacks(){
        setHeader(response);
        List<String> list = new ArrayList<>();
        list.add("rtmp://39.104.186.210/oflaDemo/haikang1/2019_01_18_17_08.flv");
        list.add("rtmp://39.104.186.210/oflaDemo/haikang1/2019_01_18_17_06.flv");
        list.add("rtmp://39.104.186.210/oflaDemo/haikang1/BladeRunner2049.flv");
        list.add("rtmp://39.104.186.210/oflaDemo/haikang1/guardians2.mp4");
        return list;
    }

    //以下是视频控制

    @RequestMapping(value = "/subscribe", method = RequestMethod.GET)
    @ResponseBody
    public boolean subscribe(@RequestParam(required = false) String ip,
                             @RequestParam(required = false) String port) throws Exception{
        setHeader(response);
        if(ip==null||port==null) {
            return HikUtil.subscribe();
        }else{
            return HikUtil.subscribe(ip,Integer.valueOf(port));
        }
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
        setHeader(response);
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
