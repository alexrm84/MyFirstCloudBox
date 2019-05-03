package alexrm84.myFirstCloudBox.server;

import java.io.IOException;
import java.sql.*;
import java.util.logging.*;

public class SQLHandler {
    private Connection connection;
    private PreparedStatement checkLoginAndPassword;
//    private static PreparedStatement psChangeNick;
    private final Logger logger = Logger.getLogger(Server.class.getName());

    public SQLHandler(){
        logger.setLevel(Level.INFO);
        logger.setUseParentHandlers(false);
        Handler handler;
        try {
            handler = new FileHandler("server_log.log",true);
            handler.setFormatter(new SimpleFormatter());
            handler.setLevel(Level.INFO);
            logger.addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, String.valueOf(e));
        }
    }

    public boolean checkLoginAndPassword(String login, String password){
        boolean check = false;
        try {
            checkLoginAndPassword.setString(1, login);
            checkLoginAndPassword.setString(2, password);
            ResultSet rs = checkLoginAndPassword.executeQuery();
            if (rs.next()) check = true;
            System.out.println(rs);
            rs.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, String.valueOf(e));
            e.printStackTrace();
        }
        return check;
    }

//    public static boolean changeNick(String oldNick, String newNick) {
//        try {
//            psChangeNick.setString(1, newNick);
//            psChangeNick.setString(2, oldNick);
//            psChangeNick.executeUpdate();return true;
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    public boolean connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:server_cloud_box.db");
//            stmt = connection.createStatement();
//            psChangeNick = connection.prepareStatement("UPDATE logins SET name = ? WHERE name = ?;");
            checkLoginAndPassword = connection.prepareStatement("SELECT * FROM logins WHERE login = ? AND password = ?;");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.valueOf(e));
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() {
//        try {
//            stmt.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
        try {
            checkLoginAndPassword.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, String.valueOf(e));
            e.printStackTrace();
        }
//        try {
//            psChangeNick.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
        try {
            connection.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, String.valueOf(e));
            e.printStackTrace();
        }
    }
}
