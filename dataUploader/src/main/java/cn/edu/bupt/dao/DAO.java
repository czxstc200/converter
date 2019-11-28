package cn.edu.bupt.dao;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;

@Slf4j
public class DAO {

    private static Connection connection;

    private static Connection getConn() {
        connection = null;
        try {
            Class.forName("org.sqlite.JDBC"); //加载对应驱动
        } catch (ClassNotFoundException e) {
            log.error("Exception happened in DAO, e:", e);
        }
        //url为本地创建数据库表的目录文件
        String url = "jdbc:sqlite::resource:cameras.db";

        try {
            connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            log.error("Get Connection failed, e:", e);
        }
        return connection;
    }

    //增加数据
    public static int insert(String serialNumber, String token) {
        Connection conn = getConn();
        int i = 0;
        String sql = "insert into camera (serialNumber,token) values(?,?)";
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, serialNumber);
            pstmt.setString(2, token);
            i = pstmt.executeUpdate();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return i;
    }

    //更新数据
    public static int update(String serialNumber, String token) {
        Connection conn = getConn();
        int i = 0;
        String sql = "update camera set serialNumber='" + serialNumber + "' where token=" + token;
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement(sql);
            i = pstmt.executeUpdate();
            System.out.println("result: " + i);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return i;
    }

    //获取数据
    public static String getAll(String serialNumber) {
        String token = null;
        Connection conn = getConn();
        String sql = "select token from camera where serialNumber='" + serialNumber + "'";
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                token = rs.getString("token");
            }
            rs.close();
            return token;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //删除数据
    public static int delete(String serialNumber) {
        Connection conn = getConn();
        int i = 0;
        String sql = "delete from camera where serialNumber='" + serialNumber + "'";
        try {
            PreparedStatement preparedStatement;
            preparedStatement = conn.prepareStatement(sql);
            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return i;
    }
}
