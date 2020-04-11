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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class FFmpegListener extends RTSPListener {

    protected final FFmpegFrameRecorder recorder;
    protected boolean usePacket;
    protected AVFormatContext fc;
    protected boolean isInit;
    protected boolean isStarted;
    protected final ExecutorService executor;

    FFmpegListener(RTSPVideoAdapter rTSPVideoAdapter, String dst, FFmpegFrameGrabber grabber, String name, boolean usePacket) {
        super(rTSPVideoAdapter, name);
        this.isInit = false;
        this.isStarted = false;
        this.usePacket = usePacket;
        List<ExecutorService> executorServices = rTSPVideoAdapter.getWorkers();
        int index = Math.abs(rTSPVideoAdapter.hashCode() + this.hashCode())%executorServices.size();
        this.executor = executorServices.get(index);
        recorder = fFmpegRecorderInit(dst, grabber);
    }

    @Override
    public void start() {
        try {
            if (isInit) {
                recorder.start(fc);
                log.info("FFmpeg recorder started, recorder name: [{}]", name);
                isStarted = true;
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

    protected abstract void pushEvent(Event event);

    protected void submitTask(Runnable runnable) {
        System.out.println("record: "+ executor);
        executor.submit(runnable);
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
