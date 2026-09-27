package com.quizzmosis.web;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import com.quizzmosis.dao.Database;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Runs once when the web application starts and once when it stops. */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        String url = Database.url();
        // Log the URL without its query string, in case someone put credentials in there.
        String safeUrl = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        try (Connection connection = Database.getConnection()) {
            String version = connection.getMetaData().getDatabaseProductVersion();
            LOG.info("Quizzmosis started, connected to MySQL " + version + " at " + safeUrl);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Quizzmosis started but cannot reach the database at " + safeUrl
                    + ". Pages will show an error until the database is available.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Database.shutdown();

        // Unregister the MySQL driver and stop its background thread. Without this Tomcat warns
        // about a memory leak every time the application is stopped or redeployed.
        ClassLoader appClassLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == appClassLoader) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "Could not unregister JDBC driver " + driver, e);
                }
            }
        }
        AbandonedConnectionCleanupThread.checkedShutdown();
    }
}
