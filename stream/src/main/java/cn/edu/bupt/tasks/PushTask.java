package cn.edu.bupt.tasks;

import cn.edu.bupt.event.Event;
import cn.edu.bupt.event.GrabEvent;
import cn.edu.bupt.event.PacketEvent;
import cn.edu.bupt.event.RTSPEvent;
import cn.edu.bupt.listener.PushListener;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class PushTask implements Runnable {

    private final BlockingQueue<Event> queue;

    private final AtomicBoolean executorStarted;

    public PushTask(BlockingQueue<Event> queue, AtomicBoolean executorStarted) {
        this.queue = queue;
        this.executorStarted = executorStarted;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Event event = queue.take();
                PushListener listener = (PushListener) ((RTSPEvent) event).getRtspListener();
                if (!listener.isStarted()) {
                    continue;
                }
                FFmpegFrameRecorder pushRecorder = listener.getRecorder();
                boolean success = false;
                try {
                    if (event instanceof PacketEvent) {
                        AVPacket avPacket = ((PacketEvent) event).getFrame();
                        if (avPacket.dts() < listener.getLastDTS()) {
                            continue;
                        } else {
                            listener.setLastDTS(avPacket.dts());
                        }
                        success = pushRecorder.recordPacket(avPacket);
                    } else if (event instanceof GrabEvent) {
                        pushRecorder.record(((GrabEvent) event).getFrame());
                        success = true;
                    } else {
                        throw new Exception("Unknown cn.edu.bupt.event type!");
                    }
                } catch (Exception e) {
                    log.warn("Push event failed for recorder [{}], exception: ", listener.getName(), e);
                } finally {
                    listener.getRTSPVideoAdapter().unref(event, success);
                }
                if (queue.isEmpty() && !executorStarted.get()) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Executor exception!");
        }
    }
}
