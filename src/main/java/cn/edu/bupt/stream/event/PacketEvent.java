package cn.edu.bupt.stream.event;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacpp.avcodec.AVPacket;

/**
 * @Description: PacketEvent，存储AVPacket
 * @Author: czx
 * @CreateDate: 2019-04-23 16:53
 * @Version: 1.0
 */
public class PacketEvent extends Event{

    private AVPacket frame;

    public PacketEvent(Object source, AVPacket frame) {
        super(source);
        this.frame = frame;
    }

    public AVPacket getFrame() {
        return frame;
    }

    public void setFrame(AVPacket frame) {
        this.frame = frame;
    }

}
