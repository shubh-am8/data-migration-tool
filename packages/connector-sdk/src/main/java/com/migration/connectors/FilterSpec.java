package com.migration.connectors;

import java.util.List;

public record FilterSpec(
    String column,
    FilterOperator operator,
    List<String> values
) {
    public static boolean isValidForType(FilterOperator op, ColumnDataType type) {
        return switch (type) {
            case NUMERIC -> List.of(FilterOperator.EQ, FilterOperator.NE, FilterOperator.LT,
                FilterOperator.LTE, FilterOperator.GT, FilterOperator.GTE,
                FilterOperator.BETWEEN, FilterOperator.IN, FilterOperator.IS_NULL).contains(op);
            case TEXT -> List.of(FilterOperator.EQ, FilterOperator.NE, FilterOperator.LIKE,
                FilterOperator.ILIKE, FilterOperator.IN, FilterOperator.IS_NULL).contains(op);
            case TIMESTAMP -> List.of(FilterOperator.EQ, FilterOperator.LT, FilterOperator.LTE,
                FilterOperator.GT, FilterOperator.GTE, FilterOperator.BETWEEN,
                FilterOperator.IS_NULL).contains(op);
            case BOOLEAN -> List.of(FilterOperator.EQ, FilterOperator.IS_NULL).contains(op);
            case OTHER -> List.of(FilterOperator.EQ, FilterOperator.IS_NULL).contains(op);
        };
    }
}
