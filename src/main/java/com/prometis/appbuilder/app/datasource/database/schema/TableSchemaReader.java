package com.prometis.appbuilder.app.datasource.database.schema;

import com.prometis.appbuilder.app.datasource.database.DatabaseKind;
import com.prometis.appbuilder.app.entity.code.ColumnType;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class TableSchemaReader {

    public static List<ColumnSchema> readColumns(DatabaseKind databaseKind, DataSource dataSource, String tableName) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String catalog = resolveCatalog(databaseKind, conn);
            String schema = resolveSchema(databaseKind, conn);
            String metaTableName = resolveTableName(databaseKind, tableName);

            Set<String> primaryKeyColumns = readPrimaryKeys(conn, catalog, schema, metaTableName);
            Map<String, String> comments = readComments(databaseKind, conn, schema, tableName);

            List<ColumnSchema> columns = new ArrayList<>();

            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(catalog, schema, metaTableName, null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    int jdbcType = rs.getInt("DATA_TYPE");
                    String typeName = rs.getString("TYPE_NAME");

                    ColumnSchema column = new ColumnSchema();
                    column.setColumnName(columnName);
                    column.setDataType(typeName);
                    column.setColumnType(mapColumnType(jdbcType));
                    column.setPrimaryKey(primaryKeyColumns.contains(columnName.toUpperCase()));
                    column.setComment(comments.get(columnName.toUpperCase()));

                    columns.add(column);
                }
            }

            return columns;
        }
    }

    private static String resolveCatalog(DatabaseKind databaseKind, Connection conn) throws SQLException {
        if (DatabaseKind.ORACLE.equals(databaseKind)) {
            return null;
        }

        return conn.getCatalog();
    }

    private static String resolveSchema(DatabaseKind databaseKind, Connection conn) throws SQLException {
        if (DatabaseKind.ORACLE.equals(databaseKind)) {
            String schema = conn.getSchema();
            return (schema != null) ? schema.toUpperCase() : conn.getMetaData().getUserName().toUpperCase();
        }

        if (DatabaseKind.MYSQL.equals(databaseKind) || DatabaseKind.MARIADB.equals(databaseKind)) {
            return null;
        }

        try {
            return conn.getSchema();
        }
        catch (SQLException e) {
            return null;
        }
    }

    private static String resolveTableName(DatabaseKind databaseKind, String tableName) {
        if (DatabaseKind.ORACLE.equals(databaseKind)) {
            return tableName.toUpperCase();
        }

        return tableName;
    }

    private static Set<String> readPrimaryKeys(Connection conn, String catalog, String schema, String tableName) throws SQLException {
        Set<String> primaryKeyColumns = new HashSet<>();

        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            while (rs.next()) {
                primaryKeyColumns.add(rs.getString("COLUMN_NAME").toUpperCase());
            }
        }

        return primaryKeyColumns;
    }

    private static Map<String, String> readComments(DatabaseKind databaseKind, Connection conn, String schema, String tableName) throws SQLException {
        Map<String, String> comments = new HashMap<>();

        String sql;
        switch (databaseKind) {
            case MYSQL:
            case MARIADB:
                sql = "SELECT COLUMN_NAME, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, tableName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            putIfPresent(comments, rs.getString("COLUMN_NAME"), rs.getString("COLUMN_COMMENT"));
                        }
                    }
                }
                break;

            case MSSQL:
                sql = "SELECT c.name AS COLUMN_NAME, CAST(ep.value AS NVARCHAR(4000)) AS COLUMN_COMMENT " +
                        "FROM sys.columns c " +
                        "LEFT JOIN sys.extended_properties ep " +
                        "  ON ep.major_id = c.object_id AND ep.minor_id = c.column_id AND ep.name = 'MS_Description' " +
                        "WHERE c.object_id = OBJECT_ID(?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, (schema != null) ? schema + "." + tableName : tableName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            putIfPresent(comments, rs.getString("COLUMN_NAME"), rs.getString("COLUMN_COMMENT"));
                        }
                    }
                }
                break;

            case ORACLE:
                sql = "SELECT COLUMN_NAME, COMMENTS FROM ALL_COL_COMMENTS WHERE TABLE_NAME = ? AND OWNER = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, tableName.toUpperCase());
                    stmt.setString(2, schema);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            putIfPresent(comments, rs.getString("COLUMN_NAME"), rs.getString("COMMENTS"));
                        }
                    }
                }
                break;

            default:
                break;
        }

        return comments;
    }

    private static void putIfPresent(Map<String, String> comments, String columnName, String comment) {
        if (comment != null && !comment.isEmpty()) {
            comments.put(columnName.toUpperCase(), comment);
        }
    }

    private static ColumnType mapColumnType(int jdbcType) {
        switch (jdbcType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
            case Types.NCLOB:
                return ColumnType.STRING;

            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.DECIMAL:
            case Types.NUMERIC:
            case Types.BIT:
            case Types.BOOLEAN:
                return ColumnType.NUMBER;

            case Types.DATE:
                return ColumnType.DATE;

            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return ColumnType.DATETIME;

            default:
                return ColumnType.STRING;
        }
    }
}
