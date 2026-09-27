package com.quizzmosis.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * A tiny helper around plain JDBC that removes the try/catch/close boilerplate every DAO method
 * would otherwise repeat.
 *
 * <p>Every statement is a {@link PreparedStatement} and every value is passed as a {@code ?}
 * parameter, never glued into the SQL string, so user input can't cause SQL injection.</p>
 *
 * <p>Methods without a {@link Connection} parameter borrow a connection, run one statement and
 * give the connection back. The variants that take a {@code Connection} are for use inside
 * {@link #inTransaction}, when several statements must succeed or fail together.</p>
 */
final class Jdbc {

    /** Turns the current row of a ResultSet into an object. */
    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Work that runs inside a transaction on the given connection. */
    @FunctionalInterface
    interface TransactionWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private Jdbc() {
    }

    /** Runs a SELECT and maps every row. */
    static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection connection = Database.getConnection()) {
            return query(connection, sql, mapper, params);
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + sql, e);
        }
    }

    static <T> List<T> query(Connection connection, String sql, RowMapper<T> mapper, Object... params)
            throws SQLException {
        try (PreparedStatement ps = prepare(connection, sql, Statement.NO_GENERATED_KEYS, params);
             ResultSet rs = ps.executeQuery()) {
            List<T> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapper.map(rs));
            }
            return rows;
        }
    }

    /** Runs a SELECT that returns at most one row; {@code null} if there is none. */
    static <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Runs a SELECT that returns a single number, such as {@code COUNT(*)}. NULL counts as 0. */
    static int queryInt(String sql, Object... params) {
        Integer value = queryOne(sql, rs -> rs.getInt(1), params);
        return value == null ? 0 : value;
    }

    static int queryInt(Connection connection, String sql, Object... params) throws SQLException {
        List<Integer> rows = query(connection, sql, rs -> rs.getInt(1), params);
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    /** Runs an UPDATE, DELETE or INSERT and returns the number of affected rows. */
    static int update(String sql, Object... params) {
        try (Connection connection = Database.getConnection()) {
            return update(connection, sql, params);
        } catch (SQLException e) {
            throw new DataAccessException("Update failed: " + sql, e);
        }
    }

    static int update(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(connection, sql, Statement.NO_GENERATED_KEYS, params)) {
            return ps.executeUpdate();
        }
    }

    /** Runs an INSERT into a table with an AUTO_INCREMENT key and returns the new id. */
    static int insert(String sql, Object... params) {
        try (Connection connection = Database.getConnection()) {
            return insert(connection, sql, params);
        } catch (SQLException e) {
            throw new DataAccessException("Insert failed: " + sql, e);
        }
    }

    static int insert(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(connection, sql, Statement.RETURN_GENERATED_KEYS, params)) {
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("The database returned no generated key for: " + sql);
            }
        }
    }

    /**
     * Runs {@code work} in a single transaction: everything is committed if it finishes normally
     * and rolled back if it throws, so the database never ends up half-updated.
     */
    static <T> T inTransaction(TransactionWork<T> work) {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Transaction failed", e);
        }
    }

    /** A column that may be NULL. getInt() returns 0 for NULL, so wasNull() tells the two apart. */
    static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * Escapes the LIKE wildcards {@code %} and {@code _} so a search for "50%" looks for the text
     * "50%" instead of "50 followed by anything".
     */
    static String likePattern(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static PreparedStatement prepare(Connection connection, String sql, int generatedKeys,
                                             Object... params) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql, generatedKeys);
        try {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        } catch (SQLException e) {
            ps.close();
            throw e;
        }
    }
}
