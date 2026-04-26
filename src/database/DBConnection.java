package database;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Centralized JDBC connection utility (config-based).
public class DBConnection {
    private static final String CONFIG_PATH = "config/db.properties";
    private static String URL;
    private static String USER;
    private static String PASS;

    static {
        try {
            loadDriver();
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Database config not found. Expected: " + CONFIG_PATH, e);
        }
    }

    private static void loadDriver() {
        String[] driverCandidates = {
                "org.mariadb.jdbc.Driver",
                "com.mysql.cj.jdbc.Driver"
        };
        for (String driver : driverCandidates) {
            try {
                Class.forName(driver);
                return;
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new RuntimeException("No MariaDB/MySQL JDBC driver found in lib/.");
    }

    // Load DB settings from config/db.properties so it can be edited anytime.
    private static void loadConfig() throws IOException {
        Path path = Paths.get(CONFIG_PATH);
        if (!Files.exists(path)) {
            throw new IOException("Missing config file: " + CONFIG_PATH);
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            props.load(in);
        }
        URL = props.getProperty("db.url");
        USER = props.getProperty("db.user");
        PASS = props.getProperty("db.pass");
        if (URL == null || USER == null || PASS == null) {
            throw new IOException("Config file must contain db.url, db.user, db.pass");
        }
    }

    // Get a new database connection.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(serverUrl(URL), USER, PASS);
    }

    public static String getConfiguredDatabaseName() {
        String dbName = configuredDatabaseName(URL);
        return dbName == null || dbName.isBlank() ? "blood_archive" : dbName;
    }

    private static String configuredDatabaseName(String jdbcUrl) {
        int schemeIndex = jdbcUrl.indexOf("://");
        int slashIndex = jdbcUrl.indexOf('/', schemeIndex >= 0 ? schemeIndex + 3 : 0);
        if (slashIndex < 0) {
            return null;
        }
        int queryIndex = jdbcUrl.indexOf('?', slashIndex);
        String dbName = queryIndex >= 0
                ? jdbcUrl.substring(slashIndex + 1, queryIndex)
                : jdbcUrl.substring(slashIndex + 1);
        return dbName.isBlank() ? null : dbName;
    }

    private static String serverUrl(String jdbcUrl) {
        int schemeIndex = jdbcUrl.indexOf("://");
        int slashIndex = jdbcUrl.indexOf('/', schemeIndex >= 0 ? schemeIndex + 3 : 0);
        if (slashIndex < 0) {
            return jdbcUrl;
        }
        int queryIndex = jdbcUrl.indexOf('?', slashIndex);
        String prefix = jdbcUrl.substring(0, slashIndex + 1);
        String suffix = queryIndex >= 0 ? jdbcUrl.substring(queryIndex) : "";
        return prefix + suffix;
    }
}
