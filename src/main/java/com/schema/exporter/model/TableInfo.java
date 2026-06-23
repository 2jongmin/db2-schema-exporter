package com.schema.exporter.model;

import java.util.List;

/**
 * DB2 테이블 정보 모델
 */
public class TableInfo {

    private String           schemaName;
    private String           tableName;
    private String           tableComment;
    private List<ColumnInfo> columns;
    private String           ddlScript;
    private String           tablespace;

    public TableInfo() {}

    // ── Getters ──
    public String           getSchemaName()   { return schemaName; }
    public String           getTableName()    { return tableName; }
    public String           getTableComment() { return tableComment; }
    public List<ColumnInfo> getColumns()      { return columns; }
    public String           getDdlScript()    { return ddlScript; }
    public String           getTablespace()   { return tablespace; }

    // ── Setters ──
    public void setSchemaName(String schemaName)           { this.schemaName = schemaName; }
    public void setTableName(String tableName)             { this.tableName = tableName; }
    public void setTableComment(String tableComment)       { this.tableComment = tableComment; }
    public void setColumns(List<ColumnInfo> columns)       { this.columns = columns; }
    public void setDdlScript(String ddlScript)             { this.ddlScript = ddlScript; }
    public void setTablespace(String tablespace)           { this.tablespace = tablespace; }

    // ── Builder ──
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final TableInfo obj = new TableInfo();
        public Builder schemaName(String v)           { obj.schemaName = v;   return this; }
        public Builder tableName(String v)            { obj.tableName = v;    return this; }
        public Builder tableComment(String v)         { obj.tableComment = v; return this; }
        public Builder columns(List<ColumnInfo> v)    { obj.columns = v;      return this; }
        public Builder ddlScript(String v)            { obj.ddlScript = v;    return this; }
        public Builder tablespace(String v)           { obj.tablespace = v;   return this; }
        public TableInfo build()                      { return obj; }
    }

    @Override
    public String toString() {
        return "TableInfo{" +
                "tableName='" + tableName + '\'' +
                ", tableComment='" + tableComment + '\'' +
                ", columns=" + (columns != null ? columns.size() : 0) +
                '}';
    }
}
