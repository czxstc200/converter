package cn.edu.bupt.tasks;

import cn.edu.bupt.event.Event;
import cn.edu.bupt.event.GrabEvent;
import cn.edu.bupt.event.PacketEvent;
import cn.edu.bupt.event.RTSPEvent;
import cn.edu.bupt.listener.RecordListener;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

@Slf4j
public class RecordTask implements Runnable {

    private final Queue<Event> queue;

    public RecordTask(Queue<Event> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        Set<RecordListener> recordListeners = new HashSet<>();
        while (!queue.isEmpty()) {
            Event event = queue.poll();
            if (event == null || ((RTSPEvent) event).getRtspListener() == null) {
                continue;
            }
            RecordListener listener = (RecordListener) ((RTSPEvent) event).getRtspListener();
            FFmpegFrameRecorder fileRecorder = listener.getRecorder();
            if (listener.isStopped()) {
                recordListeners.add(listener);
            }
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
                } else if (event instanceof PacketEvent) {
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
        // 关闭recorder
        if (!recordListeners.isEmpty()) {
            Iterator<RecordListener> iterator = recordListeners.iterator();
            iterator.forEachRemaining(listener -> {
                FFmpegFrameRecorder recorder = listener.getRecorder();
                try {
                    recorder.stop();
                } catch (Exception e) {
                    log.warn("Failed to stop a file recorder, e:", e);
                } finally {
                    listener.getCloseCountDownLatch().countDown();
                }
            });
        }
    }
}
