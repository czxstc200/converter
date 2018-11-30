package cn.edu.bupt.util;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: 进行视频的推流与存储
 * @Author: CZX
 * @CreateDate: 2018/11/30 10:48
 * @Version: 1.0
 */

@Slf4j
@Component
public class VideoConverter {

    private static long timestamp;


    private static String videoRootDir;

    @Value("${convert.dir}")
    private void getVideoRootDir(String rootDir){videoRootDir = rootDir;}

    private static Map<String,Boolean> exitMap = new ConcurrentHashMap<>();

    private static Map<String,Boolean> saveMap = new ConcurrentHashMap<>();

    /**
     * @Description 视频转换
     * @author CZX
     * @date 2018/11/30 11:16
     * @param [rtspPath, rtmpPath, audioRecord, saveVideo]
     * @return void
     */
    public static void convert(String rtspPath,String rtmpPath,int audioRecord,boolean saveVideo) throws Exception {
        log.info("Video converter starts for {rtsp is {} and rtmp is {}}",rtspPath,rtmpPath);
        exitMap.put(rtmpPath,false);
        saveMap.put(rtmpPath,saveVideo);
        push(rtmpPath,rtspPath,audioRecord,saveVideo);
        log.info("Video converter ends for {rtsp is {} and rtmp is {}}",rtspPath,rtmpPath);
    }

    /**
     * @Description push
     * @author CZX
     * @date 2018/11/30 11:16
     * @param [rtmpPath, rtspPath, audioRecord, saveVideo]
     * @return void
     */
    private static void push(String rtmpPath,String rtspPath,int audioRecord,boolean saveVideo ) throws Exception  {

        // 使用rtsp的时候需要使用 FFmpegFrameGrabber，不能再用 FrameGrabber
        FFmpegFrameGrabber grabber = FFmpegFrameGrabber.createDefault(rtspPath);
        // 使用tcp的方式，不然会丢包很严重
        grabber.setOption("rtsp_transport", "tcp");
        grabber.start();
        log.info("Grabber starts for video rtsp:{}",rtspPath);
        // 推流record
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(rtmpPath, grabber.getImageWidth(), grabber.getImageHeight(), audioRecord);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setFormat("flv");
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.start();
        log.info("Recorder starts for rtmp:{}",rtmpPath);
        //存储record
        String filePath = videoRootDir+rtmpPath.substring(rtmpPath.lastIndexOf("/")+1,rtmpPath.length())+"/";
        if(!judeDirExists(filePath)){
            log.error("dir[{}] not exists",filePath);
            throw new Exception("dir["+filePath+"] not exists");
        }
        String filename;
        FFmpegFrameRecorder fileRecorder = null;

        //开始record
        int count = 0;
        while(!exitMap.get(rtmpPath)){
            count++;
            Frame frame = grabber.grabImage();
            if(frame == null){
                continue;
            }
            if(count % 100 == 0){
                log.debug("Video[{}] counts={}",rtmpPath,count);
            }
            recorder.record(frame);

            //判断是否进行录像
            if(saveMap.get(rtmpPath)){
                filename = generateFilenameByDate() + ".mp4";
                if(fileRecorder==null){
                    timestamp = getZeroTimestamp();
                    if(saveMap.get(rtmpPath)) {
                        fileRecorder = new FFmpegFrameRecorder(filePath+filename,grabber.getImageWidth(),grabber.getImageHeight(),audioRecord);
                        fileRecorder.setFormat("mp4");
                        fileRecorder.setFrameRate(grabber.getFrameRate());
                        fileRecorder.start();
                        log.info("File recorder started. File is being saved to dir[{}]", filePath);
                    }
                }
                fileRecorder.record(frame);
                if(timestamp<getZeroTimestamp()){
                    log.info("Date changed");
                    timestamp = getZeroTimestamp();
                    filename = generateFilenameByDate()+".mp4";
                    fileRecorder.stop();
                    fileRecorder.release();
                    fileRecorder = new FFmpegFrameRecorder(filePath+filename,grabber.getImageWidth(),grabber.getImageHeight(),audioRecord);
                    fileRecorder.setFormat("mp4");
                    fileRecorder.setFrameRate(grabber.getFrameRate());
                    fileRecorder.start();
                }
            }else if(fileRecorder !=null) {
                log.info("File recorder stopped. File has been saved to dir[{}]", filePath);
                fileRecorder.stop();
                fileRecorder.release();
                fileRecorder = null;
            }
        }

        //退出record
        exitMap.remove(rtmpPath);
        grabber.stop();
        grabber.release();
        recorder.stop();
        recorder.release();
        if(fileRecorder!=null) {
            fileRecorder.stop();
            fileRecorder.release();
        }
        log.info("Video[rtsp:{},rtmp:{}] Converter stopped!",rtspPath,rtmpPath);
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
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy_MM_dd_hh_mm");
        Date date=new Date();
        String dateStringParse = sdf.format(date);
        return dateStringParse;
    }

    /**
     * @Description 根据rtmp更改录像状态
     * @author CZX
     * @date 2018/11/30 14:23
     * @param [rtmpPath]
     * @return void
     */
    public static void record(String rtmpPath){
        boolean save = saveMap.get(rtmpPath);
        saveMap.put(rtmpPath,!save);
    }

    /**
     * @Description 停止推流
     * @author CZX
     * @date 2018/11/30 15:14
     * @param [rtmpPath]
     * @return void
     */
    public static void stopConvert(String rtmpPath){
        exitMap.put(rtmpPath,true);
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
}
