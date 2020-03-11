package cn.edu.bupt.event;

import cn.edu.bupt.listener.RTSPListener;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.javacv.Frame;

@Data
@EqualsAndHashCode(callSuper = true)
public class GrabEvent extends RTSPEvent {

    private final long timestamp;

    private final PointerScope pointerScope;

    private final Frame frame;

    public GrabEvent(Object source, Frame frame, PointerScope pointerScope, long timestamp, CountEvent countEvent, RTSPListener listener) {
        super(source, listener, countEvent);
        this.frame = frame;
        this.pointerScope = pointerScope;
        this.timestamp = timestamp;
    }
}
