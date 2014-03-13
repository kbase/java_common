package us.kbase.common.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DbConn {
	private Connection conn;
	
	public DbConn(Connection conn) {
		this.conn = conn;
	}
	
	public Connection getConnection() {
		return conn;
	}
	
	public DbConn exec(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement st = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++)
            st.setObject(1 + i, params[i]);
        st.execute();
        st.close();
        return this;
    }
	
	public <T> List<T> collect(String sql, SqlLoader<T> sl, Object... params) throws SQLException {
        PreparedStatement st = getConnection().prepareStatement(sql);
        try {
        	for (int i = 0; i < params.length; i++)
        		st.setObject(1 + i, params[i]);
        	return collect(st.executeQuery(), sl);
        } finally {
        	st.close();
        }
	}
		
	public <T> List<T> collect(ResultSet rs, SqlLoader<T> sl) throws SQLException {
		try {
			List<T> ret = new ArrayList<T>();
			while (rs.next())
				ret.add(sl.collectRow(rs));
			return ret;
		} finally {
			rs.close();
		}
	}
	
	public boolean checkTable(String tableName) throws SQLException {
        Set<String> tables = new HashSet<String>(collect(
        		conn.getMetaData().getTables(null, null, "%", new String[] {"TABLE"}), 
        		new SqlLoader<String>() {
        			public String collectRow(ResultSet rs) throws SQLException {
        				return rs.getString("TABLE_NAME").toUpperCase();
        			}
        		}));
        return tables.contains(tableName.toUpperCase());
    }

	public static interface SqlLoader<T> {
        T collectRow(ResultSet rs) throws SQLException;
    }
}
