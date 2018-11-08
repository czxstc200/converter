package cn.edu.bupt;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacv.*;

import javax.swing.*;

/**
 * Created by CZX on 2018/10/12.
 */
public class convert {

    static boolean exit  = false;

    public static void begin(String rtspPath,String rtmpPath) throws Exception {
        System.out.println("start...");
//        String rtmpPath = "rtmp://10.112.77.117/liveapp/hello";
//        String rtspPath = "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov";

        //ffmpeg -f rtsp -rtsp_transport tcp -i rtsp://admin:leeking123@192.168.1.64:554/h264/ch1/main/av_stream rtmp://casic207-pc1/live360p/ss1
        // ffmpeg -i  rtsp://admin:123123@192.168.1.64:554/h264/ch1/main/av_stream -vcodec copy -acodec copy -f flv rtmp://casic207-pc1/live360p/ss1
        int audioRecord =0; // 0 = 不录制，1=录制
        boolean saveVideo = false;
        test(rtmpPath,rtspPath,audioRecord,saveVideo);
//        test2(rtspPath,rtmpPath,25);
        System.out.println("end...");
    }

//    public static void test2(String inputFile,String outputFile,int v_rs) throws Exception{
//        Loader.load(opencv_objdetect.class);
//        Long startTime=0L;
//        FrameGrabber grabber =FFmpegFrameGrabber.createDefault(inputFile);
//        try {
//            grabber.start();
//        } catch (Exception e) {
//            try {
//                grabber.restart();
//            } catch (Exception e1) {
//                throw e;
//            }
//        }
//
//        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
//        Frame grabframe =grabber.grab();
//        opencv_core.IplImage grabbedImage =null;
//        if(grabframe!=null){
//            System.out.println("取到第一帧");
//            grabbedImage = converter.convert(grabframe);
//            opencv_imgcodecs.cvSaveImage("hello.jpg", grabbedImage);
//        }else{
//            System.out.println("没有取到第一帧");
//        }
//        //如果想要保存图片,可以使用 opencv_imgcodecs.cvSaveImage("hello.jpg", grabbedImage);来保存图片
//
//        FrameRecorder recorder;
//        try {
//            recorder = FrameRecorder.createDefault(outputFile, 1280, 720);
//        } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
//            throw e;
//        }
//        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // avcodec.AV_CODEC_ID_H264
//        recorder.setFormat("flv");
//        recorder.setFrameRate(v_rs);
//        recorder.setGopSize(v_rs);
//        System.out.println("准备开始推流...");
//        try {
//            recorder.start();
//        } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
//            try {
//                System.out.println("录制器启动失败，正在重新启动...");
//                if(recorder!=null)
//                {
//                    System.out.println("尝试关闭录制器");
//                    recorder.stop();
//                    System.out.println("尝试重新开启录制器");
//                    recorder.start();
//                }
//
//            } catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {
//                System.out.println("开启失败");
//                throw e;
//            }
//        }
//        System.out.println("开始推流");
//        CanvasFrame frame = new CanvasFrame("camera", CanvasFrame.getDefaultGamma() / grabber.getGamma());
//        System.out.println("开始！");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setAlwaysOnTop(true);
//        while (frame.isVisible() && (grabframe=grabber.grab()) != null) {
//            System.out.println("推流...");
//            frame.showImage(grabframe);
//            grabbedImage = converter.convert(grabframe);
//            Frame rotatedFrame = converter.convert(grabbedImage);
//
//            if (startTime == 0) {
//                startTime = System.currentTimeMillis();
//            }
//            recorder.setTimestamp(1000 * (System.currentTimeMillis() - startTime));//时间戳
//            if(rotatedFrame!=null){
//                recorder.record(rotatedFrame);
//            }
//
//            Thread.sleep(40);
//        }
//        frame.dispose();
//        recorder.stop();
//        recorder.release();
//        grabber.stop();
//
//    }

    public static void test(String rtmpPath,String rtspPath,int audioRecord,boolean saveVideo ) throws Exception  {
        //FrameGrabber grabber = FrameGrabber.createDefault(0); // 本机摄像头 默认
        // 使用rtsp的时候需要使用 FFmpegFrameGrabber，不能再用 FrameGrabber

        int width = 1920,height = 1080;
        FFmpegFrameGrabber grabber = FFmpegFrameGrabber.createDefault(rtspPath);
        grabber.setOption("rtsp_transport", "tcp"); // 使用tcp的方式，不然会丢包很严重
//        grabber.setVideoBitrate(4096);
//        grabber.setVideoFrameNumber(15);
        // 一直报错的原因！！！就是因为是 2560 * 1440的太大了。。
//        grabber.setImageWidth(width);
//        grabber.setImageHeight(height);
        System.out.println("grabber start");
        grabber.start();
        //FrameRecorder recorder = FrameRecorder.createDefault(rtmpPath, 640,480,0);
        // 流媒体输出地址，分辨率（长，高），是否录制音频（0:不录制/1:录制）
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(rtmpPath, width, height, audioRecord);

//        recorder.setInterleaved(true);
//        recorder.setVideoOption("crf","28");
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // 28
        recorder.setFrameRate(15);
        recorder.setVideoOption("preset", "ultrafast");
//        recorder.setVideoBitrate(4096);
        recorder.setFormat("flv"); // rtmp的类型
//        recorder.setFrameRate(10);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); // yuv420p
        System.out.println("recorder start");
        recorder.start();
        //
        OpenCVFrameConverter.ToIplImage conveter = new OpenCVFrameConverter.ToIplImage();
        System.out.println("all start!!");
        int count = 0;
        while(!exit){
            count++;
            Frame frame = grabber.grabImage();
            if(frame == null){
                continue;
            }
            if(count % 100 == 0){
                System.out.println("count="+count);
            }
            recorder.record(frame);
        }
        grabber.stop();
        grabber.release();
        recorder.stop();
        recorder.release();
        System.out.println("Convert finished!");
    }
}
