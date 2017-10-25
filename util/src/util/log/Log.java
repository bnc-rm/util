package util.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

/*
 * Classe final che, una volta inizializzata per esempio in un main(), ha già
 * tutti i metodi per essere usata da classi chiamate dal main(), senza dover
 * inzializzare nuovi Logger ai livelli inferiori.
 * 
 * In qualsiasi classe basterà invocare, ad esempio, "Log.info(msg)" per mandare
 * sul file di log, a livello INFO, la string msg.
 */
public final class Log
{
	public static Logger log;
	private static Properties config;

	public static void init(String propFile)
	{
		config = new Properties();
		try
		{
			config.load(new FileReader(propFile));
		}
		catch(FileNotFoundException e)
		{
			System.err.println("File log.prop non trovato\n" + e.getMessage());
		}
		catch(IOException e)
		{
			System.err.println("File log.prop non trovato\n" + e.getMessage());
		}
		PatternLayout pl;
		File lf;
		PrintWriter pw;
		WriterAppender wa;
		log = Logger.getLogger(config.getProperty("log.file"));
		Level level = null;
		switch(config.getProperty("log.level"))
		{
			case "trace":
				level = Level.TRACE;
				break;
			case "debug":
				level = Level.DEBUG;
				break;
			case "info":
				level = Level.INFO;
				break;
			case "warn":
				level = Level.WARN;
				break;
			case "error":
				level = Level.ERROR;
				break;
			case "fatal":
				level = Level.FATAL;
				break;
			default:
				level = Level.OFF;
				break;
		}
		log.setLevel(level);
		pl = new PatternLayout(config.getProperty("log.pattern"));
		lf = new File(config.getProperty("log.file"));
		try
		{
			pw = new PrintWriter(lf);
			wa = new WriterAppender(pl, pw);
			log.addAppender(wa);
			wa = new WriterAppender(pl, System.out);
			log.addAppender(wa);
		}
		catch(FileNotFoundException e)
		{
			System.err.println("File " + propFile + " non trovato\n" + e.getMessage());
		}
	}

	public static void trace(String msg)
	{
		log.trace(msg);
	}

	public static void debug(String msg)
	{
		log.debug(msg);
	}

	public static void info(String msg)
	{
		log.info(msg);
	}

	public static void warn(String msg)
	{
		log.warn(msg);
	}

	public static void error(String msg)
	{
		log.error(msg);
	}

	public static void fatal(String msg)
	{
		log.fatal(msg);
	}
}
