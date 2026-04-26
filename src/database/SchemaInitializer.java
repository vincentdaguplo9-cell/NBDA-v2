package database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Applies the single canonical schema file used by the app.
public final class SchemaInitializer {
    private static final Path SCHEMA_FILE = Paths.get("schema.sql");

    private SchemaInitializer() {
    }

    public static void ensureSchema() {
        createDatabaseIfMissing();
        try (Connection conn = DBConnection.getConnection()) {
            applySchemaFile(conn);
            ensureUsersTable(conn);
            upgradeDonorNameSchema(conn);
            upgradeOperationalSchema(conn);
            seedDefaultUsers(conn);
        } catch (IOException e) {
            throw new RuntimeException("Schema file not found: " + SCHEMA_FILE, e);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to initialize database schema from schema.sql.", e);
        }
    }

    private static void createDatabaseIfMissing() {
        String databaseName = DBConnection.getConfiguredDatabaseName().replace("`", "");
        try (Connection conn = DBConnection.getServerConnection();
             Statement st = conn.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS `" + databaseName + "`");
        } catch (SQLException e) {
            throw new RuntimeException("Unable to create database `" + databaseName + "`.", e);
        }
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static void applySchemaFile(Connection conn) throws IOException, SQLException {
        String sqlText = Files.readString(SCHEMA_FILE, StandardCharsets.UTF_8);
        List<String> statements = splitStatements(sqlText);
        try (Statement st = conn.createStatement()) {
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty() || startsWithIgnoreCase(trimmed, "CREATE DATABASE")
                        || startsWithIgnoreCase(trimmed, "USE ")) {
                    continue;
                }
                try {
                    st.execute(trimmed);
                } catch (SQLException ex) {
                    if (isIgnorableSchemaError(trimmed, ex)) {
                        continue;
                    }
                    throw ex;
                }
            }
        }
    }

    private static void upgradeDonorNameSchema(Connection conn) throws SQLException {
        if (!tableExists(conn, "donors")) {
            return;
        }

        ensureColumn(conn, "donors", "first_name", "ALTER TABLE donors ADD COLUMN first_name VARCHAR(50) NOT NULL");
        ensureColumn(conn, "donors", "last_name", "ALTER TABLE donors ADD COLUMN last_name VARCHAR(50) NOT NULL");

        if (columnExists(conn, "donors", "full_name")) {
            backfillSplitNames(conn);
            dropIndexIfExists(conn, "donors", "uq_donor_identity");
            dropColumnIfExists(conn, "donors", "full_name");
        }

        if (!indexExists(conn, "donors", "uq_donor_identity")) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE UNIQUE INDEX uq_donor_identity ON donors (first_name, last_name, birth_date, contact_no)");
            }
        }
    }

    private static void ensureUsersTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "user_id INT PRIMARY KEY AUTO_INCREMENT," +
                            "username VARCHAR(40) NOT NULL UNIQUE," +
                            "password_hash VARCHAR(64) NOT NULL," +
                            "first_name VARCHAR(50) NOT NULL," +
                            "last_name VARCHAR(50) NOT NULL," +
                            "role ENUM('ADMIN', 'STAFF') NOT NULL DEFAULT 'STAFF'," +
                            "active TINYINT(1) NOT NULL DEFAULT 1," +
                            "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                            ")"
            );
        }
    }

    private static void upgradeOperationalSchema(Connection conn) throws SQLException {
        ensureColumn(conn, "donor_screening", "screened_by",
                "ALTER TABLE donor_screening ADD COLUMN screened_by INT NULL AFTER donor_id");
        ensureColumn(conn, "donor_screening", "guardian_consent_provided",
                "ALTER TABLE donor_screening ADD COLUMN guardian_consent_provided TINYINT(1) NOT NULL DEFAULT 0 AFTER slept_hours");

        ensureColumn(conn, "blood_inventory", "tti_tested_at",
                "ALTER TABLE blood_inventory ADD COLUMN tti_tested_at DATETIME NULL AFTER tti_overall_status");
        ensureColumn(conn, "blood_inventory", "tti_tested_by",
                "ALTER TABLE blood_inventory ADD COLUMN tti_tested_by INT NULL AFTER tti_tested_at");
        ensureColumn(conn, "blood_inventory", "tti_test_kit",
                "ALTER TABLE blood_inventory ADD COLUMN tti_test_kit VARCHAR(120) NULL AFTER tti_tested_by");
        ensureColumn(conn, "blood_inventory", "crossmatch_status",
                "ALTER TABLE blood_inventory ADD COLUMN crossmatch_status ENUM('PENDING', 'COMPATIBLE', 'INCOMPATIBLE', 'NOT_REQUIRED') NOT NULL DEFAULT 'PENDING' AFTER disposition_notes");
        ensureColumn(conn, "blood_inventory", "issue_patient_name",
                "ALTER TABLE blood_inventory ADD COLUMN issue_patient_name VARCHAR(150) NULL AFTER crossmatch_status");
        ensureColumn(conn, "blood_inventory", "patient_hospital_no",
                "ALTER TABLE blood_inventory ADD COLUMN patient_hospital_no VARCHAR(60) NULL AFTER issue_patient_name");
        ensureColumn(conn, "blood_inventory", "request_hospital",
                "ALTER TABLE blood_inventory ADD COLUMN request_hospital VARCHAR(150) NULL AFTER patient_hospital_no");
        ensureColumn(conn, "blood_inventory", "requesting_physician",
                "ALTER TABLE blood_inventory ADD COLUMN requesting_physician VARCHAR(120) NULL AFTER request_hospital");
        ensureColumn(conn, "blood_inventory", "blood_request_no",
                "ALTER TABLE blood_inventory ADD COLUMN blood_request_no VARCHAR(60) NULL AFTER requesting_physician");
        ensureColumn(conn, "blood_inventory", "issued_by",
                "ALTER TABLE blood_inventory ADD COLUMN issued_by INT NULL AFTER issued_at");
        ensureColumn(conn, "blood_inventory", "issue_notes",
                "ALTER TABLE blood_inventory ADD COLUMN issue_notes VARCHAR(255) NULL AFTER issued_by");

        try (Statement st = conn.createStatement()) {
            st.execute(
                    "CREATE TABLE IF NOT EXISTS audit_log (" +
                            "audit_id INT PRIMARY KEY AUTO_INCREMENT," +
                            "event_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "user_id INT NULL," +
                            "action_type VARCHAR(40) NOT NULL," +
                            "entity_type VARCHAR(40) NOT NULL," +
                            "entity_id VARCHAR(60) NULL," +
                            "details VARCHAR(255) NULL" +
                            ")"
            );
        }
    }

    private static void seedDefaultUsers(Connection conn) throws SQLException {
        if (userExists(conn, "admin")) {
            return;
        }

        String sql = "INSERT INTO users (username, password_hash, first_name, last_name, role, active) VALUES (?, ?, ?, ?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "admin");
            ps.setString(2, PasswordUtil.hash("admin"));
            ps.setString(3, "System");
            ps.setString(4, "Administrator");
            ps.setString(5, "ADMIN");
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "staff");
            ps.setString(2, PasswordUtil.hash("staff"));
            ps.setString(3, "Blood");
            ps.setString(4, "Staff");
            ps.setString(5, "STAFF");
            ps.executeUpdate();
        }
    }

    private static void backfillSplitNames(Connection conn) throws SQLException {
        String selectSql = "SELECT donor_id, full_name, first_name, last_name FROM donors";
        String updateSql = "UPDATE donors SET first_name = ?, last_name = ? WHERE donor_id = ?";
        try (PreparedStatement select = conn.prepareStatement(selectSql);
             ResultSet rs = select.executeQuery();
             PreparedStatement update = conn.prepareStatement(updateSql)) {
            while (rs.next()) {
                String firstName = safeText(rs.getString("first_name"));
                String lastName = safeText(rs.getString("last_name"));
                if (!firstName.isBlank() && !lastName.isBlank()) {
                    continue;
                }

                NameParts parts = splitFullName(rs.getString("full_name"));
                update.setString(1, parts.firstName);
                update.setString(2, parts.lastName);
                update.setInt(3, rs.getInt("donor_id"));
                update.executeUpdate();
            }
        }
    }

    private static NameParts splitFullName(String fullName) {
        String normalized = safeText(fullName).replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return new NameParts("", "");
        }
        List<String> parts = Arrays.asList(normalized.split(" "));
        if (parts.size() == 1) {
            return new NameParts(parts.get(0), "");
        }
        String first = parts.get(0);
        String last = parts.get(parts.size() - 1);
        return new NameParts(first, last);
    }

    private static void ensureColumn(Connection conn, String tableName, String columnName, String ddl) throws SQLException {
        if (!columnExists(conn, tableName, columnName)) {
            try (Statement st = conn.createStatement()) {
                st.execute(ddl);
            }
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(conn.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private static boolean userExists(Connection conn, String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean indexExists(Connection conn, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getIndexInfo(conn.getCatalog(), null, tableName, false, false)) {
            while (rs.next()) {
                String current = rs.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(current)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void dropIndexIfExists(Connection conn, String tableName, String indexName) throws SQLException {
        if (indexExists(conn, tableName, indexName)) {
            try (Statement st = conn.createStatement()) {
                st.execute("DROP INDEX " + indexName + " ON " + tableName);
            }
        }
    }

    private static void dropColumnIfExists(Connection conn, String tableName, String columnName) throws SQLException {
        if (columnExists(conn, tableName, columnName)) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE " + tableName + " DROP COLUMN " + columnName);
            }
        }
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static boolean isIgnorableSchemaError(String statement, SQLException ex) {
        int code = ex.getErrorCode();
        if (code == 1061 && startsWithIgnoreCase(statement, "CREATE INDEX")) {
            return true;
        }
        return false;
    }

    private static class NameParts {
        private final String firstName;
        private final String lastName;

        private NameParts(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    private static List<String> splitStatements(String sqlText) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        String normalized = sqlText.replace("\r\n", "\n");

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            char next = i + 1 < normalized.length() ? normalized.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    current.append(c);
                }
                continue;
            }

            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (!inSingleQuote && c == '-' && next == '-') {
                inLineComment = true;
                i++;
                continue;
            }

            if (!inSingleQuote && c == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }

            if (c == '\'') {
                current.append(c);
                if (next == '\'') {
                    current.append(next);
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (c == ';' && !inSingleQuote) {
                out.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (!current.toString().trim().isEmpty()) {
            out.add(current.toString());
        }
        return out;
    }
}
