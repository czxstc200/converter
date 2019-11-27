package cn.edu.bupt.listener;

import cn.edu.bupt.adapter.RTSPVideoAdapter;
import cn.edu.bupt.adapter.tasks.RecordTask;
import cn.edu.bupt.event.Event;
import cn.edu.bupt.event.GrabEvent;
import cn.edu.bupt.event.PacketEvent;
import cn.edu.bupt.event.RTSPEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.edu.bupt.util.Constants.RECORD_LISTENER_NAME;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
public class RecordListener extends FFmpegListener {

    private boolean isStopped;
    private long startTimestamp = -1;
    private CountDownLatch closeCountDownLatch = new CountDownLatch(1);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new BasicThreadFactory.Builder().namingPattern("Record-Pool-%d").daemon(false).build());
    private static final AtomicBoolean executorStarted = new AtomicBoolean(false);
    private static final BlockingQueue<Event> queue = new LinkedBlockingQueue<>();

    public RecordListener(String filename, FFmpegFrameGrabber grabber, RTSPVideoAdapter rTSPVideoAdapter, boolean usePacket) {
        super(rTSPVideoAdapter, filename, grabber, RECORD_LISTENER_NAME, 1024, 100L, queue, usePacket);
        this.isStopped = false;
    }

    @Override
    void close0() throws Exception {
        isStarted = false;
        isStopped = true;
        closeCountDownLatch.await(10000L, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void startExecutor() {
        if (executorStarted.compareAndSet(false, true)) {
            executor.scheduleAtFixedRate(new RecordTask(queue), 1, 5, TimeUnit.SECONDS);
        }
    }
}
