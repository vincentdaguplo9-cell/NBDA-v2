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
    private static String DB_NAME;

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
        String baseUrl = props.getProperty("db.url");
        USER = props.getProperty("db.user");
        PASS = props.getProperty("db.pass");
        if (baseUrl == null || USER == null || PASS == null) {
            throw new IOException("Config file must contain db.url, db.user, db.pass");
        }
        DB_NAME = configuredDatabaseName(baseUrl);
        
        // Use the base URL and ensure it has necessary parameters for connectivity
        if (baseUrl.contains("?")) {
            URL = baseUrl + "&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&connectTimeout=10000";
        } else {
            URL = baseUrl + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&connectTimeout=10000";
        }
    }

    // Get a new database connection.
    public static Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", PASS);
        props.setProperty("allowPublicKeyRetrieval", "true");
        
        return DriverManager.getConnection(URL, props);
    }

    public static Connection getServerConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", PASS);
        props.setProperty("allowPublicKeyRetrieval", "true");
        
        // Extract server URL from the configured URL
        int slashIndex = URL.indexOf("/", URL.indexOf("://") + 3);
        String serverUrl = slashIndex > 0 ? URL.substring(0, slashIndex) : URL;
        
        return DriverManager.getConnection(serverUrl, props);
    }

    public static String getConfiguredDatabaseName() {
        return DB_NAME != null && !DB_NAME.isBlank() ? DB_NAME : "blood_archive";
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
}
