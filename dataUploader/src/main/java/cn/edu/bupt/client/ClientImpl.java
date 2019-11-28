package cn.edu.bupt.client;

import cn.edu.bupt.data.CameraInfo;
import cn.edu.bupt.util.Publish;
import cn.edu.bupt.util.TokenUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Slf4j
public class ClientImpl implements Client {

    private static final Client CLIENT_INSTANCE = new ClientImpl();

    private ClientImpl() {
        tableInit();
    }

    @Override
    public void sendTelemetries(String cameraName, String key, String value) {
        try {
            String token = TokenUtil.getToken(cameraName);
            Publish.sendTelemetries(token, key, value);
        } catch (Exception e) {
            log.error("Send telemetries failed, e:", e);
        }
    }

    @Override
    public void sendAttributes(CameraInfo cameraInfo) {
        String token = TokenUtil.getToken(cameraInfo.getName());
        Publish.sendAttributes(cameraInfo, token);
    }

    public static Client getClient() {
        return CLIENT_INSTANCE;
    }

    private void tableInit() {
        try {
            Connection c;
            Statement stmt;
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite::resource:cameras.db");
            log.info("Opened database successfully");
            stmt = c.createStatement();
            String sql = "CREATE TABLE camera " +
                    "(serialNumber text PRIMARY KEY NOT NULL," +
                    " token CHAR(50) NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch (Exception e) {
            log.error("Table init failed, e:", e);
        }
    }
}
