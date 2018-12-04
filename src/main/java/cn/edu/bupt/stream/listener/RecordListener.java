package cn.edu.bupt.stream.listener;

import cn.edu.bupt.stream.event.Event;
import cn.edu.bupt.stream.event.GrabEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.util.concurrent.*;

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
    private ExecutorService executor;
    FFmpegFrameRecorder fileRecorder;
    private int queueThreshold;
    String fileName;
    boolean isInit;
    boolean isStarted;
    private BlockingQueue<Event> queue;
    private long offerTimeout;
    private boolean isSubmitted;
    private boolean closeNotifiaction;

    public RecordListener(){
        this.isStarted = false;
        this.isInit = false;
        this.executor = new ScheduledThreadPoolExecutor(1,new BasicThreadFactory.Builder().namingPattern("RecordListener-pool-%d").daemon(true).build());
        this.queueThreshold = 240;
        this.offerTimeout = 100L;
        this.isSubmitted = false;
    }

    public RecordListener(String filename, FFmpegFrameGrabber grabber) {
        this("Record Listener",filename,grabber);
    }

    public RecordListener(String listenerName,String filename, FFmpegFrameGrabber grabber) {
        this();
        this.fileName = filename;
        fileRecorderInit(filename,grabber);
        this.name = listenerName;
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
    @Override
    public void close(){
        try {
            isStarted = false;
            if(executor!=null){
                executor.shutdown();
            }
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
            pushEvent(event);
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
        this.isInit = true;
    }

    /**
     * @Description 将event推入队列中，通过新线程进行处理
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

        //如果还未给线程提交任务，则进入if内部
        if(!isSubmitted){
            this.executor.submit(new Runnable() {
                @Override
                public void run() {
                    while(isStarted){
                        try{
                            if(!RecordListener.this.queue.isEmpty()) {
                                GrabEvent nextEvent = (GrabEvent) RecordListener.this.queue.take();
                                fileRecorder.record(nextEvent.getFrame());
                                log.trace("Processing event from queue[size:{}]", queue.size());
                            }
                        }catch (Exception e){
                            log.warn("Failed to record event");
                        }
                    }
                    while(!RecordListener.this.queue.isEmpty()){
                        try {
                            GrabEvent nextEvent = (GrabEvent) RecordListener.this.queue.take();
                            fileRecorder.record(nextEvent.getFrame());
                        }catch (Exception e){

                        }
                    }
                    try {
                        fileRecorder.stop();
                        fileRecorder.release();
                        fileRecorder = null;
                    }catch (Exception e){

                    }
                }
            });
            this.isSubmitted = true;
        }
    }
}
