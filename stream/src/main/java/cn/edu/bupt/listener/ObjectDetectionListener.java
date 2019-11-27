//package cn.edu.bupt.listener;
//
//import cn.edu.bupt.adapter.RTSPVideoAdapter;
//import cn.edu.bupt.event.Event;
//import cn.edu.bupt.event.GrabEvent;
//import lombok.extern.slf4j.Slf4j;
//import okhttp3.*;
//import org.bytedeco.javacv.Frame;
//import org.bytedeco.javacv.OpenCVFrameConverter;
//import org.bytedeco.opencv.global.opencv_imgcodecs;
//import org.bytedeco.opencv.opencv_core.Mat;
//
//import java.io.File;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import static cn.edu.bupt.util.Constants.OBJECT_DETECTION_LISTENER_NAME;
//import static cn.edu.bupt.util.Constants.ROOT_DIR;
//
///**
// * @Description: ObjectDetectionListener
// * @Author: czx
// * @CreateDate: 2019-06-29 11:07
// * @Version: 1.0
// */
//@Slf4j
//public class ObjectDetectionListener extends RTSPListener {
//    private final RTSPVideoAdapter rtspVideoAdapter;
//    private static final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
//    private String rtmpPath;
//    ExecutorService executorService = Executors.newSingleThreadExecutor();
//    long lastUpdateTimestamp = 0L;
//
//    public ObjectDetectionListener(RTSPVideoAdapter rtspVideoAdapter) {
//        lastUpdateTimestamp = System.currentTimeMillis();
//        this.rtspVideoAdapter = rtspVideoAdapter;
//    }
//
//    public ObjectDetectionListener(RTSPVideoAdapter rtspVideoAdapter, String rtmpPath) {
//        this.rtspVideoAdapter = rtspVideoAdapter;
//        this.rtmpPath = rtmpPath;
//    }
//
//    @Override
//    public void fireAfterEventInvoked(Event event) throws Exception {
//        long now = System.currentTimeMillis();
//        if(now-lastUpdateTimestamp>2000) {
//            lastUpdateTimestamp = now;
//            executorService.submit(new Runnable() {
//                @Override
//                public void run() {
//                    if (event instanceof GrabEvent) {
//                        try {
//                            Frame frame = ((GrabEvent) event).getFrame();
//                            Mat mat = converter.convertToMat(frame);
//                            long time = System.currentTimeMillis();
//                            boolean res = opencv_imgcodecs.imwrite(ROOT_DIR + "CvTemp/" + time + ".jpg", mat);
//                            if (res) {
//                                Request request = new Request.Builder()
//                                        .url("http://10.112.217.199:8400/classify?threshold=0.8")
//                                        .post(RequestBody.create(MediaType.parse("image/jpeg"), new File(ROOT_DIR + "CvTemp/" + time + ".jpg")))
//                                        .build();
//                                try {
//                                    OkHttpClient mOkHttpClient = new OkHttpClient.Builder().build();
//                                    Response response = mOkHttpClient.newCall(request).execute();
//                                    String result = response.body().string();
//                                    log.info("Object Detection result : [{}]",result);
//                                    rtspVideoAdapter.send(result);
////                                    client.sendTelemetries(rTMPPath,"ObjectDetection",result);
//
//                                } catch (Exception e) {
//
//                                }
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        } finally {
//                            rtspVideoAdapter.unref(event, false);
//                        }
//                    }
//                }
//            });
//        }
//    }
//
//    @Override
//    public String getName() {
//        return OBJECT_DETECTION_LISTENER_NAME;
//    }
//
//    @Override
//    public void start() {
//
//    }
//
//    @Override
//    public void close() {
//
//    }
//}
