package cn.edu.bupt.listener;

import cn.edu.bupt.adapter.RTSPVideoAdapter;
import cn.edu.bupt.tasks.RecordTask;
import cn.edu.bupt.event.Event;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bytedeco.javacv.FFmpegFrameGrabber;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.edu.bupt.util.Constants.RECORD_LISTENER_NAME;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
public class RecordListener extends FFmpegListener {

    private boolean isStopped;
    private long startTimestamp = -1;

    public RecordListener(String filename, FFmpegFrameGrabber grabber, RTSPVideoAdapter rTSPVideoAdapter, boolean usePacket) {
        super(rTSPVideoAdapter, filename, grabber, RECORD_LISTENER_NAME, usePacket);
        this.isStopped = false;
    }

    @Override
    void close0() throws Exception {
        isStarted = false;
        isStopped = true;
    }

    @Override
    protected void pushEvent(Event event) {
        super.submitTask(new RecordTask(event));
    }
}
