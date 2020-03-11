package cn.edu.bupt.listener;

import cn.edu.bupt.adapter.RTSPVideoAdapter;
import cn.edu.bupt.event.Event;
import cn.edu.bupt.tasks.ObjectDetectionTask;
import cn.edu.bupt.util.DirUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static cn.edu.bupt.util.Constants.*;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
public class ObjectDetectionListener extends RTSPListener {

    private final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final Set<String> adapterFlags = new HashSet<>();
    private long lastUpdateTimestamp;

    public ObjectDetectionListener(RTSPVideoAdapter rtspVideoAdapter) {
        super(rtspVideoAdapter, OBJECT_DETECTION_LISTENER_NAME);
        lastUpdateTimestamp = System.currentTimeMillis();
        DirUtil.judgeDirExists(OBJECT_DETECTION_TEMP_DIR);
    }

    @Override
    public void fireAfterEventInvoked(Event event) {
        long now = System.currentTimeMillis();
        String rTMP = rTSPVideoAdapter.getRTMPPath();
        if(now-lastUpdateTimestamp>3000) {
            if (!adapterFlags.contains(rTMP)) {
                adapterFlags.add(rTMP);
                lastUpdateTimestamp = now;
                executorService.submit(new ObjectDetectionTask(event, converter, rTSPVideoAdapter.getName()));
            }
        } else {
            rTSPVideoAdapter.unref(event, false);
        }
        if (now-lastUpdateTimestamp>6000) {
            adapterFlags.remove(rTMP);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void close() {

    }

    public static void removeFlag(String rTMP) {
        adapterFlags.remove(rTMP);
    }
}
