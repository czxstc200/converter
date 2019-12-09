package cn.edu.bupt.controller;

import cn.edu.bupt.adapter.RTSPVideoAdapter;
import cn.edu.bupt.discovery.DeviceDiscovery;
import cn.edu.bupt.linux.HikUtil;
import cn.edu.bupt.soap.OnvifDevice;
import cn.edu.bupt.util.Constants;
import cn.edu.bupt.adapter.VideoAdapterManagement;
import cn.edu.bupt.util.DirUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;

@RestController
@CrossOrigin
public class VideoController {

    @Autowired
    private HttpServletResponse response;

    @Autowired
    private VideoAdapterManagement<RTSPVideoAdapter> videoAdapterManagement;

    @ApiOperation("查看推流状态")
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    @ResponseBody
    public boolean getStatus(@RequestParam String rtmp) throws Exception {
        try {
            setHeader(response);
            return videoAdapterManagement.getAdapterStatus(rtmp);
        } catch (Exception e) {
            throw new Exception("该rtmp地址不存在");
        }
    }

    @ApiOperation(value = "对视频进行推流")
    @RequestMapping(value = "/convert", method = RequestMethod.GET)
    @ResponseBody
    public String convert(@RequestParam String rtsp,
                          @RequestParam String rtmp,
                          @RequestParam(required = false) Boolean save,
                          @RequestParam(required = false) Boolean usePacket) throws Exception {
        String rtmpPath = rtmp == null ? "rtmp://localhost/oflaDemo/haikang1" : rtmp;
        String rtspPath = rtsp == null ? "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov" : rtsp;
        boolean saveVideo = save == null ? false : save;
        boolean isUsePacket = usePacket == null ? true : usePacket;
        videoAdapterManagement.startAdapter(new RTSPVideoAdapter(rtspPath, rtmpPath, videoAdapterManagement, isUsePacket));
        setHeader(response);
        return "{rtsp:'" + rtspPath + "'," + "rtmp:'" + rtmpPath + "'," + "saveVideo:" + saveVideo + ",usePacket:" + isUsePacket + "}";
    }

    @ApiOperation("通过ip推流")
    @RequestMapping(value = "/convertWithIp", method = RequestMethod.GET)
    @ResponseBody
    public String convertWithIp(@RequestParam String ip, @RequestParam String username,
                                @RequestParam String password, @RequestParam String rtmp,
                                @RequestParam(required = false) Boolean save,
                                @RequestParam(required = false) Boolean usePacket) throws Exception {
        setHeader(response);
        boolean saveVideo = save == null ? false : save;
        boolean isUsePacket = usePacket == null ? true : usePacket;
        OnvifDevice device = new OnvifDevice(ip, username, password, false);
        String rtsp = device.getMedia().getRTSPStreamUri(device.getDevices().getProfiles().get(0).getToken());
        String rtspPath = rtsp.replace("rtsp://", "rtsp://" + username + ":" + password + "@");
        videoAdapterManagement.startAdapter(new RTSPVideoAdapter(rtspPath, rtmp, videoAdapterManagement, isUsePacket));
        return "{rtsp:'" + rtspPath + "'," + "rtmp:'" + rtmp + "'," + "saveVideo:" + saveVideo + ",usePacket:" + isUsePacket + "}";
    }

    @ApiOperation(value = "画面抓拍")
    @RequestMapping(value = "/capture", method = RequestMethod.GET)
    @ResponseBody
    public String capture(@RequestParam String rTMP) {
        RTSPVideoAdapter videoAdapter = videoAdapterManagement.getVideoAdapter(rTMP);
        if (videoAdapter.capture()) {
            return "抓拍成功";
        } else {
            return "抓拍失败";
        }
    }

    @ApiOperation(value = "画面抓拍")
    @RequestMapping(value = "/capture2", method = RequestMethod.GET)
    @ResponseBody
    public Boolean capture2() {
        return HikUtil.capture("", Constants.getRootDir() + System.currentTimeMillis() + ".jpeg");
    }


    @ApiOperation("视频录制")
    @RequestMapping(value = "/record", method = RequestMethod.GET)
    @ResponseBody
    public String record(@RequestParam String rtmp) throws Exception {
        RTSPVideoAdapter videoAdapter = (RTSPVideoAdapter) videoAdapterManagement.getVideoAdapter(rtmp);
        boolean isRecording = videoAdapter.isRecording();
        setHeader(response);
        if (isRecording) {
            videoAdapter.stopRecording();
            return "停止录制";
        } else {
            videoAdapter.startRecording();
            return "开始录制";
        }
    }

