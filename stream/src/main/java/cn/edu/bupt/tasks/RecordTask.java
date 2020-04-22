package cn.edu.bupt.tasks;

import cn.edu.bupt.event.Event;
import cn.edu.bupt.event.GrabEvent;
import cn.edu.bupt.event.PacketEvent;
import cn.edu.bupt.event.RTSPEvent;
import cn.edu.bupt.listener.RecordListener;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.*;

@Slf4j
public class RecordTask implements Task {

    private final Event event;

    public RecordTask(Event event) {
        this.event = event;
    }

    private static final int priority = 2;

    private static final long endTime = System.currentTimeMillis()+5000;

    @Override
    public void run() {
        RecordListener listener = (RecordListener) ((RTSPEvent) event).getRtspListener();
        FFmpegFrameRecorder fileRecorder = listener.getRecorder();
        boolean success = false;
        try {
            if (event instanceof GrabEvent) {
                // 时间戳设置
                long timestamp = ((GrabEvent) event).getTimestamp();
                if (listener.getStartTimestamp() == -1) {
                    listener.setStartTimestamp(timestamp);
                    timestamp = 0;
                    fileRecorder.setTimestamp(timestamp);
                } else {
                    timestamp -= listener.getStartTimestamp();
                }
                if (timestamp > fileRecorder.getTimestamp()) {
                    fileRecorder.setTimestamp(timestamp);
                }
                fileRecorder.record(((GrabEvent) event).getFrame());
                success = true;
//                Random random = new Random();
//                double time = Math.abs(Math.sqrt(15)*random.nextGaussian()+30);
//                Thread.sleep((long) time);
            } else if (event instanceof PacketEvent) {
//                Random random = new Random();
//                double time = Math.abs(Math.sqrt(15)*random.nextGaussian()+30);
//                Thread.sleep((long) time);
                success = fileRecorder.recordPacket(((PacketEvent) event).getFrame());
            } else {
                log.warn("Unknown event type!");
            }
        } catch (Exception e) {
            log.warn("Record event failed for Recorder:{}, e: ", listener.getName(), e);
        } finally {
            listener.getRTSPVideoAdapter().unref(event, success);
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
