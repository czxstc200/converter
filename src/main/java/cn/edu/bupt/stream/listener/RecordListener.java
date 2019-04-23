package cn.edu.bupt.stream.listener;

import cn.edu.bupt.stream.event.Event;
import cn.edu.bupt.stream.event.GrabEvent;
import cn.edu.bupt.stream.event.PacketEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;

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
public class RecordListener implements Listener {

    private String name;
    private ExecutorService executor = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().namingPattern("Record-pool-%d").daemon(false).build());
    FFmpegFrameRecorder fileRecorder;
    private int queueThreshold;
    String fileName;
    boolean isInit;
    boolean isStarted;
    private BlockingQueue<Event> queue;
    private long offerTimeout;
    private long startTimestamp = -1;
    private avformat.AVFormatContext fc;

    public RecordListener(){
        this.isStarted = false;
        this.isInit = false;
        // 以下配置暂时无用
        this.queueThreshold = 1024;
        this.offerTimeout = 100L;
        this.queue = new LinkedBlockingQueue<>();
    }

    public RecordListener(String filename, FFmpegFrameGrabber grabber) {
        this(RECORD_LISTENER_NAME,filename,grabber);
    }

    public RecordListener(String listenerName,String filename, FFmpegFrameGrabber grabber) {
        this();
        this.fileName = filename;
        this.name = listenerName;
        fileRecorderInit(filename,grabber);
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
     * @Description 用于scheduled线程池的任务，目前暂时无用
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
                PacketEvent event = (PacketEvent)queue.poll();
                long timestamp = event.getTimestamp();
                if(startTimestamp==-1){
                    startTimestamp = timestamp;
                    timestamp = 0;
                }else{
                    timestamp -= startTimestamp;
                }
                try {
                    fileRecorder.setTimestamp(timestamp);
                    fileRecorder.recordPacket(((PacketEvent) event).getFrame());
                }catch (Exception e){
                    log.warn("Record event failed for Recorder {}",getName());
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
            startTimestamp = -1;
            if(executor!=null){
                executor.shutdown();
            }
            fileRecorder.stop();
            isStarted = false;
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
        if(isStarted) {
            long timestamp = ((PacketEvent) event).getTimestamp();
            if (startTimestamp == -1) {
                startTimestamp = timestamp;
                timestamp = 0;
            } else {
                timestamp -= startTimestamp;
            }
            ((PacketEvent) event).setTimestamp(timestamp);
            this.executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        long videoTS = ((PacketEvent) event).getTimestamp();
                        //告诉录制器写入这个timestamp
                        fileRecorder.setTimestamp(videoTS);
                        fileRecorder.recordPacket(((PacketEvent) event).getFrame());
                    } catch (Exception e) {
                        log.warn("Record event failed for Recorder {}", getName());
                    }
                }
            });
        }else{
            log.warn("Failed to fire the listener.You should start this file recorder before you start recording");
        }

//        if(isStarted){
//            try {
//                pushEvent(event);
//            }catch (Exception e){
//                log.warn("Record event failed for Recorder {}",getName());
//            }
//        }else {
//            log.warn("Failed to fire the listener.You should start this file recorder before you start recording");
//        }
    }

    /**
     * @Description FileRecord 进行初始化
     * @author czx
     * @date 2018-12-02 16:34
     * @param [filename, grabber]
     * @return void
     */
    private void fileRecorderInit(String filename,FFmpegFrameGrabber grabber){
        this.fileRecorder = new FFmpegFrameRecorder(filename,grabber.getImageWidth(),grabber.getImageHeight(),0);
        this.fileRecorder.setFrameRate(grabber.getFrameRate());
        this.fileRecorder.setFormat("flv");
        fc = grabber.getFormatContext();
        this.isInit = true;
    }

    /**
     * @Description 将event推入队列中,目前暂时无用
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
                    log.warn("Queue size is greater than threshold. queue size={} threshold={}", Integer.valueOf(this.queue.size()), Integer.valueOf(this.queueThreshold));
                }
                if(this.queue.size() < 2 * this.queueThreshold){
                    this.queue.offer(event, this.offerTimeout, TimeUnit.MILLISECONDS);
                    log.trace("Inserting event into queue[size:{}]",queue.size());
                }
            }catch (Exception e){
                log.warn("Event data was not accepted by the queue");
            }
        }
    }
}
