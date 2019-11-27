package cn.edu.bupt.event;

import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.javacv.Frame;

public class GrabEvent extends RTSPEvent {

    private final long timestamp;

    private final PointerScope pointerScope;

    private final Frame frame;

    public GrabEvent(Object source,Frame frame,PointerScope pointerScope,long timestamp) {
        super(source);
        this.frame = frame;
        this.pointerScope = pointerScope;
        this.timestamp = timestamp;
    }

    public Frame getFrame() {
        return frame;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public PointerScope getPointerScope() {
        return pointerScope;
    }
}
