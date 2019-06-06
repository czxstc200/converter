//package cn.edu.bupt.server;
//
//import cn.edu.bupt.adapter.RtspVideoAdapter;
//import cn.edu.bupt.adapter.VideoAdapterManagement;
//import cn.edu.bupt.server.annotation.Controller;
//import cn.edu.bupt.server.annotation.RequestMapping;
//import cn.edu.bupt.server.annotation.RequestParam;
//import cn.edu.bupt.linux.HikUtil;
//
///**
// * @Description: VideoController
// * @Author: czx
// * @CreateDate: 2019-06-06 10:48
// * @Version: 1.0
// */
//@Controller
//public class VideoController {
//
//    @RequestMapping(value = "/convert")
//    public String convert(@RequestParam("rtsp") String rtsp,
//                          @RequestParam("rtmp") String rtmp,
//                          @RequestParam("save") Boolean save,
//                          @RequestParam("usePacket") Boolean usePacket ) throws Exception{
//        VideoAdapterManagement.startAdapter(new RtspVideoAdapter(rtsp,rtmp,save,usePacket));
//        return "{rtsp:'"+rtsp+"',"+"rtmp:'"+rtmp+"',"+"saveVideo:"+save+",usePacket:"+usePacket+"}";
//    }
//
//    @RequestMapping(value = "/record")
//    public String record(@RequestParam("rtmp") String rtmp) throws Exception{
//        RtspVideoAdapter videoAdapter = (RtspVideoAdapter)VideoAdapterManagement.getVideoAdapter(rtmp);
//        boolean isRecording = videoAdapter.isRecording();
//        if(isRecording){
//            videoAdapter.stopRecording();
//            return "停止录制";
//        }else {
//            videoAdapter.startRecording();
//            return "开始录制";
//        }
//    }
//
//    @RequestMapping(value = "/stopConvert")
//    public void stopConvert(@RequestParam("rtmp") String rtmp){
//        VideoAdapterManagement.stopAdapter(VideoAdapterManagement.getVideoAdapter(rtmp));
//    }
//
//    @RequestMapping(value = "/subscribe")
//    public boolean subscribe(@RequestParam("rtmp") String rtmp,@RequestParam("ip") String ip,
//                             @RequestParam("port") String port) throws Exception{
//        if(ip==null||port==null) {
//            return HikUtil.subscribe();
//        }else{
//            return HikUtil.subscribe(rtmp,ip,Integer.valueOf(port));
//        }
//    }
//
//    @RequestMapping(value = "/control")
//    public boolean control(@RequestParam("rtmp") String rtmp,@RequestParam("cmd") String cmd) throws Exception{
//        int command = 0;
//        switch(cmd){
//            case "up":
//                command=21;
//                break;
//            case "down":
//                command=22;
//                break;
//            case "left":
//                command=23;
//                break;
//            case "right":
//                command=24;
//                break;
//            default:
//                break;
//        }
//        if(command==0){
//            return false;
//        }
//        if(rtmp==null){
//            rtmp = "";
//        }
//        HikUtil.control(rtmp,command);
//        return true;
//    }
//
//    @RequestMapping(value = "/setEffect")
//    public boolean setEffect(@RequestParam("rtmp") String rtmp,@RequestParam("channel") int channel,
//                             @RequestParam("bright") int bright,@RequestParam("contrast") int contrast,@RequestParam("saturation") int saturation,@RequestParam("hue") int hue) throws Exception{
//        HikUtil.setEffect(rtmp,channel,bright,contrast,saturation,hue);
//        return true;
//    }
//
//}
