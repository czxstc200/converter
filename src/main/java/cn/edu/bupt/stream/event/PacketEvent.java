package cn.edu.bupt.stream.event;


import org.bytedeco.ffmpeg.avcodec.AVPacket;

/**
 * @Description: PacketEvent，存储AVPacket
 * @Author: czx
 * @CreateDate: 2019-04-23 16:53
 * @Version: 1.0
 */
public class PacketEvent extends Event{

    private final AVPacket frame;

    private CountEvent countEvent;

    public PacketEvent(Object source, AVPacket frame,CountEvent countEvent) {
        super(source);
        this.frame = frame;
        this.countEvent = countEvent;
    }

    public PacketEvent(Object source, AVPacket frame) {
        super(source);
        this.frame = frame;
    }

    public AVPacket getFrame() {
        return frame;
    }

    public CountEvent getCountEvent() {
        return countEvent;
    }

    public PacketEvent setCountEvent(CountEvent countEvent) {
        this.countEvent = countEvent;
        return this;
    }

    public static class CountEvent extends Event{

        public CountEvent() {
            super(new Object());
        }
    }

}
