package cn.edu.bupt.stream.listener;

import cn.edu.bupt.stream.event.Event;
import cn.edu.bupt.stream.event.GrabEvent;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

/**
 * @Description: RecordListener,用于视频流的存储
 * @Author: czx
 * @CreateDate: 2018-12-02 16:17
 * @Version: 1.0
 */
@Slf4j
public class RecordListener implements Listener {

    FFmpegFrameRecorder fileRecorder;

    boolean isInit;

    boolean isStarted;

    public RecordListener(String filename, FFmpegFrameGrabber grabber) {
        fileRecorderInit(filename,grabber);
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
                fileRecorder.start();
                isStarted = true;
                log.info("File recorder started");
            }else {
                throw new Exception("You must initialize the file recorder before start it");
            }
        }catch (Exception e){
            log.error("File recorder failed to start");
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
            fileRecorder.stop();
            fileRecorder.release();
            isStarted = false;
            this.fileRecorder = null;
            log.info("File recorder stopped");
        }catch (Exception e){
            log.error("File recorder failed to close");
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
                fileRecorder.record(frame);
            }catch (Exception e){
                log.error("File recorder failed to record");
                e.printStackTrace();
            }
        }else {
            log.warn("Failed to fire the listener.You should start this file recorder before you start recording");
        }
    }

    /**
     * @Description FileRecord 进行初始化
     * @author czx
     * @date 2018-12-02 16:34
     * @param [filename, grabber]
     * @return void
     */
    private void fileRecorderInit(String filename,FFmpegFrameGrabber grabber){
        this.fileRecorder = new FFmpegFrameRecorder(filename,grabber.getImageWidth(),grabber.getImageHeight(),grabber.getAudioChannels());
        fileRecorder.setFrameRate(grabber.getFrameRate());
        fileRecorder.setFormat("mp4");
        this.isInit = true;
    }
}
