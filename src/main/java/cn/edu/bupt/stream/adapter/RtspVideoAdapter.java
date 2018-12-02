package cn.edu.bupt.stream.adapter;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.beans.factory.annotation.Value;

/**
 * @Description: RtspVideoAdapter
 * @Author: czx
 * @CreateDate: 2018-12-02 17:52
 * @Version: 1.0
 */
public class RtspVideoAdapter extends VideoAdapter{

    private static long timestamp;

    private String videoRootDir;

    private FFmpegFrameGrabber grabber;

    public RtspVideoAdapter(String adapterName) {
        super(adapterName);
    }

    @Override
    public void start() {
        super.start();
    }
}
