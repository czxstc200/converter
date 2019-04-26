package cn.edu.bupt.stream.adapter;

import cn.edu.bupt.stream.event.GrabEvent;
import cn.edu.bupt.stream.event.PacketEvent;
import cn.edu.bupt.stream.listener.Listener;
import cn.edu.bupt.stream.listener.PushListener;
import cn.edu.bupt.stream.listener.RecordListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /*
    是否使用AVPacket的方式直接进行拉流与推流
     */
    private boolean usePacket;

    OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

    private ExecutorService executor = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().namingPattern("Capture-pool-%d").daemon(false).build());

    /**
     * 用于获取capture的future
     */
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private Future<Boolean> captureFuture;

    public RtspVideoAdapter(){
        listeners = new ArrayList<>();
        isRecording = false;
        stop = false;
        videoRootDir = ROOT_DIR;
        timestamp = getZeroTimestamp();
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

        log.info("RtspVideoAdapter starts for {rtsp is {},rtmp is {}}",rtspPath,rtmpPath);
        grabberInit();
        log.info("Grabber starts for video rtsp:{}",rtspPath);
        startAllListeners();

        String filePath = videoRootDir+rtmpPath.substring(rtmpPath.lastIndexOf("/")+1,rtmpPath.length())+"/";
        String capturesPath = filePath+"captures/";
        String videoPath = filePath+"videos/";

        judeDirExists(filePath);
        judeDirExists(videoPath);
        judeDirExists(capturesPath);
        if(save){
            startRecording(videoPath+generateFilenameByDate()+".flv");
        }

        startPushing();


        int count = 0;
        int nullFrames = 0;
        while(!stop){
            //记录帧数
            count++;
            if(count % 100 == 0){
                log.debug("Video[{}] counts={}",rtspPath,count);
            }

            //时间超过零点进行视频录像的切分
            if(isRecording&&timestamp<getZeroTimestamp()){
                timestamp = getZeroTimestamp();
                restartRecording(filePath+generateFilenameByDate()+".flv");
            }

            //使用AVPacket进行推流，目前这种模式下不能对数据帧进行处理
            if(usePacket) {
                avcodec.AVPacket pkt = null;
                pkt = grabber.grabPacket();
                if (pkt == null) {
                    nullFrames++;
                    //连续5帧都是null时判断已经停止推流
                    if (nullFrames >= 5) {
                        stop();
                        log.info("Video[{}] stopped!", rtspPath);
                        break;
                    }
                } else {
                    nullFrames = 0;
                }

                //AVPacket采用计数法进行内存的回收，因此在每一个listener进行处理时，
                //都需要创建一个新的ref。由于JavaCV中的方法自带unref，如果没有创建
                //ref，一个listener处理完后就有可能回收内存
                for (Listener listener : listeners) {
                    avcodec.AVPacket newPkt = avcodec.av_packet_alloc();
                    avcodec.av_packet_ref(newPkt, pkt);
                    PacketEvent grabEvent = new PacketEvent(this, newPkt);
                    listener.fireAfterEventInvoked(grabEvent);
                }
                avcodec.av_packet_unref(pkt);
            }else{//使用传统方式进行处理，效率较低（增加了编解码的时间），但是可以对画面frame进行处理
                Frame frame = null;
                try {
                    frame = grabber.grabImage();
                }catch (Exception e){
                    log.warn("Grab Image Exception!");
                }
                if(frame==null){
                    nullFrames++;
                    if(nullFrames >= 5){
                        stop();
                        log.info("Video[{}] stopped!",rtspPath);
                        break;
                    }
                }
                GrabEvent grabEvent = new GrabEvent(this,frame,grabber.getTimestamp());
                //进行抓拍操作
                if(capture.get()&&judeDirExists(capturesPath)&&frame!=null){
                    Frame frame1 = frame.clone();
                    Future<Boolean> future = executor.submit(new CaptureTask(frame1,capturesPath));
                    captureFuture = future;
                    countDownLatch.countDown();
                    capture.set(false);
                }

                for(Listener listener:listeners){
                    listener.fireAfterEventInvoked(grabEvent);
                }
            }
        }
        closeAllListeners();
        log.info("Grabber ends for video rtsp:{}",rtspPath);
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
                countDownLatch.await();
                return captureFuture.get(1000,TimeUnit.MILLISECONDS);
            }catch (Exception e){
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
        listeners.removeAll(listeners);
    }

    /**
     * @Description 判断文件夹是否存在,不存在则创建
     * @author CZX
     * @date 2018/11/30 12:24
     * @param [filename]
     * @return boolean
     */
    private static boolean judeDirExists(String filename) throws Exception{
        File file = new File(filename);
        if (file.exists()) {
            if (file.isDirectory()) {
                log.warn("dir[{}] exists",file.getName());
                return true;
            } else {
                log.error("the same name file[{}] exists, can not create dir",file.getName());
                throw new Exception("the same name file exists");
            }
        }else{
            log.info("dir[{}] not exists, create it",file.getName());
            return file.mkdir();
        }
    }

    /**
     * @Description 根据日期生成视频的文件名
     * @author CZX
     * @date 2018/11/30 12:30
     * @param []
     * @return java.lang.String
     */
    private static String generateFilenameByDate(){
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date date=new Date();
        String dateStringParse = sdf.format(date);
        return dateStringParse;
    }

    /**
     * @Description 获取明天的零点时间戳
     * @author CZX
     * @date 2018/11/30 14:36
     * @param []
     * @return java.lang.Long
     */
    private static Long getZeroTimestamp(){
        Long currentTimestamps=System.currentTimeMillis();
        Long oneDayTimestamps= Long.valueOf(60*60*24*1000);
        return currentTimestamps-(currentTimestamps+60*60*8*1000)%oneDayTimestamps+oneDayTimestamps;
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

    /**
     * @Description 根据名字删除一个listener
     * @author czx
     * @date 2018-12-07 14:56
     * @param [name]
     * @return void
     */
    private void removeListener(String name){
        Listener removedListener = null;
        for(Listener listener:listeners){
            if(listener.getName().equals(name)){
                listener.close();
                removedListener = listener;
            }
        }
        listeners.remove(removedListener);
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
    public void startRecording(String filename){
        if(isRecording){
            log.warn("Video recording has already been started.");
        }else {
            RecordListener recordListener = new RecordListener(filename, getGrabber(),usePacket);
            addListener(recordListener);
            recordListener.start();
            isRecording = true;
        }
    }

    public void startRecording(){
        if(isRecording){
            log.warn("Video recording has already been started.");
        }else {
            String filePath = videoRootDir+rtmpPath.substring(rtmpPath.lastIndexOf("/")+1,rtmpPath.length())+"/";
            String videoPath = filePath+"videos/";
            RecordListener recordListener = new RecordListener(videoPath+generateFilenameByDate()+".flv", getGrabber(),usePacket);
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
            removeListener(RECORD_LISTENER_NAME);
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
    public void startPushing(){
        if(isPushing){
            log.warn("Video pushing has already been started.");
        }else {
            PushListener pushListener = new PushListener(rtmpPath,getGrabber(),usePacket);
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
    public void stopPushing(){
        if(!isPushing){
            log.warn("Can not stop pushing cause pushing has not been started.");
        }else {
            removeListener(PUSH_LISTENER_NAME);
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
        String path = ROOT_DIR+"videos/"+rtmpPath.substring(rtmpPath.lastIndexOf("/")+1,rtmpPath.length());
        File file = new File(path);
        File[] files = file.listFiles();
        List<String> fileList = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                fileList.add(files[i].getName());
            }
        }
        return fileList;
    }

    /**
     * 获取抓拍文件
     * @param rtmpPath
     * @return
     */
    public List<String> getCaptures(String rtmpPath){
        String path = ROOT_DIR+"captures/"+rtmpPath.substring(rtmpPath.lastIndexOf("/")+1,rtmpPath.length());
        File file = new File(path);
        File[] files = file.listFiles();
        List<String> fileList = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                fileList.add(files[i].getName());
            }
        }
        return fileList;
    }

    // 抓拍任务
    class CaptureTask implements Callable<Boolean> {

        private Frame frame;

        private String capturesPath;

        public CaptureTask(Frame frame, String capturesPath) {
            this.frame = frame;
            this.capturesPath = capturesPath;
        }

        @Override
        public Boolean call() {
            opencv_core.Mat mat = converter.convertToMat(frame);
            frame = null;
            long time = System.currentTimeMillis();
            log.info("Video capture is storing in [{}]!", this.capturesPath);
            return opencv_imgcodecs.imwrite(capturesPath + time + ".png", mat);
        }
    }
}
