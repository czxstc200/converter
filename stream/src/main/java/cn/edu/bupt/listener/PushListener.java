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

    private final String rTMPPath;

    public PushListener(String rTMPPath, FFmpegFrameGrabber grabber, RTSPVideoAdapter rTSPVideoAdapter, boolean usePacket) {
        super(rTSPVideoAdapter, rTMPPath, grabber, PUSH_LISTENER_NAME, usePacket);
        this.rTMPPath = rTMPPath;
    }

    @Override
    protected void pushEvent(Event event) {
        super.submitTask(new PushTask(event));
    }

    @Override
    void close0() throws Exception {
        recorder.stop();
        isStarted = false;
    }
}
