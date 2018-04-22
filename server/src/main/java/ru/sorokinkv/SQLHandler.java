package ru.sorokinkv;

import java.sql.*;

public class SQLHandler {
    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement psAddHistory;
    private static PreparedStatement psGetHistory;


    public static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:server/data.db");
        stmt = connection.createStatement();
        psAddHistory = connection.prepareStatement("INSERT INTO history (from_user_id, to_user_id, message) VALUES (?, ?, ?);");
        psGetHistory = connection.prepareStatement("SELECT message FROM history WHERE to_user_id = -1 OR to_user_id = ?;");
        // createBaseUsers();
    }

    public static void createBaseUsers() {
        for (int i = 1; i <= 30; i++) {
            addNewUser("login" + i, "pass" + i, "nick" + i);
        }
    }

    public static String getNickByLoginPass(String login, String pass) {
        try {
            int passHash = pass.hashCode();
            ResultSet rs = stmt.executeQuery(String.format("SELECT nick FROM users WHERE login = '%s' AND password = '%d';", login, passHash));
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getIdByNick(String nick) {
        try {
            ResultSet rs = stmt.executeQuery(String.format("SELECT id FROM users WHERE nick = '%s';", nick));
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean addNewUser(String login, String pass, String nick) {
        try {
            int passHash = pass.hashCode();
            stmt.executeUpdate(String.format("INSERT INTO users (login, password, nick) VALUES ('%s', '%d', '%s');", login, passHash, nick));
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void addHistory(int fromUserId, int toUserId, String msg) {
        try {
            psAddHistory.setInt(1, fromUserId);
            psAddHistory.setInt(2, toUserId);
            psAddHistory.setString(3, msg);
            psAddHistory.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getHistory(int userId) {
        try {
            StringBuilder sb = new StringBuilder();
            psGetHistory.setInt(1, userId);
            ResultSet rs = psGetHistory.executeQuery();
            while (rs.next()) {
                sb.append(rs.getString(1)).append("\n");
            }

            return sb.substring(0, sb.length() - 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
