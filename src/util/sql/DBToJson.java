package util.sql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import util.log.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;

/*
 * Classe per la serializzazione di un DB in JSON.
 */
public class DBToJson
{
	private Properties config;
	private DB source;
	private String sourceSchema;
	private Gson gson;
	private int maxRows = Integer.MAX_VALUE;

/*
 * Costruttore che legge il prop file. Le proprietà più importanti sono
 * ovviamente quelle che permettono di accedere correttamente al DB
 */

	public DBToJson(String cFile)
	{
		config = new Properties();
		try
		{
			config.load(new FileReader(cFile));
			Log.info("Connessione al DB...");
			String driver, url, user, pass;
			driver = config.getProperty("source.driver");
			url = config.getProperty("source.url");
			user = config.getProperty("source.username");
			pass = config.getProperty("source.password");
			source = new DB(driver, url, user, pass);
			sourceSchema = config.getProperty("source.schema");
			gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
			if(config.getProperty("source.limit") != null)
			{
				maxRows = Integer.valueOf(config.getProperty("source.limit"));
			}
		}
		catch(FileNotFoundException e)
		{
			System.err.println(e.getMessage());
			Log.error("File non trovato: " + e.getMessage());
		}
		catch(IOException e)
		{
			Log.error("Impossibile leggere il file di configurazione: " + e.getMessage());
		}
		catch(Exception e)
		{
			Log.error("Errore generico: " + e.getMessage());
		}
	}

/*
 * Estrae dal DB tutte le tabelle. L'eventuale schema deve essere specificato
 * nel prop file. L'output è un ResultSet.
 */
	public ResultSet getTables()
	{
		DatabaseMetaData metadata = null;
		ResultSet rs = null;
		try
		{
			Connection conn = source.conn;
			metadata = conn.getMetaData();
			rs = metadata.getTables(null, sourceSchema, "%", null);
		}
		catch(SQLException e)
		{
			Log.error("Errore SQL generico: " + e.getMessage());
		}
		return rs;
	}

/*
 * Estrae dal DB le colonne della tabella specificata, anche in questo caso
 * sotto forma di un ResultSet.
 */
	public ResultSet getColumns(String table)
	{
		DatabaseMetaData metadata = null;
		ResultSet rs = null;
		try
		{
			Connection conn = source.conn;
			metadata = conn.getMetaData();
			rs = metadata.getColumns(null, sourceSchema, table, "%");
		}
		catch(SQLException e)
		{
			Log.error("Errore SQL generico: " + e.getMessage());
		}
		return rs;
	}

/*
 * Estrae dal DB tutti i record di una tabella. Di questi saranno salvati in
 * JSON i soli valori: i tipi sarnno salvati solo una volta, per cercare di
 * contenere le dimensioni dei file risultati.
 */
	public ResultSet getRecords(String table)
	{
		ResultSet rs = null;
		rs = source.select("select * from " + table);
		return rs;
	}

/*
 * Output del DB in formato JSON. Si comincia con l'elenco di tutte le tabelle,
 * anche se molte saranno scartate perché di sistema. Viene anche creato qualche
 * primo oggetto legato a JSON. Ogni singola tabella sarà salvata nell'oggetto
 * jTable.
 */
	public void output(String outputDir)
	{
		ResultSet rsTables = getTables();
		try
		{
/*
 * Si scorre l'elenco delle tabelle, scartando quelle che iniziano con "sys",
 * perché lo scopo della classe è soprattutto il salvataggio dei dati, non tanto
 * la replica esatta di un DB con relazioni, vincoli etc...
 */
			while(rsTables.next())
			{
				String table = rsTables.getString("TABLE_NAME");
				if(table.startsWith("sys")) continue;

/*
 * Poiché alcune tabelle possono essere troppo grandi da gestire in memoria,
 * conviene scaricare i record un po' alla volta sull'output, invece di
 * aspettare che un'intera tabella sia caricata in un array Gson. Per questo si
 * usa un oggetto JsonWriter
 */
				PrintWriter pw = new PrintWriter(new File(outputDir + "/" + table + ".json"));
				JsonWriter jw = new JsonWriter(pw);
				jw.setIndent("  ");

/*
 * Si apre un oggetto a cui si assegna subito la proprietà "name", il cui valore
 * è il nome della tabella
 */

				jw.beginObject();
				jw.name("name").value(table);
/*
 * I nomi delle colonne si ricavano dal result set dei record, invece che dai
 * metadati del database, quindi in questa fase.
 */
				ResultSet rsRecords = getRecords(table);
				ResultSetMetaData rsmd = rsRecords.getMetaData();
				int rowNum = 0;
/*
 * Si può già salvare una proprietà "numero di colonne" della tabella attuale
 */
				JsonObject jColumns = new JsonObject();
				int colNum = rsmd.getColumnCount();
				jw.name("numberOfColumns").value(colNum);
				Log.info("Tabella: " + table + " (" + colNum + " colonne)");
/*
 * Si crea l'elenco delle colonne come oggetto jColumns. I valori delle
 * proprietà comprendono anche la lunghezza-precisione dei singoli campi
 */
				for(int i = 1; i <= colNum; i++)
				{
					String column = rsmd.getColumnName(i);
					String cType = rsmd.getColumnTypeName(i);
					int cSize = rsmd.getPrecision(i);
					jColumns.add(column, new JsonPrimitive(cType + "(" + cSize + ")"));
					Log.debug("Colonna: " + table + "." + column + " " + cType + "(" + cSize + ")");
				}
/*
 * L'oggetto jColumns viene scaricato sul writer, senza bisogno di begin o end,
 * grazie a una versione del metodo toJson che permette di indicare un writer.
 */
				jw.flush();
				jw.name("columns");
				gson.toJson(jColumns, jw);
/*
 * Si itera sui record, ognuno dei quali è un array di stringhe (jRecord), e
 * insieme saranno un array di oggetti, aperto però come stream per evitare che
 * una tabella troppo grande esaurisca la memoria
 */
				jw.name("records").beginArray();
				while(rsRecords.next())
				{
					if(++rowNum > maxRows)
					{
						break;
					}
					JsonArray jRecord = new JsonArray();
					for(int i = 1; i <= colNum; i++)
					{
						JsonPrimitive jp = null;
						try
						{
							if(rsRecords.getString(i) == null)
							{
								jRecord.add(null);
							}
							else
							{
								switch(rsmd.getColumnType(i))
								{
									case Types.INTEGER:
										jp = new JsonPrimitive(rsRecords.getLong(i));
										break;
									case Types.SMALLINT:
										jp = new JsonPrimitive(rsRecords.getLong(i));
										break;
									case Types.DATE:
										jp = new JsonPrimitive((rsRecords.getString(i)));
										break;
									case Types.TIME:
										jp = new JsonPrimitive((rsRecords.getString(i)));
										break;
									case Types.TIMESTAMP:
										jp = new JsonPrimitive((rsRecords.getString(i)));
										break;
									case Types.VARCHAR:
										jp = new JsonPrimitive(rsRecords.getString(i));
										break;
									case Types.BOOLEAN:
										jp = new JsonPrimitive(rsRecords.getBoolean(i));
										break;
									default:
										jp = new JsonPrimitive(rsRecords.getString(i));
										break;
								}
							}
						}
						catch(SQLException e)
						{
							if(e.getMessage().startsWith("Cannot convert value '0000-00-00 00:00:00'"))
							{
								Log.error("Data incompatibile con JDBC, sostituita con null: " + e.getMessage());
								jp = null;
							}
						}
						jRecord.add(jp);
					}
					gson.toJson(jRecord, jw);
				}
/*
 * Finito di iterare i record, è bene chiudere il relativo ResultSet, che può
 * essere molto ingombrante, rilasciando le risorse
 */
				rsRecords.close();
/*
 * Si chiude l'array dei record, si scrive il numero di record alla fine
 * e poi si chiude tutto
 */
				jw.endArray();
				jw.name("numberOfRecords").value(--rowNum);
				jw.endObject();
				jw.close();
				pw.close();
			}
			rsTables.close();
		}
		catch(SQLException e)
		{
			Log.error("Errore SQL genericoooo: " + e.getMessage());
		}
		catch(FileNotFoundException e)
		{
			Log.error("File non trovato: " + e.getMessage());
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}