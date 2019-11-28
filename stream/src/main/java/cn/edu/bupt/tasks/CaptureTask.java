package cn.edu.bupt.tasks;

import cn.edu.bupt.util.DirUtil;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import java.util.concurrent.Callable;

@Slf4j
public class CaptureTask implements Callable<Boolean> {

    private static final OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();

    private Frame frame;

    private String capturesPath;

    public CaptureTask(Frame frame, String capturesPath) {
        this.frame = frame;
        this.capturesPath = capturesPath;
    }

    @Override
    public Boolean call() {
        boolean dirExists = false;
        try {
            dirExists = DirUtil.judgeDirExists(capturesPath);
        } catch (Exception e) {
            log.error("Exception happened when judge whether dir exists, e:", e);
        }
        if (dirExists) {
            Mat mat = converter.convertToMat(frame);
            frame = null;
            long time = System.currentTimeMillis();
            log.info("Video capture is storing in [{}]!", this.capturesPath);
            return opencv_imgcodecs.imwrite(capturesPath + time + ".png", mat);
        } else {
            log.error("Capture dir not exists, dir:[{}]", capturesPath);
            return false;
        }
    }
}
