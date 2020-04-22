package cn.edu.bupt.tasks;

import cn.edu.bupt.event.Event;
import cn.edu.bupt.event.GrabEvent;
import cn.edu.bupt.event.PacketEvent;
import cn.edu.bupt.event.RTSPEvent;
import cn.edu.bupt.listener.PushListener;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class PushTask implements Task {

    private final Event event;

    public PushTask(Event event) {
        this.event = event;
    }

    private static final int priority = 1;

    private static final long endTime = System.currentTimeMillis()+5000;

    @Override
    public void run() {
        try {
            PushListener listener = (PushListener) ((RTSPEvent) event).getRtspListener();
            FFmpegFrameRecorder pushRecorder = listener.getRecorder();
            boolean success = false;
            try {
                if (event instanceof PacketEvent) {
                    AVPacket avPacket = ((PacketEvent) event).getFrame();
//                    Random random = new Random();
//                    double time = Math.abs(Math.sqrt(5)*random.nextGaussian()+10);
//                    Thread.sleep((long) time);
                    success = pushRecorder.recordPacket(avPacket);
                } else if (event instanceof GrabEvent) {
//                    Random random = new Random();
//                    double time = Math.abs(Math.sqrt(5)*random.nextGaussian()+10);
//                    Thread.sleep((long) time);
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
        } catch (Exception e) {
            log.warn("Executor exception :", e);
        }
    }


    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }
}
