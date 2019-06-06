//package cn.edu.bupt;
//
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.Statement;
//
//public class InsertData {
//    public static void main( String args[] )
//    {
//        Connection c = null;
//        Statement stmt = null;
//        try {
//            Class.forName("org.sqlite.JDBC");
//            c = DriverManager.getConnection("jdbc:sqlite::resource:cameras.db");
//            c.setAutoCommit(false);
//            System.out.println("Opened database successfully");
//
//            stmt = c.createStatement();
//            String sql = "INSERT INTO camera (serialNumber, token) " +
//                    "VALUES ('aaaaa','ynufq4EbcyXhouj31brz');";
//            stmt.executeUpdate(sql);
//
//            stmt.close();
//            c.commit();
//            c.close();
//        } catch ( Exception e ) {
//            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
//            System.exit(0);
//        }
//        System.out.println("Records created successfully");
//    }
//}
