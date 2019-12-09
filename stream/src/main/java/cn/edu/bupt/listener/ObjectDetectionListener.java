package cn.edu.bupt.listener;

import cn.edu.bupt.adapter.RTSPVideoAdapter;
import cn.edu.bupt.event.Event;
import cn.edu.bupt.tasks.ObjectDetectionTask;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static cn.edu.bupt.util.Constants.OBJECT_DETECTION_LISTENER_NAME;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
public class ObjectDetectionListener extends RTSPListener {

    private final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private long lastUpdateTimestamp;

    public ObjectDetectionListener(RTSPVideoAdapter rtspVideoAdapter) {
        super(rtspVideoAdapter, OBJECT_DETECTION_LISTENER_NAME);
        lastUpdateTimestamp = System.currentTimeMillis();
    }

    @Override
    public void fireAfterEventInvoked(Event event) {
        long now = System.currentTimeMillis();
        if(now-lastUpdateTimestamp>3000) {
            lastUpdateTimestamp = now;
            executorService.submit(new ObjectDetectionTask(event, converter, rTSPVideoAdapter.getName()));
        } else {
            rTSPVideoAdapter.unref(event, false);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void close() {

    }
}
