package com.migration.connectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FilterSpecTest {

    @Test
    void numericOperatorsAllowed() {
        assertTrue(FilterSpec.isValidForType(FilterOperator.GTE, ColumnDataType.NUMERIC));
        assertFalse(FilterSpec.isValidForType(FilterOperator.LIKE, ColumnDataType.NUMERIC));
    }

    @Test
    void textOperatorsAllowed() {
        assertTrue(FilterSpec.isValidForType(FilterOperator.ILIKE, ColumnDataType.TEXT));
        assertFalse(FilterSpec.isValidForType(FilterOperator.BETWEEN, ColumnDataType.TEXT));
    }

    @Test
    void timestampOperatorsAllowed() {
        assertTrue(FilterSpec.isValidForType(FilterOperator.BETWEEN, ColumnDataType.TIMESTAMP));
        assertFalse(FilterSpec.isValidForType(FilterOperator.IN, ColumnDataType.TIMESTAMP));
    }
}
