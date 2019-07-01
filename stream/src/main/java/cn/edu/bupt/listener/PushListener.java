package cn.edu.bupt.listener;

import cn.edu.bupt.adapter.RtspVideoAdapter;
import cn.edu.bupt.event.Event;
import cn.edu.bupt.event.GrabEvent;
import cn.edu.bupt.event.PacketEvent;
import cn.edu.bupt.event.RTSPEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.edu.bupt.util.Constants.PUSH_LISTENER_NAME;

/**
 * @Usage: 1.Init进行初始化 2.Start启动监听器 3.Fire cn.edu.bupt.listener，开始Push Event
 * @Description: PushListener，用于视频流的推流
 * @Author: czx
 * @CreateDate: 2018-12-02 16:17
 * @Version: 1.0
 */
@Slf4j
public class PushListener extends RtspListener {

    private String name;
    private FFmpegFrameGrabber grabber;
    private static ExecutorService executor = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().namingPattern("Push-Pool-%d").daemon(false).build());
    private static final AtomicBoolean executorStarted = new AtomicBoolean(false);
    private FFmpegFrameRecorder pushRecorder;
    private int queueThreshold;
    private String rtmpPath;
    private boolean isInit;
    private boolean isStarted;
    private static BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
    private long offerTimeout;
    private boolean usePacket;
    private final RtspVideoAdapter rtspVideoAdapter;
    private AVFormatContext fc;
    private long lastDTS = 0;

    private PushListener(String listenerName,RtspVideoAdapter rtspVideoAdapter){
        this.isStarted = false;
        this.isInit = false;
        this.usePacket = false;
        this.name = listenerName;
        this.rtspVideoAdapter = rtspVideoAdapter;
        this.queueThreshold = 1024;
        this.offerTimeout = 100L;
    }

    public PushListener(String listenerName,String rtmpPath,FFmpegFrameGrabber grabber,RtspVideoAdapter rtspVideoAdapter,boolean usePacket){
        this(listenerName,rtspVideoAdapter);
        this.rtmpPath = rtmpPath;
        this.grabber = grabber;
        this.usePacket = usePacket;
        pushRecorderInit(rtmpPath,grabber);
    }

    public PushListener(String listenerName,String rtmpPath,FFmpegFrameGrabber grabber,RtspVideoAdapter rtspVideoAdapter){
        this(listenerName,rtmpPath,grabber,rtspVideoAdapter,false);
    }

    public PushListener(String rtmpPath, FFmpegFrameGrabber grabber,RtspVideoAdapter rtspVideoAdapter) {
        this(PUSH_LISTENER_NAME,rtmpPath,grabber,rtspVideoAdapter,false);
    }

    public PushListener(String rtmpPath, FFmpegFrameGrabber grabber,RtspVideoAdapter rtspVideoAdapter,boolean usePacket) {
        this(PUSH_LISTENER_NAME,rtmpPath,grabber,rtspVideoAdapter,usePacket);
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

    public boolean isUsePacket() {
        return usePacket;
    }

    public RtspVideoAdapter getRtspVideoAdapter() {
        return rtspVideoAdapter;
    }

    /**
     * @Description recorder在初始化之后还需要进行启动，启动调用该方法
     * @author czx
     * @date 2018-12-02 17:17
     * @param
     * @return void
     */
    @Override
    public void start(){
        try {
            if(isInit) {
                pushRecorder.start(fc);
                startExecutor();
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
     * @param
     * @return void
     */
    @Override
    public void close(){
        try {
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
     * @param event
     * @return void
     */
    @Override
    public void fireAfterEventInvoked(Event event) throws Exception{
        if(isStarted) {
            ((RTSPEvent)event).setListener(this);
            pushEvent(event);
        }else {
            log.warn("Failed to fire the cn.edu.bupt.listener [{}].You should start this push recorder before you start pushing",name);
            throw new Exception("Failed to fire the cn.edu.bupt.listener!");
        }
    }

    /**
     * @Description PushRecorder 进行初始化
     * @author czx
     * @date 2018-12-02 16:34
     * @param rtmpPath, grabber
     * @return void
     */
    private void pushRecorderInit(String rtmpPath,FFmpegFrameGrabber grabber){
        //若选择录制声音，会造成较高的延迟
        this.pushRecorder = new FFmpegFrameRecorder(rtmpPath,grabber.getImageWidth(),grabber.getImageHeight(),0);
        pushRecorder.setFrameRate(grabber.getFrameRate());
        pushRecorder.setFormat("flv");
        pushRecorder.setVideoOption("preset", "ultrafast");
        if(usePacket){
            fc = grabber.getFormatContext();
        }else{
            fc = null;
        }
        this.isInit = true;
    }


    /**
     * @Description 将event推入队列中，通过新线程进行处理
     * @author czx
     * @date 2018-12-04 13:13
     * @param event
     * @return void
     */
    private void pushEvent(Event event){
        //将event推入queue
        try{
            if(queue.size() > this.queueThreshold) {
                log.warn("Queue size is greater than threshold. queue size={} threshold={} timestamp={}", this.queue.size(), this.queueThreshold,System.currentTimeMillis());
            }
            if(queue.size() < 2 * this.queueThreshold){
                queue.offer(event, this.offerTimeout, TimeUnit.MILLISECONDS);
                log.trace("Inserting cn.edu.bupt.event into queue[size:{}]",queue.size());
            }else {
                log.warn("clear queue");
                queue.clear();
            }
        }catch (Exception e){
            log.warn("Event data was not accepted by the queue");
        }
    }

    private void startExecutor(){
        if(executorStarted.compareAndSet(false,true)) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Event event = queue.take();
                            PushListener listener = (PushListener) ((RTSPEvent) event).getListener();
                            if (!listener.isStarted) {
                                continue;
                            }
                            FFmpegFrameRecorder pushRecorder = listener.pushRecorder;
                            boolean success = false;
                            try {
                                if (event instanceof PacketEvent) {
                                    AVPacket avPacket = ((PacketEvent) event).getFrame();
                                    if(avPacket.dts()<listener.lastDTS){
                                        continue;
                                    }else{
                                        listener.lastDTS = avPacket.dts();
                                    }
                                    success = pushRecorder.recordPacket(avPacket);
                                } else if (event instanceof GrabEvent) {
                                    pushRecorder.record(((GrabEvent) event).getFrame());
                                    success = true;
                                } else {
                                    throw new Exception("Unknown cn.edu.bupt.event type!");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                log.warn("Push cn.edu.bupt.event failed for pushRecorder [{}]", getName());
                            } finally {
                                listener.rtspVideoAdapter.unref(event, success);
                            }
                            if(queue.isEmpty()&&!executorStarted.get()){
                                break;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Executor exception!");
                    }
                }
            });
        }
    }
}
