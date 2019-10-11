package csv;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;

public class TimeSheet
{
	private static Logger log;
	private Properties config;
	private String dateA;
	private String dateB;
	private SpreadsheetDocument input, output;
  private int sheetNum;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
	private String oFilePrefix, oDirName;
	private String minSep;

	private void initLogger()
	{
		PatternLayout pl;
		PrintWriter pw = null;
		WriterAppender wa;
		log = Logger.getLogger("TIMESHEET");
// log.setLevel(Level.INFO);
		log.setLevel(Level.DEBUG);
		pl = new PatternLayout(config.getProperty("log.pattern"));
		try
		{
			pw = new PrintWriter(config.getProperty("log.file"));
		}
		catch(FileNotFoundException e)
		{
			log.error("File di log " + " non trovato: " + e.getMessage());
		}
		wa = new WriterAppender(pl, pw);
		log.addAppender(wa);
		wa = new WriterAppender(pl, System.out);
		log.addAppender(wa);
	}

// Un metodo primitivo ma estremamente efficace per ricavare i minuti da un
// tempo nel formato hh.mm.ss o simili

	private int minutes(String s)
	{
		log.debug("Orario input: " + s);
		String[] tokens = s.split(minSep);
		log.debug("Ore input: " + tokens[0]);
		log.debug("Minuti input: " + tokens[1]);
		int hh = Integer.parseInt(tokens[0]);
		int mm = Integer.parseInt(tokens[1]);
		return hh * 60 + mm;
	}

