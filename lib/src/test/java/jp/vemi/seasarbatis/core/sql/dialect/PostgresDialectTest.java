/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql.dialect;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link PostgresDialect} のテストクラスです。
 * 
 * @author H.Kurosawa
 * @version 0.0.1
 */
class PostgresDialectTest {

    private PostgresDialect dialect;

    @BeforeEach
    void setUp() {
        dialect = new PostgresDialect();
    }

    @Test
    void testGetDatabaseProductName() {
        assertEquals("PostgreSQL", dialect.getDatabaseProductName());
    }

    @Test
    void testFormatString() {
        assertEquals("'hello'", dialect.formatString("hello"));
        assertEquals("'hello''world'", dialect.formatString("hello'world"));
        assertEquals("''", dialect.formatString(""));
    }

    @Test
    void testFormatStringWithNull() {
        assertEquals("NULL", dialect.formatString(null));
    }

    @Test
    void testFormatDate() {
        String formatted = dialect.formatDate("2025-03-15 14:30:45");
        assertEquals("TIMESTAMP '2025-03-15 14:30:45'", formatted);
    }

    @Test
    void testFormatDateWithNull() {
        assertEquals("NULL", dialect.formatDate(null));
    }

    @Test
    void testFormatTimestamp() {
        String formatted = dialect.formatTimestamp("2025-03-15 14:30:45.123456");
        assertEquals("TIMESTAMP '2025-03-15 14:30:45.123456'", formatted);
    }

    @Test
    void testFormatTimestampWithNull() {
        assertEquals("NULL", dialect.formatTimestamp(null));
    }

    @Test
    void testFormatArray_Numbers() {
        String formatted = dialect.formatArray("1,2,3");
        assertEquals("ARRAY[1,2,3]", formatted);
    }

    @Test
    void testFormatArray_Strings() {
        String formatted = dialect.formatArray("'a','b','c'");
        assertEquals("ARRAY['a','b','c']", formatted);
    }

    @Test
    void testFormatArray_Empty() {
        String formatted = dialect.formatArray("");
        assertEquals("ARRAY[]", formatted);
    }

    @Test
    void testFormatArray_Null() {
        String formatted = dialect.formatArray(null);
        assertEquals("ARRAY[]", formatted);
    }
}
