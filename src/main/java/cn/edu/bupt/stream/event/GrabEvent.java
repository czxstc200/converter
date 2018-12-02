package cn.edu.bupt.stream.event;

import org.bytedeco.javacv.Frame;

/**
 * @Description: GrabEvent
 * @Author: czx
 * @CreateDate: 2018-12-02 15:57
 * @Version: 1.0
 */
public class GrabEvent extends Event {

    private Frame frame;

    public GrabEvent(Object source,Frame frame) {
        super(source);
        this.frame = frame;
    }

    public Frame getFrame() {
        return frame;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }
}
