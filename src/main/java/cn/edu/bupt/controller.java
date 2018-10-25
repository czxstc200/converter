package cn.edu.bupt;

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
}
