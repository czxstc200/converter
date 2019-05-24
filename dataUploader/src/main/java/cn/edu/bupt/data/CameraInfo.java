package cn.edu.bupt.data;

import lombok.Data;

/**
 * @Description: CameraInfo
 * @Author: czx
 * @CreateDate: 2019-05-24 15:25
 * @Version: 1.0
 */
@Data
public class CameraInfo {

    private String serialNumber;
    private String RTSP;

    public CameraInfo(String serialNumber, String RTSP) {
        this.serialNumber = serialNumber;
        this.RTSP = RTSP;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"serialNumber\":\"")
                .append(serialNumber).append('\"');
        sb.append(",\"RTSP\":\"")
                .append(RTSP).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
