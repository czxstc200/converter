package cn.edu.bupt.listener;

import cn.edu.bupt.adapter.RTSPVideoAdapter;
import cn.edu.bupt.event.Event;
import cn.edu.bupt.event.RTSPEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class FFmpegListener extends RTSPListener {

    protected final int queueThreshold;
    protected final long offerTimeout;
    protected final BlockingQueue<Event> queue;
    protected final FFmpegFrameRecorder recorder;
    protected boolean usePacket;
    protected AVFormatContext fc;
    protected boolean isInit;
    protected boolean isStarted;

    FFmpegListener(RTSPVideoAdapter rTSPVideoAdapter, String dst, FFmpegFrameGrabber grabber, String name, int queueThreshold, long offerTimeout, BlockingQueue<Event> queue,boolean usePacket) {
        super(rTSPVideoAdapter, name);
        this.queueThreshold = queueThreshold;
        this.offerTimeout = offerTimeout;
        this.queue = queue;
        this.isInit = false;
        this.isStarted = false;
        this.usePacket = usePacket;
        recorder = fFmpegRecorderInit(dst, grabber);
    }

    @Override
    public void start() {
        try {
            if (isInit) {
                recorder.start(fc);
                startExecutor();
                isStarted = true;
                log.info("FFmpeg recorder started, recorder name: [{}]", name);
            } else {
                throw new Exception("You must initialize the FFmpeg recorder before start it");
            }
        } catch (Exception e) {
            log.error("FFmpeg recorder failed to start, recorder name: [{}], exception: ", name, e);
        }
    }

    @Override
    public void close() {
        try {
            close0();
            log.info("FFmpeg recorder stopped, recorder name: [{}]", name);
        } catch (Exception e) {
            log.error("FFmpeg recorder failed to close, recorder name: [{}], exception: ", name, e);
        }
    }

    @Override
    public void fireAfterEventInvoked(Event event) throws Exception {
        if (isStarted) {
            ((RTSPEvent) event).setRtspListener(this);
            pushEvent(event);
        } else {
            log.warn("Failed to fire listener [{}]. You should start this recorder before you start pushing", name);
            throw new Exception("Failed to fire the listener!");
        }
    }

    abstract void close0() throws Exception;

    abstract void startExecutor();

    private void pushEvent(Event event) {
        try {
            if (queue.size() > this.queueThreshold) {
                log.warn("Queue size is greater than threshold. queue size={} threshold={} timestamp={}", queue.size(), this.queueThreshold, System.currentTimeMillis());
            }
            if (queue.size() < 2 * this.queueThreshold) {
                queue.offer(event, this.offerTimeout, TimeUnit.MILLISECONDS);
                log.trace("Inserting event into queue, queue size:[{}]", queue.size());
            } else {
                log.warn("Clear queue");
                queue.clear();
            }
        } catch (Exception e) {
            log.warn("Event data was not accepted by the queue");
        }
    }

    private FFmpegFrameRecorder fFmpegRecorderInit(String dst, FFmpegFrameGrabber grabber) {
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(dst, grabber.getImageWidth(), grabber.getImageHeight(), 0);
        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setFormat("flv");
        if (usePacket) {
            fc = grabber.getFormatContext();
        } else {
            fc = null;
        }
        this.isInit = true;
        return recorder;
    }
}
