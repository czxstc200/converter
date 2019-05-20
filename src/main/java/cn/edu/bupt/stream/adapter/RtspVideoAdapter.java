package cn.edu.bupt.stream.adapter;

import cn.edu.bupt.stream.Constants;
import cn.edu.bupt.stream.event.Event;
import cn.edu.bupt.stream.event.GrabEvent;
import cn.edu.bupt.stream.event.PacketEvent;
import cn.edu.bupt.stream.listener.Listener;
import cn.edu.bupt.stream.listener.PushListener;
import cn.edu.bupt.stream.listener.RecordListener;
import cn.edu.bupt.util.DirUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.edu.bupt.stream.Constants.*;

/**
 * @Description: RtspVideoAdapter
 * @Author: czx
 * @CreateDate: 2018-12-02 17:52
 * @Version: 1.0
 */
@Slf4j
public class RtspVideoAdapter extends VideoAdapter{

    private String name;
    private static long timestamp;
    private String videoRootDir;
    private boolean isRecording;
    private boolean isPushing;
    private boolean stop;
    private FFmpegFrameGrabber grabber;
    private String rtspPath;
    private String rtmpPath;
    private List<Listener> listeners;
    private boolean save;
    private AtomicBoolean capture = new AtomicBoolean(false);
    private final int NULL_FRAME_THRESHOLD = 512;
    /**
     * 是否使用AVPacket的方式直接进行拉流与推流
     */
    private boolean usePacket;
    private static final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
    private static ExecutorService executor = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().namingPattern("Rtsp-pool-%d").daemon(false).build());
    /**
     * 用于获取capture的future
     */
    private CountDownLatch countDownLatch = new CountDownLatch(1);
    /**
     * 用于记录每一个frame需要完成的listeners个数，在全部listener完成任务后，调用PointerScope进行内存回收
     */
    private Map<Event, AtomicInteger> frameFinishCount = new HashMap<>();
    private Future<Boolean> captureFuture;

    public RtspVideoAdapter(){
        listeners = new ArrayList<>();
        isRecording = false;
        stop = false;
        videoRootDir = Constants.getRootDir();
        timestamp = DirUtil.getZeroTimestamp();
        save = false;
        usePacket = false;
        // 设置日志打印等级
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
    }

    public RtspVideoAdapter(String adapterName) {
        this();
        name = adapterName;

    }

    public RtspVideoAdapter(String rtspPath, String rtmpPath,boolean save,boolean usePacket) {
        this(rtmpPath);
        this.rtspPath = rtspPath;
        this.rtmpPath =rtmpPath;
        this.save = save;
        this.usePacket = usePacket;
    }

    public RtspVideoAdapter(String rtspPath, String rtmpPath,boolean save) {
        this(rtmpPath);
        this.rtspPath = rtspPath;
        this.rtmpPath =rtmpPath;
        this.save = save;
    }

    public String getVideoRootDir() {
        return videoRootDir;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isPushing() {
        return isPushing;
    }

    public boolean isStop() {
        return stop;
    }


    public boolean isUsePacket() {
        return usePacket;
    }

    public String getRtspPath() {
        return rtspPath;
    }

    public String getRtmpPath() {
        return rtmpPath;
    }

    public Map<Event, AtomicInteger> getFrameFinishCount() {
        return frameFinishCount;
    }

    public FFmpegFrameGrabber getGrabber() {
        return grabber;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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
        log.info("RtspVideoAdapter is starting : [rtsp is {},rtmp is {}]",rtspPath,rtmpPath);
        grabberInit();
        log.info("Grabber started [{}]",rtspPath);
        startAllListeners();
        String filePath = videoRootDir+rtmpPath.substring(rtmpPath.lastIndexOf("/")+1)+"/";
        String capturesPath = filePath+"captures/";
        String videoPath = filePath+"videos/";

        DirUtil.judeDirExists(filePath);
        DirUtil.judeDirExists(videoPath);
        DirUtil.judeDirExists(capturesPath);
        if(save){
            startRecording(videoPath+DirUtil.generateFilenameByDate()+".flv");
        }

        startPushing();


        int count = 0;
        int nullFrames = 0;
        try {
            while (!stop) {
                //记录帧数
                count++;
                if (count % 100 == 0) {
                    log.debug("Video[{}] counts={}", rtspPath, count);
                }

                //时间超过零点进行视频录像的切分
                if (isRecording && timestamp < DirUtil.getZeroTimestamp()) {
                    executor.submit(()->{
                        timestamp = DirUtil.getZeroTimestamp();
                        restartRecording(videoPath + DirUtil.generateFilenameByDate() + ".flv");
                    });
                }

                //使用AVPacket进行推流，目前这种模式下不能对数据帧进行处理
                if (usePacket) {
                    AVPacket pkt = null;
                    try {
                        pkt = grabber.grabPacket();
                    }catch (Exception e){
                        log.warn("Grab Packet Exception!");
                    }

                    // 检查是否接收到数据
                    if (pkt==null || pkt.size()<=0 || pkt.data()==null) {
                        nullFrames++;
                        if(nullFrames%50==0){
                            log.info("Null Frame number is [{}] and rtmp : [{}]",nullFrames, rtmpPath);
                        }
                        //连续帧都是null时判断已经停止推流
                        if (nullFrames >= NULL_FRAME_THRESHOLD) {
                            stop();
                            log.info("Video[{}] stopped!", rtmpPath);
                        }
                        continue;
                    } else {
                        nullFrames = 0;
                    }

                    PacketEvent.CountEvent countEvent = new PacketEvent.CountEvent();
                    frameFinishCount.put(countEvent,new AtomicInteger(listeners.size()));

                    //AVPacket采用计数法进行内存的回收，因此在每一个listener进行处理时，
                    //都需要创建一个新的ref。由于JavaCV中的方法自带unref，如果没有创建
                    //ref，一个listener处理完后就有可能回收内存。为了保险起见，自己实现了一个
                    //Unref的逻辑
                    for (Listener listener : listeners) {
                        AVPacket newPkt = avcodec.av_packet_alloc();
                        avcodec.av_packet_ref(newPkt, pkt);
                        PacketEvent grabEvent = new PacketEvent(this, newPkt,countEvent);
                        listener.fireAfterEventInvoked(grabEvent);
                    }
                    avcodec.av_packet_unref(pkt);
                } else {//使用传统方式进行处理，效率较低（增加了编解码的时间），但是可以对画面frame进行处理
                    Frame frame = null;
                    try {
                        frame = grabber.grabImage();
                    } catch (Exception e) {
                        log.warn("Grab Image Exception!");
                    }
                    if (frame == null || frame.image==null) {
                        nullFrames++;
                        if(nullFrames%50==0){
                            log.info("Null Frame number is [{}] and rtmp : [{}]",nullFrames, rtmpPath);
                        }
                        if (nullFrames >= NULL_FRAME_THRESHOLD) {
                            stop();
                            log.info("Video[{}] stopped!", rtmpPath);
                        }
                        continue;
                    }

                    // PointScope用于释放frame的内存
                    // Pointer会自动attach到PointerScope上。
                    // 同样，clone获得的frame需要进行unref并且释放内存
                    PointerScope pointerScope = new PointerScope();
                    Frame newFrame = frame.clone();

                    //进行抓拍操作
                    if (capture.get() && newFrame != null) {
                        captureFuture = executor.submit(new CaptureTask(newFrame, capturesPath));
                        capture.set(false);
                    }

                    GrabEvent grabEvent = new GrabEvent(this,newFrame,pointerScope,grabber.getTimestamp());
                    frameFinishCount.put(grabEvent,new AtomicInteger(listeners.size()));
                    for (Listener listener : listeners) {
                        listener.fireAfterEventInvoked(grabEvent);
                    }
                }
            }
        }catch (Exception e){
            log.warn("Adapter [{}] throws an Exception!",name);
            e.printStackTrace();
        }finally {
            closeAllListeners();
            grabber.stop();
            VideoAdapterManagement.stopAdapter(this);
            log.info("Grabber ends for video rtmp:{}",rtmpPath);
        }
    }

    /**
     * @Description 结束推流
     * @author czx
     * @date 2019-04-23 23:50
     * @param []
     * @return void
     */
    @Override
    public void stop(){
        stop = true;
    }

    /**
     * @Description 进行抓拍
     * @author czx
     * @date 2019-04-23 23:50
     * @param []
     * @return void
     */
    public boolean capture(){
        if(capture.compareAndSet(false,true)){
            try{
                countDownLatch.await(3000,TimeUnit.MILLISECONDS);
                return captureFuture.get(5000,TimeUnit.MILLISECONDS);
            }catch (Exception e){
                e.printStackTrace();
                log.info("Capture failed!");
                return false;
            }finally {
                countDownLatch = new CountDownLatch(1);
            }
        }else{
            return false;
        }
    }

    /**
     * @Description 启动所有的listener
     * @author czx
     * @date 2019-04-23 23:49
     * @param []
     * @return void
     */
    private void startAllListeners(){
        log.info("Start all listeners");
        for(Listener listener:listeners){
            listener.start();
        }
    }

    /**
     * @Description 关闭所有listener
     * @author czx
     * @date 2019-04-23 23:49
     * @param []
     * @return void
     */
    private void closeAllListeners(){
        log.info("Close all listeners");
        for(Listener listener:listeners){
            listener.close();
        }
        listeners = new ArrayList<>();
    }



    /**
     * @Description 拉流grabber的初始化与启动
     * @author czx
     * @date 2018-12-03 23:00
     * @param []
     * @return void
     */
    private void grabberInit(){
        try {
            // 使用rtsp的时候需要使用 FFmpegFrameGrabber，不能再用FrameGrabber
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

    /**
     * @Description 根据listener的类型删除一个listener
     * @author czx
     * @date 2018-12-07 14:56
     * @param [name]
     * @return boolean
     */
    private boolean removeListener(Class listenerClass){
        Listener removedListener = null;
        for(Listener listener:listeners){
            if(listener.getClass()==listenerClass){
                removedListener = listener;
                break;
            }
        }
        if(removedListener==null){
            return false;
        }
        listeners.remove(removedListener);
        try {
            removedListener.close();
            return true;
        }catch (Exception e){
            return false;
        }
    }

    /**
     * @Description 重新开始录制视频
     * @author czx
     * @date 2018-12-07 13:58
     * @param [filename]
     * @return void
     */
    public void restartRecording(String filename){
        log.info("Restart recording. New filename is [{}]",filename);
        stopRecording();
        startRecording(filename);
    }
    
    /**
     * @Description 开始录制
     * @author czx
     * @date 2018-12-07 14:12
     * @param [filename]
     * @return void
     */
    private void startRecording(String filename){
        if(isRecording){
            log.warn("Video recording has already been started.");
        }else {
            RecordListener recordListener = new RecordListener(filename, getGrabber(),this,usePacket);
            addListener(recordListener);
            recordListener.start();
            isRecording = true;
        }
    }

    public void startRecording(){
        if(isRecording){
            log.warn("Video recording has already been started.");
        }else {
            String filePath = videoRootDir+rtmpPath.substring(rtmpPath.lastIndexOf("/")+1)+"/";
            String videoPath = filePath+"videos/";
            RecordListener recordListener = new RecordListener(videoPath+DirUtil.generateFilenameByDate()+".flv", getGrabber(),this,usePacket);
            recordListener.start();
            addListener(recordListener);
            isRecording = true;
        }
    }
    
    /**
     * @Description 停止录制
     * @author czx
     * @date 2018-12-07 14:13
     * @param []
     * @return void
     */
    public void stopRecording(){
        if(!isRecording){
            log.warn("Can not stop recording cause recording has not been started.");
        }else {
            removeListener(RecordListener.class);
            isRecording = false;
        }
    }

    /**
     * @Description 开始推流
     * @author czx
     * @date 2018-12-07 15:06
     * @param []
     * @return void
     */
    private void startPushing(){
        if(isPushing){
            log.warn("Video pushing has already been started.");
        }else {
            PushListener pushListener = new PushListener(rtmpPath,getGrabber(),this,usePacket);
            addListener(pushListener);
            pushListener.start();
            isPushing = true;
        }
    }

    /**
     * @Description 停止推流
     * @author czx
     * @date 2018-12-07 15:05
     * @param []
     * @return void
     */
    private void stopPushing(){
        if(!isPushing){
            log.warn("Can not stop pushing cause pushing has not been started.");
        }else {
            removeListener(PushListener.class);
            isPushing = false;
        }
    }

    /**
     * @Description 根据rtmp获取该视频流下的所有录像文件
     * @author CZX
     * @date 2018/11/30 18:55
     * @param [rtmpPath]
     * @return java.util.List<java.lang.String>
     */
    public List<String> getFiles(String rtmpPath){
        String path = Constants.getRootDir()+"videos/"+rtmpPath.substring(rtmpPath.lastIndexOf("/")+1);
        return DirUtil.getFileList(path);
    }

    /**
     * 获取抓拍文件
     * @param rtmpPath
     * @return
     */
    public List<String> getCaptures(String rtmpPath){
        String path = Constants.getRootDir()+"captures/"+rtmpPath.substring(rtmpPath.lastIndexOf("/")+1);
        return DirUtil.getFileList(path);
    }


    /**
     * 通过unref将内存引用减少1，并且引用为0时进行回收
     * @param event
     */
    public void unref(Event event,boolean isSuccess){
        executor.submit(new UnrefTask(frameFinishCount,event,isSuccess));
    }

    // 抓拍任务
    class CaptureTask implements Callable<Boolean> {

        private Frame frame;

        private String capturesPath;

        private CaptureTask(Frame frame, String capturesPath) {
            this.frame = frame;
            this.capturesPath = capturesPath;
        }

        @Override
        public Boolean call() {
            boolean dirExists = false;
            try{
                dirExists = DirUtil.judeDirExists(capturesPath);
            }catch (Exception e){
                e.printStackTrace();
                log.warn("judeDirExists() exception");
            }
            if (dirExists) {
                Mat mat = converter.convertToMat(frame);
                frame = null;
                long time = System.currentTimeMillis();
                log.info("Video capture is storing in [{}]!", this.capturesPath);
                boolean res = opencv_imgcodecs.imwrite(capturesPath + time + ".png", mat);
                countDownLatch.countDown();
                return res;
            }else{
                countDownLatch.countDown();
                return false;
            }
        }
    }

    // Unref任务
    class UnrefTask implements Runnable{

        final Map<Event,AtomicInteger> map;

        final Event event;

        final boolean success;

        UnrefTask(Map<Event, AtomicInteger> map, Event event,boolean isSuccess) {
            this.map = map;
            this.event = event;
            this.success = isSuccess;
        }

        @Override
        public void run() {
            if(event instanceof GrabEvent){
                int count = map.get(event).decrementAndGet();
                if(count==0) {
                    ((GrabEvent) event).getPointerScope().deallocate();
                    map.remove(event);
                }
            }else if(event instanceof PacketEvent){
                AVPacket avPacket = ((PacketEvent) event).getFrame();
                int count = map.get(((PacketEvent) event).getCountEvent()).decrementAndGet();
                if(!success){
                    avcodec.av_packet_unref(avPacket);
                }
                if(count==0){
                    map.remove(((PacketEvent) event).getCountEvent());
                    if(!avPacket.isNull()){
                        avcodec.av_packet_free(avPacket);
                    }
                }
            }else{
                log.warn("Unknown event type!");
            }
        }
    }
}
