package util.sql;

import java.io.File;
import java.io.IOException;

import util.log.Log;


/*
 * Questa classe dovrebbe leggere un JSON che rappresenta un database e
 * ricrearlo su un database MySQL vivo.
 */
public class TestDBToJson
{

	public static void main(String[] args)
	{
		Log.init(args[0]);
		DBToJson dj;
		try
		{
			System.err.println(new File(".").getCanonicalPath());
			System.err.println(Log.log.getLevel());
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		Log.info("File prop: " + args[0]);
		Log.info("File json: " + args[1]);
		dj = new DBToJson(args[0]);
		dj.output(args[1]);
	}
}
