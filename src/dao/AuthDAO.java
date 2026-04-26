package dao;

import database.DBConnection;
import database.PasswordUtil;
import model.LoginResult;
import model.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Simple local authentication against the users table.
public class AuthDAO {
    public LoginResult login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return LoginResult.failure("Enter username and password.");
        }

        String sql = "SELECT user_id, username, password_hash, first_name, last_name, role, active FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return LoginResult.failure("Invalid username or password.");
                }
                if (!rs.getBoolean("active")) {
                    return LoginResult.failure("This account is inactive.");
                }
                if (!PasswordUtil.matches(password, rs.getString("password_hash"))) {
                    return LoginResult.failure("Invalid username or password.");
                }
                return LoginResult.success(new UserAccount(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            return LoginResult.failure("Login failed: " + e.getMessage());
        }
    }
}
