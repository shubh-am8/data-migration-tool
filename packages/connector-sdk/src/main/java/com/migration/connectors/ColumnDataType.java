package com.migration.connectors;

public enum ColumnDataType {
    NUMERIC, TEXT, TIMESTAMP, BOOLEAN, OTHER;

    public static ColumnDataType fromSql(String sqlType) {
        if (sqlType == null) return OTHER;
        String t = sqlType.toLowerCase();
        if (t.contains("int") || t.contains("numeric") || t.contains("decimal") || t.contains("float") || t.contains("double") || t.contains("real") || t.contains("serial")) {
            return NUMERIC;
        }
        if (t.contains("char") || t.contains("text") || t.contains("uuid")) {
            return TEXT;
        }
        if (t.contains("timestamp") || t.contains("date") || t.contains("time")) {
            return TIMESTAMP;
        }
        if (t.contains("bool")) {
            return BOOLEAN;
        }
        return OTHER;
    }
}
