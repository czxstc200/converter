package cn.edu.bupt.listener;

import cn.edu.bupt.adapter.RTSPVideoAdapter;
import cn.edu.bupt.tasks.PushTask;
import cn.edu.bupt.event.Event;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bytedeco.javacv.FFmpegFrameGrabber;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.edu.bupt.util.Constants.PUSH_LISTENER_NAME;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
public class PushListener extends FFmpegListener {

    private long lastDTS = 0;
    private final String rTMPPath;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().namingPattern("Push-Pool-%d").daemon(true).build());
    private static final AtomicBoolean executorStarted = new AtomicBoolean(false);
    private static final BlockingQueue<Event> queue = new LinkedBlockingDeque<>();

    public PushListener(String rTMPPath, FFmpegFrameGrabber grabber, RTSPVideoAdapter rTSPVideoAdapter) {
        super(rTSPVideoAdapter, rTMPPath, grabber, PUSH_LISTENER_NAME, 1024, 100L, queue);
        this.rTMPPath = rTMPPath;
    }

    @Override
    void close0() throws Exception {
        recorder.stop();
        isStarted = false;
    }

    @Override
    protected void startExecutor() {
        if (executorStarted.compareAndSet(false, true)) {
            executor.submit(new PushTask(queue, executorStarted));
        }
    }
}
