/**
 * 
 */
package com.dinnerbell.webapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Class for database access
 * 
 * @author EB03
 */
public class DAO {

	Logger logger = Logger.getGlobal();

	/**
	 * parameter for database access
	 * 
	 */
	String url = "jdbc:oracle:thin:@Nina-VAIO:1521:XE";
	String user = "SYSTEM";
	String passwd = "m6c8aYJk";

	/**
	 * Establishes database-connection
	 * 
	 * @return Connection
	 */
	public Connection connect() {

		Connection conn = null;

		try {
			Class.forName("oracle.jdbc.OracleDriver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		try {
			// prepare connection
			Properties connectionProps = new Properties();
			connectionProps.put("user", this.user);
			connectionProps.put("password", this.passwd);
			// establish connection
			conn = DriverManager.getConnection(url, connectionProps);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return conn;
	}

	/**
	 * Closes given connection
	 * 
	 * @param conn
	 */
	public void close(Connection conn) {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
