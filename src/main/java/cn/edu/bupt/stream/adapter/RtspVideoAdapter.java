package cn.edu.bupt.stream.adapter;

import cn.edu.bupt.stream.event.GrabEvent;
import cn.edu.bupt.stream.listener.Listener;
import cn.edu.bupt.stream.listener.PushListener;
import cn.edu.bupt.stream.listener.RecordListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private boolean capture;

    OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

    private static ExecutorService executor = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().namingPattern("Capture-pool-%d").daemon(false).build());


    public RtspVideoAdapter(){
        listeners = new ArrayList<>();
        isRecording = false;
        stop = false;
        videoRootDir = ROOT_DIR;
        timestamp = getZeroTimestamp();
        save = false;
        capture = false;
        // 设置日志打印等级
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
    }

    public RtspVideoAdapter(String adapterName) {
        this();
        name = adapterName;

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

    public void capture(){
        capture = true;
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
        if(save){
            judeDirExists(videoPath);
            startRecording(videoPath+generateFilenameByDate()+".flv");
        }
        startPushing();


        int count = 0;
        long startTime = 0L;
        while(!stop){
            if(isRecording&&timestamp<getZeroTimestamp()){
                timestamp = getZeroTimestamp();
                restartRecording(filePath+generateFilenameByDate()+".flv");
            }
            count++;
            if(count % 100 == 0){
                log.debug("Video[{}] counts={}",rtspPath,count);
            }
            Frame frame = null;
            try {
                frame = grabber.grabImage();
            }catch (Exception e){
                log.warn("Grab Image Exception!");
            }
            if(frame==null){
                stop();
                log.info("Video[{}] stopped!",rtspPath);
                break;
            }

            if (startTime == 0) {
                startTime = frame.timestamp;
            }
            if(capture){
                if(judeDirExists(capturesPath)){
                    opencv_core.Mat mat = converter.convertToMat(frame);
                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                long time = System.currentTimeMillis();
                                opencv_imgcodecs.imwrite(capturesPath + time + ".png", mat);
                                log.info("captured at timestamp {}", time);
                            }catch (Exception e){

                            }
                        }
                    });
                }else{

                }
                capture = !capture;
            }
            // 创建一个 timestamp用来写入帧中
            long videoTS = frame.timestamp;
            GrabEvent grabEvent = new GrabEvent(this,frame,videoTS);
            for(Listener listener:listeners){
                listener.fireAfterEventInvoked(grabEvent);
            }
        }
        log.info("Grabber ends for video rtsp:{}",rtspPath);
        closeAllListeners();

    }

    @Override
    public void stop(){
        stop = true;
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
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy_MM_dd_HH_mm");
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
            RecordListener recordListener = new RecordListener(filename, getGrabber());
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
            RecordListener recordListener = new RecordListener(videoPath+generateFilenameByDate()+".flv", getGrabber());
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
            PushListener pushListener = new PushListener(rtmpPath,getGrabber());
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

    public static void main(String[] args) {
        RtspVideoAdapter rtspVideoAdapter = new RtspVideoAdapter("rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov","rtmp://10.112.217.199/live360p/test2",false);

        try {
            rtspVideoAdapter.start();
        }catch (Exception e){

        }
    }
}
