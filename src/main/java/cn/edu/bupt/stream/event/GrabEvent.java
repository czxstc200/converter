package cn.edu.bupt.stream.event;

import org.bytedeco.javacv.Frame;

/**
 * @Description: GrabEvent，存储Frame
 * @Author: czx
 * @CreateDate: 2018-12-02 15:57
 * @Version: 1.0
 */
public class GrabEvent extends Event {

    private long timestamp;

    private Frame frame;

    public GrabEvent(Object source,Frame frame,long timestamp) {
        super(source);
        this.frame = frame;
        this.timestamp = timestamp;
    }

    public Frame getFrame() {
        return frame;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
