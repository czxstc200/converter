package cn.edu.bupt.stream.listener;

import cn.edu.bupt.stream.event.Event;
import cn.edu.bupt.stream.event.GrabEvent;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

/**
 * @Description: PushListener，用于视频流的推流
 * @Author: czx
 * @CreateDate: 2018-12-02 16:17
 * @Version: 1.0
 */
@Slf4j
public class PushListener implements Listener {

    private String name;

    FFmpegFrameRecorder pushRecorder;

    String rtmpPath;

    boolean isInit;

    boolean isStarted;

    public PushListener(String listenerName,String rtmpPath,FFmpegFrameGrabber grabber){
        this.rtmpPath = rtmpPath;
        pushRecorderInit(rtmpPath,grabber);
        this.name = listenerName;
    }

    public PushListener(String rtmpPath, FFmpegFrameGrabber grabber) {
        this.rtmpPath = rtmpPath;
        pushRecorderInit(rtmpPath,grabber);
        this.name = "Push Listener";
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRtmpPath() {
        return rtmpPath;
    }

    /**
     * @Description recorder在初始化之后还需要进行启动，启动调用该方法
     * @author czx
     * @date 2018-12-02 17:17
     * @param []
     * @return void
     */
    public void start(){
        try {
            if(isInit) {
                pushRecorder.start();
                isStarted = true;
                log.info("Push recorder started");
            }else {
                throw new Exception("You must initialize the push recorder before start it");
            }
        }catch (Exception e){
            log.error("Push recorder failed to start");
            e.printStackTrace();
        }
    }

    /**
     * @Description 关闭该recorder
     * @author czx
     * @date 2018-12-02 17:16
     * @param []
     * @return void
     */
    public void close(){
        try {
            pushRecorder.stop();
            pushRecorder.release();
            isStarted = false;
            this.pushRecorder = null;
            log.info("Push recorder stopped");
        }catch (Exception e){
            log.error("Push recorder failed to close");
            e.printStackTrace();
        }
    }

    /**
     * @Description 当发生事件时调用该函数
     * @author czx
     * @date 2018-12-02 17:15
     * @param [event]
     * @return void
     */
    @Override
    public void fireAfterEventInvoked(Event event) {
        if(isStarted){
            try {
                GrabEvent grabEvent = (GrabEvent) event;
                Frame frame = grabEvent.getFrame();
                pushRecorder.record(frame);
            }catch (Exception e){
                log.error("Push recorder failed to record");
                e.printStackTrace();
            }
        }else {
            log.warn("Failed to fire the listener.You should start this Push recorder before you start pushing");
        }
    }

    /**
     * @Description FileRecord 进行初始化
     * @author czx
     * @date 2018-12-02 16:34
     * @param [rtmpPath, grabber]
     * @return void
     */
    private void pushRecorderInit(String rtmpPath,FFmpegFrameGrabber grabber){
        this.pushRecorder = new FFmpegFrameRecorder(rtmpPath,grabber.getImageWidth(),grabber.getImageHeight(),grabber.getAudioChannels());
        pushRecorder.setFormat("mp4");
        pushRecorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        pushRecorder.setFrameRate(grabber.getFrameRate());
        pushRecorder.setVideoOption("preset", "ultrafast");
        pushRecorder.setFormat("flv");
        pushRecorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        this.isInit = true;
    }
}
