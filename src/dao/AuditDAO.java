package dao;

import database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Records operational audit events for screening, testing, and issuance.
public class AuditDAO {
    public void log(Integer userId, String actionType, String entityType, String entityId, String details) {
        String sql = "INSERT INTO audit_log (user_id, action_type, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, userId);
            }
            ps.setString(2, actionType);
            ps.setString(3, entityType);
            ps.setString(4, entityId);
            ps.setString(5, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to write audit log.", e);
        }
    }
}
