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

    private String name;
    private String serialNumber;
    private String RTMP;

    public CameraInfo(String name,String serialNumber, String RTMP) {
        this.name = name;
        this.serialNumber = serialNumber;
        this.RTMP = RTMP;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"name\":\"")
                .append(name).append('\"');
        sb.append(",\"serialNumber\":\"")
                .append(serialNumber).append('\"');
        sb.append(",\"RTMP\":\"")
                .append(RTMP).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
