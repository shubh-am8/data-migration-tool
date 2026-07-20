package com.migration.connectors.postgresql;

import com.migration.connectors.FilterOperator;
import com.migration.connectors.FilterSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class PgSqlBuilder {

    private PgSqlBuilder() {}

    static String buildWhere(List<FilterSpec> filters, String hotColdFilter) {
        List<String> clauses = new ArrayList<>();
        if (hotColdFilter != null && !hotColdFilter.isBlank()) {
            clauses.add("(" + hotColdFilter + ")");
        }
        for (FilterSpec f : filters) {
            clauses.add(buildFilterClause(f));
        }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    static String buildFilterClause(FilterSpec f) {
        String col = PostgresqlConnectorPlugin.quoteIdent(f.column());
        return switch (f.operator()) {
            case EQ -> col + " = " + quoteValue(f.values().get(0));
            case NE -> col + " <> " + quoteValue(f.values().get(0));
            case LT -> col + " < " + quoteValue(f.values().get(0));
            case LTE -> col + " <= " + quoteValue(f.values().get(0));
            case GT -> col + " > " + quoteValue(f.values().get(0));
            case GTE -> col + " >= " + quoteValue(f.values().get(0));
            case BETWEEN -> col + " BETWEEN " + quoteValue(f.values().get(0)) + " AND " + quoteValue(f.values().get(1));
            case IN -> col + " IN (" + f.values().stream().map(PgSqlBuilder::quoteValue).collect(Collectors.joining(", ")) + ")";
            case LIKE -> col + " LIKE " + quoteValue(f.values().get(0));
            case ILIKE -> col + " ILIKE " + quoteValue(f.values().get(0));
            case IS_NULL -> col + " IS NULL";
        };
    }

    static String quoteValue(String v) {
        if (v == null) return "NULL";
        if (v.matches("-?\\d+(\\.\\d+)?")) return v;
        return "'" + v.replace("'", "''") + "'";
    }
}
