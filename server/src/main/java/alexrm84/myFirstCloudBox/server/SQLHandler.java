package alexrm84.myFirstCloudBox.server;

import java.io.IOException;
import java.sql.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SQLHandler {
    private Connection connection;
    private PreparedStatement checkLoginAndPassword;
//    private static PreparedStatement psChangeNick;
    private static final Logger logger = LogManager.getLogger(SQLHandler.class);

    public boolean checkLoginAndPassword(String login, String password){
        boolean check = false;
        try {
            checkLoginAndPassword.setString(1, login);
            checkLoginAndPassword.setString(2, password);
            ResultSet rs = checkLoginAndPassword.executeQuery();
            if (rs.next()) {
                check = true;
                logger.log(Level.INFO, "User: " + login + " is authorized");
            }
            rs.close();
        } catch (SQLException e) {
            logger.log(Level.ERROR, "Database query error: ", e);
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
            logger.log(Level.ERROR, "Database connection error: ", e);
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
            logger.log(Level.ERROR, "Database connection close error: ", e);
        }
//        try {
//            psChangeNick.close();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
        try {
            connection.close();
        } catch (SQLException e) {
            logger.log(Level.ERROR, "Database connection close error: ", e);
        }
    }
}
