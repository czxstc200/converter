package cn.edu.bupt.stream.event;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacpp.avcodec.AVPacket;

/**
 * @Description: PacketEvent
 * @Author: czx
 * @CreateDate: 2019-04-23 16:53
 * @Version: 1.0
 */
public class PacketEvent extends Event{

    private long timestamp;

    private AVPacket frame;

    public PacketEvent(Object source, AVPacket frame, long timestamp) {
        super(source);
        this.frame = frame;
        this.timestamp = timestamp;
    }

    public AVPacket getFrame() {
        return frame;
    }

    public void setFrame(AVPacket frame) {
        this.frame = frame;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
