/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis.generator;

import java.util.List;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.Plugin;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;

public class SBEntityMetaPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        // テーブルメタデータを生成
        generateTableMeta(topLevelClass, introspectedTable);
        return true;
    }

    @Override
    public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass,
            IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable,
            Plugin.ModelClassType modelClassType) {
        // カラムメタデータを生成
        generateColumnMeta(field, introspectedColumn, introspectedTable);
        return true;
    }

    private void generateTableMeta(TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();
        String schema = introspectedTable.getTableConfiguration().getSchema();

        topLevelClass.addImportedType("jp.vemi.seasarbatis.meta.SBTableMeta");
        topLevelClass.addAnnotation(
                String.format("@SBTableMeta(name = \"%s\", schema = \"%s\")",
                        tableName, schema != null ? schema : ""));
    }

    private void generateColumnMeta(Field field, IntrospectedColumn column, IntrospectedTable introspectedTable) {
        boolean isPrimaryKey = column.isIdentity() ||
                column.isSequenceColumn() ||
                introspectedTable.getPrimaryKeyColumns().contains(column);

        field.addAnnotation(
                String.format("@SBColumnMeta(name = \"%s\", primaryKey = %b)",
                        column.getActualColumnName(),
                        isPrimaryKey));
    }
}