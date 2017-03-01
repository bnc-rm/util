package util.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import util.log.Log;

/** Description of the Class */

public class DB
{
	public Connection conn;
	public Connection connTest;
	public static String mysqlDriver = "com.mysql.jdbc.Driver";
	public static String urlTest = "jdbc:mysql://gauss/abi";
	public static String urlGauss = "jdbc:mysql://gauss/abi";
	public static String urlEsercizio = "jdbc:postgresql://anagrafe.iccu.sbn.it:5432/abi2";
	public static String urlEsercizioGiuliano = "jdbc:postgresql://192.168.20.131:5432/abi2";

	public PreparedStatement prepare(String sql)
	{
		PreparedStatement temp = null;
		try
		{
			temp = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE);
		}
		catch(SQLException e)
		{
			Log.error(e.getMessage());
		}
		return temp;
	}

	public ResultSet select(String query)
	{
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery(query);
		}
		catch(SQLException e)
		{
			Log.error(e.getMessage());
		}
		return rs;
	}

	/*
	 * Costruttore parametrico. Le credenziali vanno ovviamente cambiate a seconda
	 * delle reali implementazioni. Meglio sarebbe passare anche quelle al
	 * costruttore, prelevandole magari da un file di Properties.
	 */
	public DB(String url, String username, String password)
	{
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
		}
		catch(Exception e)
		{
			Log.error("ERROR: failed to load JDBC driver.");
		}
		try
		{
			conn = DriverManager.getConnection(url, username, password);
			if(conn == null)
			{
				Log.error("Connessione nulla!");
			}
		}
		catch(SQLException e)
		{
			Log.error("ERROR: failed to connect!");
		}
	}

	/*
	 * Costruttore parametrico. Le credenziali vanno ovviamente cambiate a seconda
	 * delle reali implementazioni. Meglio sarebbe passare anche quelle al
	 * costruttore, prelevandole magari da un file di Properties.
	 */
	public DB(String driver, String url, String username, String password)
	{
		try
		{
			Class.forName(driver);
		}
		catch(Exception e)
		{
			Log.error("ERROR: failed to load JDBC driver (" + driver + ")");
		}
		try
		{
			conn = DriverManager.getConnection(url, username, password);
			if(conn == null)
			{
				Log.error("Connessione nulla!");
			}
		}
		catch(SQLException e)
		{
			Log.error("ERROR: failed to connect!");
		}
	}

	public void free()
	{
		try
		{
			conn.close();
			if(connTest != null) connTest.close();
		}
		catch(SQLException e)
		{
			Log.error("ERROR: Fetch statement failed: " + e.getMessage());
			Log.error(e.toString());
		}
	}
}
