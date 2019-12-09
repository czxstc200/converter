package cn.edu.bupt.event;

import cn.edu.bupt.listener.RTSPListener;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RTSPEvent extends Event {

    private RTSPListener rtspListener;

    private CountEvent countEvent;

    public RTSPEvent(Object source, CountEvent countEvent) {
        super(source);
        this.countEvent = countEvent;
    }
}
