//package cn.edu.bupt;
//
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.ResultSet;
//import java.sql.Statement;
//
//public class SelectTable {
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
//            ResultSet rs = stmt.executeQuery( "SELECT * FROM camera;" );
//            while ( rs.next() ) {
//                String serialNumber = rs.getString("serialNumber");
//                String  name = rs.getString("token");
//                System.out.println( "ID = " + serialNumber );
//                System.out.println( "Token = " + name );
//            }
//            rs.close();
//            stmt.close();
//            c.close();
//        } catch ( Exception e ) {
//            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
//            System.exit(0);
//        }
//        System.out.println("Operation done successfully");
//    }
//}
