package cn.edu.bupt.stream.adapter;

import cn.edu.bupt.stream.event.GrabEvent;
import cn.edu.bupt.stream.listener.Listener;
import cn.edu.bupt.stream.listener.PushListener;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: RtspVideoAdapter
 * @Author: czx
 * @CreateDate: 2018-12-02 17:52
 * @Version: 1.0
 */
@Slf4j
public class RtspVideoAdapter extends VideoAdapter{

    private static long timestamp;

    private String videoRootDir;

    private boolean stop;

    private FFmpegFrameGrabber grabber;

    private String rtspPath;

    private List<Listener> listeners;

    public RtspVideoAdapter(String adapterName) {
        super(adapterName);
        listeners = new ArrayList<>();
        stop = false;
    }

    public RtspVideoAdapter(String adapterName, String rtspPath) {
        super(adapterName);
        this.rtspPath = rtspPath;
        listeners = new ArrayList<>();
        stop = false;
    }

    @Override
    public boolean addListener(Listener listener){
        log.info("Add listener[{}] from VideoAdapter[{}]",listener.getName(),getName());
        return listeners.add(listener);
    }

    @Override
    public boolean removeListener(Listener listener){
        log.info("remove listener[{}] from VideoAdapter[{}]",listener.getName(),getName());
        return listeners.remove(listener);
    }

    /**
     * @Description adapter启动，根据需要可以添加listener实现相应功能
     * @author czx
     * @date 2018-12-03 23:01
     * @param []
     * @return void
     */
    @Override
    public void start() throws Exception{
        log.info("RtspVideoAdapter starts for {rtsp is {}}",rtspPath);
        grabberInit();
        log.info("Grabber starts for video rtsp:{}",rtspPath);
        startAllListeners();
        int count = 0;
        while(!stop){
            count++;
            if(count % 100 == 0){
                log.debug("Video[{}] counts={}",rtspPath,count);
            }
            for(Listener listener:listeners){
                listener.fireAfterEventInvoked(new GrabEvent(this,grabber.grabImage()));
            }
        }
        log.info("Grabber ends for video rtsp:{}",rtspPath);
        closeAllListeners();

    }

    @Override
    public void stop(){
        stop = true;
    }

    /**
     * @Description 拉流grabber的初始化与启动
     * @author czx
     * @date 2018-12-03 23:00
     * @param []
     * @return void
     */
    public void grabberInit(){
        try {
            // 使用rtsp的时候需要使用 FFmpegFrameGrabber，不能再用 FrameGrabber
            FFmpegFrameGrabber grabber = FFmpegFrameGrabber.createDefault(rtspPath);
            // 使用tcp的方式，不然会丢包很严重
            grabber.setOption("rtsp_transport", "tcp");
            this.grabber = grabber;
            this.grabber.start();
        }catch (Exception e){
            log.error("Grabber failed to initialize");
            e.printStackTrace();
        }
    }

    private void startAllListeners(){
        log.info("Start all listeners");
        for(Listener listener:listeners){
            listener.start();
        }
    }

    private void closeAllListeners(){
        log.info("Close all listeners");
        for(Listener listener:listeners){
            listener.close();
        }
        listeners.removeAll(listeners);
    }

    public FFmpegFrameGrabber getGrabber() {
        return grabber;
    }

    public static void main(String[] args) {
        RtspVideoAdapter rtspVideoAdapter = new RtspVideoAdapter("rtsp","rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov");
        rtspVideoAdapter.grabberInit();
        PushListener pushListener = new PushListener("rtmp://10.112.17.185/oflaDemo/haikang1",rtspVideoAdapter.getGrabber());
        rtspVideoAdapter.addListener(pushListener);
        try {
            rtspVideoAdapter.start();

        }catch (Exception e){

        }
    }
}
