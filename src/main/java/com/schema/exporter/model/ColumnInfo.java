package com.schema.exporter.model;

/**
 * DB2 컬럼 정보 모델
 */
public class ColumnInfo {

    private int    ordinalPosition;
    private String columnName;
    private String columnComment;
    private String dataType;
    private String fullDataType;
    private int    columnLength;
    private int    decimalDigits;
    private boolean nullable;
    private boolean primaryKey;
    private int    pkPosition;
    private String defaultValue;
    private boolean indexed;
    private String indexName;

    public ColumnInfo() {}

    // ── Getters ──
    public int     getOrdinalPosition() { return ordinalPosition; }
    public String  getColumnName()      { return columnName; }
    public String  getColumnComment()   { return columnComment; }
    public String  getDataType()        { return dataType; }
    public String  getFullDataType()    { return fullDataType; }
    public int     getColumnLength()    { return columnLength; }
    public int     getDecimalDigits()   { return decimalDigits; }
    public boolean isNullable()         { return nullable; }
    public boolean isPrimaryKey()       { return primaryKey; }
    public int     getPkPosition()      { return pkPosition; }
    public String  getDefaultValue()    { return defaultValue; }
    public boolean isIndexed()          { return indexed; }
    public String  getIndexName()       { return indexName; }

    // ── Setters ──
    public void setOrdinalPosition(int ordinalPosition)    { this.ordinalPosition = ordinalPosition; }
    public void setColumnName(String columnName)           { this.columnName = columnName; }
    public void setColumnComment(String columnComment)     { this.columnComment = columnComment; }
    public void setDataType(String dataType)               { this.dataType = dataType; }
    public void setFullDataType(String fullDataType)       { this.fullDataType = fullDataType; }
    public void setColumnLength(int columnLength)          { this.columnLength = columnLength; }
    public void setDecimalDigits(int decimalDigits)        { this.decimalDigits = decimalDigits; }
    public void setNullable(boolean nullable)              { this.nullable = nullable; }
    public void setPrimaryKey(boolean primaryKey)          { this.primaryKey = primaryKey; }
    public void setPkPosition(int pkPosition)              { this.pkPosition = pkPosition; }
    public void setDefaultValue(String defaultValue)       { this.defaultValue = defaultValue; }
    public void setIndexed(boolean indexed)                { this.indexed = indexed; }
    public void setIndexName(String indexName)             { this.indexName = indexName; }

    // ── Builder ──
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ColumnInfo obj = new ColumnInfo();
        public Builder ordinalPosition(int v)    { obj.ordinalPosition = v; return this; }
        public Builder columnName(String v)      { obj.columnName = v;      return this; }
        public Builder columnComment(String v)   { obj.columnComment = v;   return this; }
        public Builder dataType(String v)        { obj.dataType = v;        return this; }
        public Builder fullDataType(String v)    { obj.fullDataType = v;    return this; }
        public Builder columnLength(int v)       { obj.columnLength = v;    return this; }
        public Builder decimalDigits(int v)      { obj.decimalDigits = v;   return this; }
        public Builder nullable(boolean v)       { obj.nullable = v;        return this; }
        public Builder primaryKey(boolean v)     { obj.primaryKey = v;      return this; }
        public Builder pkPosition(int v)         { obj.pkPosition = v;      return this; }
        public Builder defaultValue(String v)    { obj.defaultValue = v;    return this; }
        public Builder indexed(boolean v)        { obj.indexed = v;         return this; }
        public Builder indexName(String v)       { obj.indexName = v;       return this; }
        public ColumnInfo build()                { return obj; }
    }

    @Override
    public String toString() {
        return "ColumnInfo{" +
                "columnName='" + columnName + '\'' +
                ", fullDataType='" + fullDataType + '\'' +
                ", nullable=" + nullable +
                ", primaryKey=" + primaryKey +
                '}';
    }
}
