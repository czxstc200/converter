package cn.edu.bupt.tasks;

import cn.edu.bupt.adapter.RTSPVideoAdapter;
import cn.edu.bupt.client.*;
import cn.edu.bupt.event.Event;
import cn.edu.bupt.event.GrabEvent;
import cn.edu.bupt.listener.ObjectDetectionListener;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static cn.edu.bupt.util.Constants.*;

@Slf4j
public class ObjectDetectionTask implements Runnable {

    private final Event event;
    private final OpenCVFrameConverter.ToIplImage converter;
    private final String cameraName;
    private static final Client client = ClientImpl.getClient();
    private static final String keyName = "ObjectDetection";
    private static final NettyClientPool clientPool = new NettyClientPool(new NettyClientFactory(OBJECT_DETECTION_HOST, OBJECT_DETECTION_PORT));

    public ObjectDetectionTask(Event event, OpenCVFrameConverter.ToIplImage converter, String cameraName) {
        this.event = event;
        this.converter = converter;
        this.cameraName = cameraName;
    }

    @Override
    public void run() {
        if (event instanceof GrabEvent) {
            final RTSPVideoAdapter adapter = ((GrabEvent) event).getRtspListener().getRTSPVideoAdapter();
            try {
                final String rTMP = adapter.getRTMPPath();
                Frame frame = ((GrabEvent) event).getFrame();
                Mat mat = converter.convertToMat(frame);
                long time = System.currentTimeMillis();
                boolean res = opencv_imgcodecs.imwrite(OBJECT_DETECTION_TEMP_DIR + time + ".png", mat);
                if (res) {
                    NettyClient nettyClient = clientPool.borrowObject();
                    nettyClient.post(OBJECT_DETECTION_URL, Unpooled.wrappedBuffer(Files.readAllBytes(new File(ROOT_DIR + "CvTemp/" + time + ".png").toPath())),
                            new PromiseCallback<FullHttpResponse>() {
                                @Override
                                public void onSuccess(FullHttpResponse response) {
                                    String result = response.content().toString(Charset.forName("UTF-8"));
                                    System.out.println(result);
                                    log.info("Object Detection result : [{}]", result);
                                    if (!StringUtils.isBlank(result)) {
                                        client.sendTelemetries(cameraName, keyName, result);
                                    }
                                    ObjectDetectionListener.removeFlag(rTMP);
                                    clientPool.returnObject(nettyClient);
                                }

                                @Override
                                public void onFailed(FullHttpResponse response) {
                                    log.error("Object Detection failed");
                                    ObjectDetectionListener.removeFlag(rTMP);
                                    clientPool.returnObject(nettyClient);
                                }
                            });

                }
            } catch (Exception e) {
                log.error("Exception happened when send request to object detection server, e:", e);
            } finally {
                adapter.unref(event, false);
            }
        }
    }
}
