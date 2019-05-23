package cn.edu.bupt.event;

import cn.edu.bupt.listener.Listener;

/**
 * @Description: RTSPEvent
 * @Author: czx
 * @CreateDate: 2019-05-15 18:17
 * @Version: 1.0
 */
public class RTSPEvent extends Event {

    private Listener listener;

    public RTSPEvent(Object source) {
        super(source);
    }

    public Listener getListener() {
        return listener;
    }

    public RTSPEvent setListener(Listener listener) {
        this.listener = listener;
        return this;
    }
}