    @ApiOperation("视频录制")
    @RequestMapping(value = "/re", method = RequestMethod.GET)
    @ResponseBody
    public String restartRecord(@RequestParam String rtmp) throws Exception {
        RTSPVideoAdapter videoAdapter = (RTSPVideoAdapter) videoAdapterManagement.getVideoAdapter(rtmp);
        boolean isRecording = videoAdapter.isRecording();
        setHeader(response);
        if (isRecording) {
            videoAdapter.restartRecording(Constants.getRootDir() + System.currentTimeMillis() + ".flv");
            return "成功";
        } else {
            return "失败";
        }
    }

    @ApiOperation("停止视频推流")
    @RequestMapping(value = "/stopConvert", method = RequestMethod.GET)
    @ResponseBody
    public void stopConvert(@RequestParam String rtmp) {
        setHeader(response);
        videoAdapterManagement.stopAdapter(videoAdapterManagement.getVideoAdapter(rtmp));
    }

    @ApiOperation("获取录像")
    @RequestMapping(value = "/records", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getRecords(@RequestParam String rtmp) throws Exception {
        setHeader(response);
        RTSPVideoAdapter videoAdapter = (RTSPVideoAdapter) videoAdapterManagement.getVideoAdapter(rtmp);
        return DirUtil.getFileList(videoAdapter.getVideoPath());
    }

    @ApiOperation("获取抓拍")
    @RequestMapping(value = "/captures", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getCaptures(@RequestParam String rtmp) throws Exception {
        setHeader(response);
        RTSPVideoAdapter videoAdapter = (RTSPVideoAdapter) videoAdapterManagement.getVideoAdapter(rtmp);
        return DirUtil.getFileList(videoAdapter.getCapturesPath());
    }

    @ApiOperation("获取所有视频源")
    @RequestMapping(value = "/feedback", method = RequestMethod.GET)
    @ResponseBody
    public Set<String> getFeedbacks() {
        setHeader(response);
        return videoAdapterManagement.getAllStreams();
    }

    @ApiOperation("查找局域网内的摄像头")
    @RequestMapping(value = "/discovery", method = RequestMethod.GET)
    @ResponseBody
    public Set<String> discovery() {
        setHeader(response);
        return DeviceDiscovery.discoverIpv4DevicesWithoutProxy();
    }

    @ApiOperation("获取设备的RTSP地址")
    @RequestMapping(value = "/getRtsp", method = RequestMethod.GET)
    @ResponseBody
    public String getRtsp(@RequestParam String ip, @RequestParam String username,
                          @RequestParam String password) throws Exception {
        setHeader(response);
        OnvifDevice device = new OnvifDevice(ip, username, password, false);
        String info = device.getDevices().getDeviceInformation().toString();
        System.out.println(info);
        return device.getMedia().getRTSPStreamUri(device.getDevices().getProfiles().get(0).getToken());
    }

    //以下是视频控制

    @RequestMapping(value = "/subscribe", method = RequestMethod.GET)
    @ResponseBody
    public boolean subscribe(@RequestParam(required = false) String rtmp, @RequestParam(required = false) String ip,
                             @RequestParam(required = false) String port) throws Exception {
        setHeader(response);
        if (ip == null || port == null) {
            return HikUtil.subscribe();
        } else {
            return HikUtil.subscribe(rtmp, ip, Integer.valueOf(port));
        }
    }

    @RequestMapping(value = "/control", method = RequestMethod.GET)
    @ResponseBody
    public boolean control(@RequestParam String rtmp, @RequestParam String cmd,
                           @RequestParam int status) throws Exception {
        setHeader(response);
        int command = 0;
        switch (cmd) {
            case "up":
                command = 21;
                break;
            case "down":
                command = 22;
                break;
            case "left":
                command = 23;
                break;
            case "right":
                command = 24;
                break;
            default:
                break;
        }
        if (command == 0) {
            return false;
        }
        if (rtmp == null) {
            rtmp = "";
        }
        HikUtil.control(rtmp, command);
        setHeader(response);
        return true;
    }

    @RequestMapping(value = "/setEffect", method = RequestMethod.GET)
    @ResponseBody
    public boolean setEffect(@RequestParam String rtmp, @RequestParam int channel,
                             @RequestParam int bright, @RequestParam int contrast, @RequestParam int saturation, @RequestParam int hue) throws Exception {
        setHeader(response);
        HikUtil.setEffect(rtmp, channel, bright, contrast, saturation, hue);
        setHeader(response);
        return true;
    }

    public void setHeader(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Method", "POST,GET");
    }
}
