package cn.edu.bupt.event;

import cn.edu.bupt.listener.Listener;

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
