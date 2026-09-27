package com.quizzmosis.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Hands out database connections from a HikariCP connection pool.
 *
 * <p>Opening a brand-new MySQL connection for every query is slow, so the pool keeps a few
 * connections open and lends them out; {@code connection.close()} gives a connection back to
 * the pool instead of closing it.</p>
 *
 * <p>Connection settings are never hard-coded. Each one is read from a Java system property
 * first, then from an environment variable, then falls back to a local-development default:</p>
 * <pre>
 *   db.url      / DB_URL       (default jdbc:mysql://localhost:3306/quiz_db)
 *   db.user     / DB_USER      (default root)
 *   db.password / DB_PASSWORD  (default empty)
 * </pre>
 */
public final class Database {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/quiz_db";

    private static volatile HikariDataSource dataSource;

    private Database() {
    }

    /** Borrows a connection from the pool. Always use it in try-with-resources so it is returned. */
    public static Connection getConnection() throws SQLException {
        return dataSource().getConnection();
    }

    /** The JDBC URL in use (never includes the password), handy for log messages. */
    public static String url() {
        return setting("db.url", "DB_URL", DEFAULT_URL);
    }

    /** Closes the pool. Called once when the web application shuts down. */
    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    /** Creates the pool on first use (double-checked locking, so only one pool is ever created). */
    private static HikariDataSource dataSource() throws SQLException {
        HikariDataSource ds = dataSource;
        if (ds == null) {
            synchronized (Database.class) {
                ds = dataSource;
                if (ds == null) {
                    ds = createDataSource();
                    dataSource = ds;
                }
            }
        }
        return ds;
    }

    private static HikariDataSource createDataSource() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setPoolName("quizzmosis-db");
        // Setting the driver class explicitly matters inside Tomcat, which does not auto-register
        // JDBC drivers that live in the web application's WEB-INF/lib folder.
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(url());
        config.setUsername(setting("db.user", "DB_USER", "root"));
        config.setPassword(setting("db.password", "DB_PASSWORD", ""));
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(5_000);
        try {
            return new HikariDataSource(config);
        } catch (RuntimeException e) {
            // Hikari fails fast when the database is unreachable; turn that into a normal SQLException.
            throw new SQLException("Cannot connect to the database at " + url()
                    + " (check DB_URL, DB_USER and DB_PASSWORD)", e);
        }
    }

    private static String setting(String systemProperty, String environmentVariable, String defaultValue) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
