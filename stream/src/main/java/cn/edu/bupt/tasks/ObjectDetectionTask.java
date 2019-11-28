package cn.edu.bupt.tasks;

import cn.edu.bupt.client.Client;
import cn.edu.bupt.client.ClientImpl;
import cn.edu.bupt.event.Event;
import cn.edu.bupt.event.GrabEvent;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.File;

import static cn.edu.bupt.util.Constants.*;

@Slf4j
public class ObjectDetectionTask implements Runnable {

    private final Event event;
    private final OpenCVFrameConverter.ToIplImage converter;
    private final String cameraName;
    private static final Client client = ClientImpl.getClient();
    private static final String keyName = "ObjectDetection";

    public ObjectDetectionTask(Event event, OpenCVFrameConverter.ToIplImage converter, String cameraName) {
        this.event = event;
        this.converter = converter;
        this.cameraName = cameraName;
    }

    @Override
    public void run() {
        if (event instanceof GrabEvent) {
            try {
                Frame frame = ((GrabEvent) event).getFrame();
                Mat mat = converter.convertToMat(frame);
                long time = System.currentTimeMillis();
                boolean res = opencv_imgcodecs.imwrite(OBJECT_DETECTION_TEMP_DIR + time + ".png", mat);
                if (res) {
                    Request request = new Request.Builder()
                            .url(OBJECT_DETECTION_URL)
                            .post(RequestBody.create(MediaType.parse("image/jpeg"), new File(ROOT_DIR + "CvTemp/" + time + ".jpg")))
                            .build();
                    OkHttpClient mOkHttpClient = new OkHttpClient.Builder().build();
                    Response response = mOkHttpClient.newCall(request).execute();
                    if (response.body() == null) {
                        throw new Exception("Response Body is null");
                    }
                    String result = response.body().string();
                    log.info("Object Detection result : [{}]", result);
                    client.sendTelemetries(cameraName, keyName, result);
                }
            } catch (Exception e) {
                log.error("Exception happened when send request to object detection server, e:", e);
            } finally {
                ((GrabEvent) event).getRtspListener().getRTSPVideoAdapter().unref(event, false);
            }
        }
    }
}
