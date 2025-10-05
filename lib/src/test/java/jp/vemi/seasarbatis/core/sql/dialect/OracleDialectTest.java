/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql.dialect;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link OracleDialect} のテストクラスです。
 * 
 * @author H.Kurosawa
 * @version 0.1.0
 */
class OracleDialectTest {

    private OracleDialect dialect;

    @BeforeEach
    void setUp() {
        dialect = new OracleDialect();
    }

    @Test
    void testGetDatabaseProductName() {
        assertEquals("Oracle", dialect.getDatabaseProductName());
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
        assertEquals("TO_DATE('2025-03-15 14:30:45', 'YYYY-MM-DD HH24:MI:SS')", formatted);
    }

    @Test
    void testFormatDateWithNull() {
        assertEquals("NULL", dialect.formatDate(null));
    }

    @Test
    void testFormatTimestamp() {
        String formatted = dialect.formatTimestamp("2025-03-15 14:30:45.123456");
        assertEquals("TO_TIMESTAMP('2025-03-15 14:30:45.123456', 'YYYY-MM-DD HH24:MI:SS.FF6')", formatted);
    }

    @Test
    void testFormatTimestampWithNull() {
        assertEquals("NULL", dialect.formatTimestamp(null));
    }

    @Test
    void testFormatArray_Numbers() {
        String formatted = dialect.formatArray("1,2,3");
        // OracleではIN句用のカンマ区切り
        assertEquals("1,2,3", formatted);
    }

    @Test
    void testFormatArray_Strings() {
        String formatted = dialect.formatArray("'a','b','c'");
        assertEquals("'a','b','c'", formatted);
    }

    @Test
    void testFormatArray_Empty() {
        String formatted = dialect.formatArray("");
        assertEquals("", formatted);
    }

    @Test
    void testFormatArray_Null() {
        String formatted = dialect.formatArray(null);
        assertEquals("", formatted);
    }
}
