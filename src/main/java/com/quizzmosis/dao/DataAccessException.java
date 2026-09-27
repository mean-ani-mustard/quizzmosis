package com.quizzmosis.dao;

import java.sql.SQLException;

/**
 * Unchecked wrapper for {@link SQLException}.
 *
 * <p>The first version of the DAOs caught every SQLException, printed it and returned
 * {@code null}/{@code false}/an empty list. A broken query then looked exactly like "no data"
 * and the bug was easy to miss. Now a real database error travels up to the servlet container,
 * which logs it and shows the error page.</p>
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, SQLException cause) {
        super(message, cause);
    }

    /** True if the database rejected the statement because of a UNIQUE/PRIMARY KEY/FOREIGN KEY rule. */
    public boolean isConstraintViolation() {
        // SQLState class 23 = "integrity constraint violation" in every SQL database.
        String sqlState = ((SQLException) getCause()).getSQLState();
        return sqlState != null && sqlState.startsWith("23");
    }
}
