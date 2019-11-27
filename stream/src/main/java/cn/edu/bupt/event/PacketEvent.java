package cn.edu.bupt.event;


import org.bytedeco.ffmpeg.avcodec.AVPacket;

public class PacketEvent extends RTSPEvent{

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
