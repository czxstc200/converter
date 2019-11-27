package cn.edu.bupt.listener;

import cn.edu.bupt.adapter.RTSPVideoAdapter;
import lombok.Data;

@Data
abstract class RTSPListener implements Listener{

    protected final RTSPVideoAdapter rTSPVideoAdapter;

    protected String name;

    public RTSPListener(RTSPVideoAdapter rTSPVideoAdapter, String name) {
        this.rTSPVideoAdapter = rTSPVideoAdapter;
        this.name = name;
    }
}
