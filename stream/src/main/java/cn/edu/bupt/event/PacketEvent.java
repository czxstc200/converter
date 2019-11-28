package cn.edu.bupt.event;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bytedeco.ffmpeg.avcodec.AVPacket;

@Data
@EqualsAndHashCode(callSuper = true)
public class PacketEvent extends RTSPEvent {

    private final AVPacket frame;

    private CountEvent countEvent;

    public PacketEvent(Object source, AVPacket frame, CountEvent countEvent) {
        super(source);
        this.frame = frame;
        this.countEvent = countEvent;
    }

    public static class CountEvent extends Event {

        public CountEvent() {
            super(new Object());
        }
    }

}
