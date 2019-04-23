package cn.edu.bupt.stream.listener;

import cn.edu.bupt.stream.event.Event;
import cn.edu.bupt.stream.event.GrabEvent;
import cn.edu.bupt.stream.event.PacketEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;

import java.util.concurrent.*;

import static cn.edu.bupt.stream.Constants.PUSH_LISTENER_NAME;

/**
 * @Usage: 1.Init进行初始化 2.Start启动监听器 3.Fire listener，开始Push Event
 * @Description: PushListener，用于视频流的推流
 * @Author: czx
 * @CreateDate: 2018-12-02 16:17
 * @Version: 1.0
 */
@Slf4j
public class PushListener implements Listener {

    private String name;
    private FFmpegFrameGrabber grabber;
    private static ExecutorService executor = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().namingPattern("Push-pool-%d").daemon(true).build());
    private FFmpegFrameRecorder pushRecorder;
    private int queueThreshold;
    private String rtmpPath;
    private boolean isInit;
    private boolean isStarted;
    private BlockingQueue<Event> queue;
    private long offerTimeout;
    private boolean isSubmitted;
    avformat.AVFormatContext fc;

    public PushListener(){
        this.isStarted = false;
        this.isInit = false;
        //以下配置暂时无用
        this.queueThreshold = 240;
        this.offerTimeout = 100L;
        this.isSubmitted = false;
    }

    public PushListener(String listenerName,String rtmpPath,FFmpegFrameGrabber grabber){
        this();
        this.rtmpPath = rtmpPath;
        this.grabber = grabber;
        this.name = listenerName;
        pushRecorderInit(rtmpPath,grabber);
    }

    public PushListener(String rtmpPath, FFmpegFrameGrabber grabber) {
        this(PUSH_LISTENER_NAME,rtmpPath,grabber);
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
    @Override
    public void start(){
        try {
            if(isInit) {
                pushRecorder.start(fc);
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
    @Override
    public void close(){
        try {
            if(executor!=null){
                executor.shutdown();
            }
            pushRecorder.stop();
            isStarted = false;
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
        if(isStarted) {
            boolean success = false;
            avcodec.AVPacket pkt = ((PacketEvent) event).getFrame();
            try {
                success = pushRecorder.recordPacket(pkt);
            } catch (FrameRecorder.Exception e) {
                log.warn("Push event failed for pushRecorder {}", getName());
            } finally {
                if (!success) {
                    avcodec.av_packet_unref(pkt);
                }
            }
        }else{
            log.warn("Failed to fire the listener.You should start this push recorder before you start pushing");
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
        //若选择录制声音，会造成较高的延迟
        this.pushRecorder = new FFmpegFrameRecorder(rtmpPath,grabber.getImageWidth(),grabber.getImageHeight(),0);
        pushRecorder.setFrameRate(grabber.getFrameRate());
        pushRecorder.setVideoOption("preset", "ultrafast");
        pushRecorder.setFormat("flv");
        fc = grabber.getFormatContext();
        this.isInit = true;
    }


    /**
     * @Description 将event推入队列中，通过新线程进行处理,目前暂时无用
     * @author czx
     * @date 2018-12-04 13:13
     * @param [event]
     * @return void
     */
    public void pushEvent(Event event){
        if(event instanceof GrabEvent){

            //如果queue为空，初始化它
            if(this.queue == null){
                log.trace("Creating event queue");
                this.queue = new LinkedBlockingQueue<>();
            }

            //将event推入queue
            try{
                if(this.queue.size() > this.queueThreshold) {
                    log.warn("Queue size is greater than threshold. queue size={} threshold={} timestamp={}", Integer.valueOf(this.queue.size()), Integer.valueOf(this.queueThreshold),System.currentTimeMillis());
                }
                if(this.queue.size() < 2 * this.queueThreshold){
                    this.queue.offer(event, this.offerTimeout, TimeUnit.MILLISECONDS);
                    log.trace("Inserting event into queue[size:{}]",queue.size());
                }else {
                    log.warn("clear queue");
                    this.queue.clear();
                }
            }catch (Exception e){
                log.warn("Event data was not accepted by the queue");
            }
        }

        //如果还未给线程提交任务，则进入if内部
        if(!isSubmitted){
            this.executor.submit(new Runnable() {
                @Override
                public void run() {
                    while(isStarted){
                        try{
//                            if(!PushListener.this.queue.isEmpty()) {
                                GrabEvent nextEvent = (GrabEvent) PushListener.this.queue.take();
                                if(nextEvent!=null) {
                                    pushRecorder.setTimestamp(grabber.getTimestamp());
                                    pushRecorder.record(nextEvent.getFrame());
                                }
                                log.trace("Processing event from queue[size:{}]", queue.size());
//                            }
                        }catch (Exception e){
                            log.warn("Failed to push event");
                        }
                    }
                    try {
                        pushRecorder.stop();
                        pushRecorder.release();
                        pushRecorder = null;
                        queue=null;
                    }catch (Exception e){

                    }
                }
            });
            this.isSubmitted = true;
        }
    }
}