	public TimeSheet()
	{
		config = new Properties();
		try
		{
			config.load(new FileReader("src/timesheet.prop"));
			initLogger();
			log.debug("Inizializzo pts...");
			input = SpreadsheetDocument.loadDocument(config.getProperty("ods.input"));
//			wbInput = WorkbookFactory.create(new File(config.getProperty("xlsx.input")));
//			log.info("Il file in formato XLSX ha " + wbInput.getNumberOfSheets() + " fogli");
			minSep = config.getProperty("ods.minsep");
			log.debug("Separatore minuti: '" + minSep + "'");

/*
 * Il template viene solo caricato, mai salvato con lo stesso nome. Il foglio
 * badge dovrebbe stare sempre nella stessa posizione, ma i metodi disponibili
 * per queste operazioni sono carenti, quindi lo si elimina e lo si appende già
 * qui, in modo che in seguito risulterà sempre il numero 2.
 */

			output = SpreadsheetDocument.loadDocument(config
					.getProperty("ods.template"));
			output.removeSheet(0);
//			wbOutput = WorkbookFactory.create(new File(config.getProperty("xlsx.template")));
//			wbOutput.removeSheetAt(0);
			log.debug("Aggiunto il foglio badge con "
					+ output.appendSheet("badge").getHeaderRowCount()
					+ " righe di header");
		}
		catch(FileNotFoundException e)
		{
			log.warn("File non trovato: " + e.getMessage());
		}
		catch(IOException e)
		{
			log.error("Impossibile leggere il file di configurazione: "
					+ e.getMessage());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		dateA = config.getProperty("date.A");
		dateB = config.getProperty("date.B");
		log.info("Sarà lavorato il periodo dal " + dateA + " al " + dateB);
		sheetNum = input.getSheetCount();
		oFilePrefix = config.getProperty("ods.output.file.prefix");
		oDirName = config.getProperty("ods.output.dir");
//		xDirName = config.getProperty("xlsx.output.dir");
		File oDir = new File(oDirName);
//		File xDir = new File(xDirName);
		oDir.mkdirs();
//		xDir.mkdirs();
	}

	public void createTimeSheet(Table sheet)
	{
		String name = sheet.getTableName().toLowerCase();
		log.info("Elaborazione impiegato: " + name);

// va ripulito il foglio badge: si rimuove e si riappende

		output.removeSheet(2);
//		wbOutput.removeSheetAt(1);
		Table badge = output.appendSheet("badge");
//		Table badge = output.getSheetByName("badge");

// intestazioni delle due colonne

		badge.getCellByPosition("A1").setStringValue(
				config.getProperty("ods.output.headerA"));
		badge.getCellByPosition("B1").setStringValue(
				config.getProperty("ods.output.headerB"));
//		xBadge.getRow(1).getCell(N("A")).setCellValue(config.getProperty("ods.output.headerA"));
//		xBadge.getRow(1).getCell(N("B")).setCellValue(config.getProperty("ods.output.headerB"));

// per iterare fra le date indicate, servono due calendari

		Calendar calA = Calendar.getInstance();
		Calendar calB = Calendar.getInstance();
		try
		{
			calA.setTime(dateFormat.parse(dateA));
			calB.setTime(dateFormat.parse(dateB));
		}
		catch(ParseException e)
		{
			log.error("Data non corretta: " + e.getMessage());
		}

/*
 * A questo punto si può ciclare su tutte le date del periodo scelto. Per
 * ognuna, si prova a scorrere la lista delle righe del foglio attuale, e si
 * vede se la riga ha la stessa data e se ha la causale giusta. Per qualche
 * motivo, la prima riga "valida" del foglio sembra essere la terza. Potrebbe
 * essere una implementazione errata del metodo getRowIterator.
 */

		Iterator<Row> rows = sheet.getRowIterator();
		Row iRow, oRow = null;
		String date = null, rowDate;
		iRow = rows.next();
		iRow = rows.next();
		iRow = rows.next();
		int accu = 0;
		while(!calA.after(calB))
		{
			rowDate = iRow.getCellByIndex(2).getStringValue();
			date = dateFormat.format(calA.getTime());
			log.debug("Data riga input " + rowDate + ", data interna " + date);

/*
 * In questo caso, le due date coincidono e si possono accumulare le ore valide
 */

			if(date.equals(rowDate))
			{
				log.debug("La data java " + date + " e la data della riga " + rowDate
						+ " coincidono");
				String causale = iRow.getCellByIndex(6).getStringValue();
				int minuti = minutes(iRow.getCellByIndex(4).getStringValue());
//				|| causale.equals("Servizio esterno")
				if(
						causale.equals("Ore Lavorate")
						|| causale.equals("Ore eccedenti - anno corrente")
						|| causale.equals("Recupero permessi brevi")
						)
				{
					accu += minuti;
				}

/*
 * Se il foglio ha altre righe si passa alla prossima, altrimenti si deve uscire
 */

				if(rows.hasNext())
				{
					iRow = (Row) rows.next();
				}
				else
				{
					break;
				}
			}

/*
 * In questo caso, la data java non ha una riga nel foglio. Bisogna estrarre una
 * riga con la data e ore nulle e metterla nel foglio di output, prima di
 * passare ad una altra data e vedere se per questa c'è una corrispondenza nel
 * foglio di input
 */

			else
			{
				log.debug("La data java " + date + " e la data della riga " + rowDate
						+ " non coincidono");
				log.debug(date + ", " + accu);
				oRow = badge.appendRow();
				oRow.getCellByIndex(0).setDateValue(calA);
				oRow.getCellByIndex(1).setDoubleValue((double) accu);
				accu = 0;
				calA.add(Calendar.DATE, 1);
			}
		}

/*
 * Il ciclo è finito, ma c'è ancora una riga già calcolata che va inserita nel
 * foglio di output
 */

		try
		{
			System.out.println(date + ", " + accu);
			oRow = badge.appendRow();
			oRow.getCellByIndex(0).setDateValue(calA);
			oRow.getCellByIndex(1).setDoubleValue((double) accu);
			badge.removeRowsByIndex(1, 1);
			output.save(new File(oDirName + "/" + oFilePrefix + " " + name + ".ods"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public void createAll()
	{
		for(int i = 0; i < sheetNum; i++)
		{
			Table sheet = input.getSheetByIndex(i);
			createTimeSheet(sheet);
		}
	}

	public static void main(String[] args)
	{
		TimeSheet ts = new TimeSheet();
		ts.createAll();
	}
}
