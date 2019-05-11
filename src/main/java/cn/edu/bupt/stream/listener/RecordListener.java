package cn.edu.bupt.stream.listener;

import cn.edu.bupt.stream.adapter.RtspVideoAdapter;
import cn.edu.bupt.stream.event.Event;
import cn.edu.bupt.stream.event.GrabEvent;
import cn.edu.bupt.stream.event.PacketEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.concurrent.*;

import static cn.edu.bupt.stream.Constants.RECORD_LISTENER_NAME;

/**
 * @Usage: 1.Init进行初始化 2.Start启动监听器 3.Fire listener，开始Record Event
 * @Description: RecordListener,用于视频流的存储
 * @Author: czx
 * @CreateDate: 2018-12-02 16:17
 * @Version: 1.0
 */
@Slf4j
public class RecordListener extends RtspListener {

    private String name;
    private ScheduledExecutorService executor;
    private FFmpegFrameRecorder fileRecorder;
    private int queueThreshold;
    private String fileName;
    private boolean isInit;
    private boolean isStarted;
    private boolean isStopped;
    private BlockingQueue<Event> queue;
    private long offerTimeout;
    private long startTimestamp = -1;
    private boolean usePacket;
    private final RtspVideoAdapter rtspVideoAdapter;
    private AVFormatContext fc;
    /**
     * Listener的fire失败次数
     */
    private int failCount = 0;
    /**
     * 失败次数的阈值，达到这个数目会从adapter中移除该listener
     */
    private int FAIL_COUNT_THRESHOLD = 5;

    private RecordListener(String listenerName, RtspVideoAdapter rtspVideoAdapter){
        this.executor = Executors.newScheduledThreadPool(1,new BasicThreadFactory.Builder().namingPattern(listenerName+"-%d").daemon(false).build());
        this.isStarted = false;
        this.isInit = false;
        this.usePacket = false;
        this.isStopped = false;
        this.name = listenerName;
        this.queueThreshold = 1024;
        this.offerTimeout = 100L;
        this.rtspVideoAdapter = rtspVideoAdapter;
        this.queue = new LinkedBlockingQueue<>();
    }

    public RecordListener(String filename, FFmpegFrameGrabber grabber,RtspVideoAdapter rtspVideoAdapter) {
        this(RECORD_LISTENER_NAME,filename,grabber,rtspVideoAdapter,false);
    }

    public RecordListener(String filename, FFmpegFrameGrabber grabber,RtspVideoAdapter rtspVideoAdapter,boolean usePacket) {
        this(RECORD_LISTENER_NAME,filename,grabber,rtspVideoAdapter,usePacket);
    }

    public RecordListener(String listenerName,String filename, FFmpegFrameGrabber grabber,RtspVideoAdapter rtspVideoAdapter,boolean usePacket) {
        this(listenerName,rtspVideoAdapter);
        this.fileName = filename;
        this.usePacket = usePacket;
        fileRecorderInit(filename,grabber);
    }

    public RecordListener(String listenerName,String filename, FFmpegFrameGrabber grabber,RtspVideoAdapter rtspVideoAdapter) {
        this(listenerName,filename,grabber,rtspVideoAdapter,false);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isUsePacket() {
        return usePacket;
    }

    public boolean isStopped() {
        return isStopped;
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
                fileRecorder.start(fc);
                executor.scheduleAtFixedRate(this::executorTask,1,5,TimeUnit.SECONDS);
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
     * @Description 用于scheduled线程池的任务
     * @author czx
     * @date 2019-04-23 23:33
     * @param []
     * @return void
     */
    private void executorTask(){
        if(queue==null){
            log.warn("Queue is null");
        }else{
            while(!queue.isEmpty()){
                Event event = queue.poll();
                try {
                    if (event instanceof GrabEvent) {

                        // 时间戳设置
                        long timestamp = ((GrabEvent) event).getTimestamp();
                        if (startTimestamp == -1) {
                            startTimestamp = timestamp;
                            timestamp = 0;
                            fileRecorder.setTimestamp(timestamp);
                        } else {
                            timestamp -= startTimestamp;
                        }
                        if (timestamp > fileRecorder.getTimestamp()) {
                            fileRecorder.setTimestamp(timestamp);
                        }

                        fileRecorder.record(((GrabEvent) event).getFrame());
                    } else if (event instanceof PacketEvent) {
                        fileRecorder.recordPacket(((PacketEvent) event).getFrame());
                    } else {
                        log.warn("Unknown event type!");
                    }
                }catch (Exception e) {
                    log.warn("Record event failed for Recorder : {}", getName());
                }finally {
                    rtspVideoAdapter.unref(event);
                }
            }
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
            isStarted = false;
            isStopped = true;
            if(executor!=null){
                executor.shutdownNow();
            }
            // 存储queue中剩余的event
            executorTask();
            fileRecorder.stop();
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
    public void fireAfterEventInvoked(Event event) throws Exception{
        if(isStarted) {
            pushEvent(event);
            failCount = 0;
        }else if(isInit){
            start();
            if(isStarted) {
                pushEvent(event);
                failCount = 0;
            }else {
                failCount++;
                if(failCount>=5){
                    rtspVideoAdapter.removeListener(this);
                    close();
                    log.error("Record Listener [{}] error. Remove it",name);
                }else{
                    log.warn("Failed to fire the listener [{}]",name);
                }
            }
        }else{
            rtspVideoAdapter.removeListener(this);
            log.error("Record Listener [{}] not initialized. Remove it",name);
        }
    }

    /**
     * @Description FileRecorder 进行初始化
     * @author czx
     * @date 2018-12-02 16:34
     * @param [filename, grabber]
     * @return void
     */
    private void fileRecorderInit(String filename,FFmpegFrameGrabber grabber){
        this.fileRecorder = new FFmpegFrameRecorder(filename,grabber.getImageWidth(),grabber.getImageHeight(),0);
        this.fileRecorder.setFrameRate(grabber.getFrameRate());
        this.fileRecorder.setFormat("flv");
        if(usePacket){
            fc = grabber.getFormatContext();
        }else{
            fc = null;
        }
        this.isInit = true;
    }

    /**
     * @Description 将event推入队列中
     * @author czx
     * @date 2018-12-04 13:13
     * @param [event]
     * @return void
     */
    private void pushEvent(Event event){
        //如果queue为null，初始化queue
        if(this.queue == null){
            log.trace("Creating event queue");
            this.queue = new LinkedBlockingQueue<>();
        }

        //将event推入queue
        try{
            if(this.queue.size() > this.queueThreshold) {
                log.warn("Queue size is greater than threshold. queue size={} threshold={}", this.queue.size(), this.queueThreshold);
            }
            if(this.queue.size() < 2 * this.queueThreshold){
                this.queue.offer(event, this.offerTimeout, TimeUnit.MILLISECONDS);
                log.trace("Inserting event into queue[size:{}]",queue.size());
            }
        }catch (Exception e) {
            log.warn("Event data was not accepted by the queue");
        }
    }
}
