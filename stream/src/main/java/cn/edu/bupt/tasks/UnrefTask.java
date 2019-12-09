package cn.edu.bupt.tasks;

import cn.edu.bupt.event.Event;
import cn.edu.bupt.event.GrabEvent;
import cn.edu.bupt.event.PacketEvent;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class UnrefTask implements Runnable {

    private final Map<Event, AtomicInteger> map;

    private final Event event;

    private final boolean success;

    public UnrefTask(Map<Event, AtomicInteger> map, Event event, boolean isSuccess) {
        this.map = map;
        this.event = event;
        this.success = isSuccess;
    }

    @Override
    public void run() {
        if (event instanceof GrabEvent) {
            int count = map.get(((GrabEvent) event).getCountEvent()).decrementAndGet();
            if (count == 0) {
                ((GrabEvent) event).getPointerScope().deallocate();
                map.remove(event);
            }
        } else if (event instanceof PacketEvent) {
            AVPacket avPacket = ((PacketEvent) event).getFrame();
            int count = map.get(((PacketEvent) event).getCountEvent()).decrementAndGet();
            if (!success) {
                avcodec.av_packet_unref(avPacket);
            }
            if (count == 0) {
                map.remove(((PacketEvent) event).getCountEvent());
                if (!avPacket.isNull()) {
                    avcodec.av_packet_free(avPacket);
                }
            }
        } else {
            log.warn("Unknown event type!");
        }
    }
}
