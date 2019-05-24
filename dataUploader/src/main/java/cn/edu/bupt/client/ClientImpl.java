package cn.edu.bupt.client;

import cn.edu.bupt.data.CameraInfo;
import cn.edu.bupt.util.Publish;
import cn.edu.bupt.util.TokenUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;


public class ClientImpl implements Client{

    public static boolean first = true;

    public ClientImpl() {
        if(first=true){
            Connection c = null;
            Statement stmt = null;
            try {
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite::resource:cameras.db");
                System.out.println("Opened database successfully");

                stmt = c.createStatement();
                String sql = "CREATE TABLE camera " +
                        "(serialNumber text PRIMARY KEY     NOT NULL," +
                        " token           CHAR(50)    NOT NULL)";
                stmt.executeUpdate(sql);
                stmt.close();
                c.close();
            } catch ( Exception e ) {
                System.err.println( e.getClass().getName() + ": " + e.getMessage() );
                System.exit(0);
            }
            System.out.println("Table created successfully");
            first = false;
        }
    }

    @Override
    public void sendTelemetries(CameraInfo cameraInfo,String key,String value) {
        String token = TokenUtil.getToken(cameraInfo);
        Publish.sendTelemetries(token,key,value);
    }

    @Override
    public void sendAttributes(CameraInfo cameraInfo) {
        String token = TokenUtil.getToken(cameraInfo);
        Publish.sendAttributes(cameraInfo,token);
    }
}
