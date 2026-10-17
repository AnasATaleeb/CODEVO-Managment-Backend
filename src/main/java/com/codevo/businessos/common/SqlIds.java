package com.codevo.businessos.common;
import java.sql.*;
import java.util.UUID;
public final class SqlIds {
    private SqlIds() {}
    public static UUID uuid(ResultSet rs,String column) throws SQLException { return parse(rs.getString(column)); }
    public static UUID uuid(ResultSet rs,int column) throws SQLException { return parse(rs.getString(column)); }
    public static String text(UUID id) { return id==null?null:id.toString(); }
    private static UUID parse(String value) { return value==null?null:UUID.fromString(value); }
}
