package cn.edu.bupt.listener;

import cn.edu.bupt.adapter.RtspVideoAdapter;
import cn.edu.bupt.event.Event;
import cn.edu.bupt.event.GrabEvent;
import cn.edu.bupt.event.PacketEvent;
import cn.edu.bupt.event.RTSPEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.edu.bupt.util.Constants.RECORD_LISTENER_NAME;

/**
 * @Usage: 1.Init进行初始化 2.Start启动监听器 3.Fire cn.edu.bupt.listener，开始Record Event
 * @Description: RecordListener,用于视频流的存储
 * @Author: czx
 * @CreateDate: 2018-12-02 16:17
 * @Version: 1.0
 */
@Slf4j
public class RecordListener extends RtspListener {

    private String name;
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1,new BasicThreadFactory.Builder().namingPattern("Record-Pool-%d").daemon(false).build());
    private static AtomicBoolean executorStarted = new AtomicBoolean(false);
    private FFmpegFrameRecorder fileRecorder;
    private int queueThreshold;
    private String fileName;
    private boolean isInit;
    private boolean isStarted;
    private boolean isStopped;
    private static BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
    private long offerTimeout;
    private long startTimestamp = -1;
    private boolean usePacket;
    private final RtspVideoAdapter rtspVideoAdapter;
    private AVFormatContext fc;
    private CountDownLatch closeCountDownLatch = new CountDownLatch(1);
    /**
     * Listener的fire失败次数
     */
    private int failCount = 0;
    /**
     * 失败次数的阈值，达到这个数目会从adapter中移除该listener
     */
    private int FAIL_COUNT_THRESHOLD = 5;

    private RecordListener(String listenerName, RtspVideoAdapter rtspVideoAdapter){
        this.isStarted = false;
        this.isInit = false;
        this.usePacket = false;
        this.isStopped = false;
        this.name = listenerName;
        this.queueThreshold = 1024;
        this.offerTimeout = 100L;
        this.rtspVideoAdapter = rtspVideoAdapter;
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
                if(executorStarted.compareAndSet(false,true)) {
                    executor.scheduleAtFixedRate(()->{executorTask();}, 1, 5, TimeUnit.SECONDS);
                }
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
            Set<RecordListener> recordListeners = new HashSet<>();
            while(!queue.isEmpty()){
                Event event = queue.poll();
                RecordListener listener = (RecordListener)((RTSPEvent) event).getListener();
                FFmpegFrameRecorder fileRecorder = listener.fileRecorder;
                if(listener.isStopped){
                    recordListeners.add(listener);
                }
                boolean success = false;
                try {
                    if (event instanceof GrabEvent) {
                        // 时间戳设置
                        long timestamp = ((GrabEvent) event).getTimestamp();
                        if (listener.startTimestamp == -1) {
                            listener.startTimestamp = timestamp;
                            timestamp = 0;
                            fileRecorder.setTimestamp(timestamp);
                        } else {
                            timestamp -= listener.startTimestamp;
                        }
                        if (timestamp > fileRecorder.getTimestamp()) {
                            fileRecorder.setTimestamp(timestamp);
                        }
                        fileRecorder.record(((GrabEvent) event).getFrame());
                        success = true;
                    } else if (event instanceof PacketEvent) {
                        success = fileRecorder.recordPacket(((PacketEvent) event).getFrame());
                    } else {
                        log.warn("Unknown cn.edu.bupt.event type!");
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                    log.warn("Record cn.edu.bupt.event failed for Recorder : {}", listener.getName());
                }finally {
                    listener.rtspVideoAdapter.unref(event,success);
                }
            }
            // 关闭recorder
            if(!recordListeners.isEmpty()){
                Iterator<RecordListener> iterator = recordListeners.iterator();
                while(iterator.hasNext()){
                    RecordListener recordListener = iterator.next();
                    FFmpegFrameRecorder recorder = recordListener.fileRecorder;
                    try {
                        recorder.stop();
                    }catch (Exception e){
                        e.printStackTrace();
                        log.warn("Failed to stop a file recorder");
                    }finally {
                        recordListener.closeCountDownLatch.countDown();
                    }
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
            closeCountDownLatch.await(10000L,TimeUnit.MILLISECONDS);
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
            ((RTSPEvent)event).setListener(this);
            pushEvent(event);
            failCount = 0;
        }else if(isInit&&!isStopped){
            start();
            if(isStarted) {
                pushEvent(event);
                failCount = 0;
            }else {
                failCount++;
                if(failCount>=FAIL_COUNT_THRESHOLD){
                    rtspVideoAdapter.removeListener(this);
                    close();
                    log.error("Record Listener [{}] error. Remove it",name);
                }else{
                    log.warn("Failed to fire the cn.edu.bupt.listener [{}]",name);
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
        if(queue == null){
            log.trace("Creating cn.edu.bupt.event queue");
            queue = new LinkedBlockingQueue<>();
        }

        //将event推入queue
        try{
            if(queue.size() > this.queueThreshold) {
                log.warn("Queue size is greater than threshold. queue size={} threshold={}", this.queue.size(), this.queueThreshold);
            }
            if(queue.size() < 2 * this.queueThreshold){
                queue.offer(event, this.offerTimeout, TimeUnit.MILLISECONDS);
                log.trace("Inserting cn.edu.bupt.event into queue[size:{}]",queue.size());
            }
        }catch (Exception e) {
            log.warn("Event data was not accepted by the queue");
        }
    }
}
