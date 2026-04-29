package dao;

import database.DBConnection;
import database.PasswordUtil;
import model.LoginResult;
import model.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Simple local authentication against the users table.
public class AuthDAO {
    public LoginResult login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return LoginResult.failure("Enter username and password.");
        }

        String sql = "SELECT user_id, staff_id, username, password_hash, first_name, last_name, role, active FROM users WHERE username = ?";
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
                        rs.getString("staff_id"),
                        rs.getString("username"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("role"),
                        rs.getBoolean("active"),
                        null
                ));
            }
        } catch (SQLException e) {
            return LoginResult.failure("Login failed: " + e.getMessage());
        }
    }

    public List<UserAccount> getAllUsers() {
        List<UserAccount> users = new ArrayList<>();
        String sql = "SELECT user_id, staff_id, username, first_name, last_name, role, active, created_at FROM users ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UserAccount user = new UserAccount(
                        rs.getInt("user_id"),
                        rs.getString("staff_id"),
                        rs.getString("username"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("role"),
                        rs.getBoolean("active"),
                        rs.getTimestamp("created_at")
                );
                users.add(user);
            }
        } catch (SQLException e) {
            System.out.println("Error fetching users: " + e.getMessage());
        }
        return users;
    }

    private String generateStaffId(String role) {
        String prefix = "NBDA-ADM-";
        if (!"ADMIN".equalsIgnoreCase(role)) {
            prefix = "NBDA-STF-";
        }
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1) + 1;
                    return prefix + String.format("%03d", count);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error generating staff ID: " + e.getMessage());
        }
        return prefix + "001";
    }

    public boolean createUser(String username, String password, String firstName, String lastName, String role) {
        String staffId = generateStaffId(role);
        String sql = "INSERT INTO users (staff_id, username, password_hash, first_name, last_name, role) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, staffId);
            ps.setString(2, username.trim());
            ps.setString(3, PasswordUtil.hash(password));
            ps.setString(4, firstName.trim());
            ps.setString(5, lastName.trim());
            ps.setString(6, role);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error creating user: " + e.getMessage());
            return false;
        }
    }

    public boolean deactivateUser(int userId) {
        String sql = "UPDATE users SET active = FALSE WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error deactivating user: " + e.getMessage());
            return false;
        }
    }

    public boolean changePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error changing password: " + e.getMessage());
            return false;
        }
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking username: " + e.getMessage());
        }
        return false;
    }
}
