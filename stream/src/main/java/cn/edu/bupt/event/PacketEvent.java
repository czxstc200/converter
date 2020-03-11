package cn.edu.bupt.event;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bytedeco.ffmpeg.avcodec.AVPacket;

@Data
@EqualsAndHashCode(callSuper = true)
public class PacketEvent extends RTSPEvent {

    private final AVPacket frame;

    public PacketEvent(Object source, AVPacket frame, CountEvent countEvent) {
        super(source, null, countEvent);
        this.frame = frame;
    }
}
