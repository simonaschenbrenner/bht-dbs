package birdflu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.ChartUtils;

/**
 * Class for generating charts for the semester project "Bird Flu"
 * 
 * Modul Datenbanksysteme, Dozent Aljoscha Marcel Everding, SS2020
 * 
 * @author Simon Aschenbrenner, Luis Rieke, Paul Gronemeyer, Büsra Bagci
 * 
 * Charts are generated using:
 * JFreeChart : a free chart library for the Java(tm) platform
 * (C) Copyright 2000-2017, by Object Refinery Limited and Contributors.
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 * [Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.]
 */

public class ChartPlotter {

	// Change these values as needed
	public static int CHART_WIDTH = 1280;
	public static int CHART_HEIGHT = 720;
	public static Level LOG_LEVEL = Level.FINE;
	
	// Do not change these constants
	public static final int INIT = 1; // Drop tables and create them from scratch
	public static final int CREATE = 2; // Don't drop, create tables from scratch
	public static final int NO_INIT = 0; // Keep database, just create the charts
	
	public static void main(String[] args) {
		Locale.setDefault(Locale.US);
		Logger sqlLogger  = Logger.getLogger("SQL Logger");
		Logger chrtLogger = Logger.getLogger("Chart Logger");
		Logger fileLogger = Logger.getLogger("File Logger");
		sqlLogger.setLevel(Level.ALL);
		chrtLogger.setLevel(Level.ALL);
		fileLogger.setLevel(Level.ALL);
		sqlLogger.setUseParentHandlers(false);
		chrtLogger.setUseParentHandlers(false);
		fileLogger.setUseParentHandlers(false);
		Handler handler = new ConsoleHandler();
		handler.setFormatter(new FluFormatter());
		handler.setLevel(LOG_LEVEL);
		sqlLogger.addHandler(handler);
		chrtLogger.addHandler(handler);
		fileLogger.addHandler(handler);
		
		// Constructor's parameter must be either INIT, CREATE or NO_INIT (see above)
		new ChartPlotter(NO_INIT).createCharts();
	}
	
	public ChartPlotter(int init) {
		switch(init) {
			case 0: break;
			case 1: CreateTable.init(); break;
			case 2: CreateTable.create(); break;
			default:
				Logger.getLogger("File Logger").warning
					("Wrong database initialization parameter");
		}		
	}
	
	/**
	 * Scans the input directory for files to create and save charts using the read-
	 * and save-methods below. The filename will be carried through as an identifier
	 */
	
	public void createCharts() {
		StringWriter reportWriter = new StringWriter();
		int counter = 0;
		
		File[] input = new File("./input/").listFiles(new InputFileFilter());
		Scanner in = null;
		
		for(File file : input) {
			String filename = file.getName();
			if(filename.length() == 11) { filename = filename.substring(0, 7); };
			if(filename.length() == 12) { filename = filename.substring(0, 8); };
			if(filename.length() == 13) { filename = filename.substring(0, 9); };
			Logger.getLogger("File Logger").finer("Reading " + filename);
			try {
				in = new Scanner(file);
				BirdFluChart chart = read(in, filename);
				if(chart != null) {
					if(save(chart)) {
						reportWriter.write(chart.getFilename() + ", ");
						counter++;
						Logger.getLogger("File Logger").fine("Created chart for " + filename);
					}
				}
			} catch (FileNotFoundException e) {
				Logger.getLogger("File Logger").warning
					(filename + " not found: " + e.getMessage());
			} catch (Exception e) {
				Logger.getLogger("File Logger").warning
				("Error while reading " + filename + ": " + e.getMessage());
			} finally {
				if(in != null) {
					try {
						in.close();
					} catch (Exception e) {
						Logger.getLogger("File Logger").warning
						("Error while closing " + filename + ": " + e.getMessage());
					}
				}
			}
		}
		
		if(counter > 0) {
			String report = reportWriter.toString();
			Logger.getLogger("File Logger").info(String.format("%d/%d saved charts: %s",
					counter, input.length, report.substring(0, report.length()-2)));
		} else {
			Logger.getLogger("File Logger").warning("No charts saved");
		}
	}
	
	/**
	 * Creates charts using the correct factory method specified in the first line of the
	 * input file. The files have to be formatted in the exact way as specified in the
	 * switch/case-block
	 * @param in - the scanner used for reading the information contained in each file
	 * @param filename - used as an identifier of the source of each chart
	 * @return 	a chart implementing the BirdFluChart interface, so it can be saved later
	 * @throws NoSuchElementException
	 */
	
	private BirdFluChart read(Scanner in, String filename)
			throws NoSuchElementException, NumberFormatException {
		
		String chartType = in.nextLine();
		int threshold = Integer.parseInt(in.nextLine());
		String title = in.nextLine();
		
		switch (chartType) {
			case "LineChartPCM": // Für: Anzahl der Suchanfragen (Frage 1B)
				String xAxis = in.nextLine();
				String yAxis = in.nextLine();
				String dividendQuery = in.nextLine();
				String divisorQuery = in.nextLine();
				Logger.getLogger("File Logger").finer(filename + " read succesfully");
				return LineChart.createLineChartPCM
						(filename, new Query(dividendQuery), new Query(divisorQuery),
								title, xAxis, yAxis);
			case "PieChart": // Für: Popularität der Suchbegriffe (Frage 1A)
				String pieQuery = in.nextLine();
				Logger.getLogger("File Logger").finer(filename + " read succesfully");
				return PieChart.createPieChart(filename, new Query(pieQuery), title,
						threshold);
			case "BarChart1": // Für: Landesbezüge (Frage 2) und Webseitenkategorien (Frage 4)
				String catAxis1 = in.nextLine();
				String valAxis1 = in.nextLine();
				boolean horizontal1 = (Integer.parseInt(in.nextLine()) != 0) ? true : false;
				String barQuery = in.nextLine();
				Logger.getLogger("File Logger").finer(filename + " read succesfully");
				return BarChart.createBarChart1(filename, barQuery, title, catAxis1, valAxis1,
						horizontal1, threshold);
			case "BarChart2": // Für: Andere Krankheiten (Frage 9) und Folgesuchen (Fragen 6-8)
				String catAxis2 = in.nextLine();
				String valAxis2 = in.nextLine();
				boolean horizontal2 = (Integer.parseInt(in.nextLine()) != 0) ? true : false;
				String refQuery = in.nextLine();
				String datQuery = in.nextLine();
				Logger.getLogger("File Logger").finer(filename + " read succesfully");
				return BarChart.createBarChart2(filename, refQuery, datQuery, title, catAxis2,
						valAxis2, horizontal2, threshold);
			case "GroupedBarChart": // Für Ländersuchen vs. Fälle (Frage 10)
				String catAxis3 = in.nextLine();
				String valAxis3 = in.nextLine();
				String c1rQuery = in.nextLine();
				String c1dQuery = in.nextLine();
				String c2rQuery = in.nextLine();
				String c2dQuery = in.nextLine();
				Logger.getLogger("File Logger").finer(filename + " read succesfully");
				return GroupedBarChart.createGroupedBarChart(filename, c1rQuery, c1dQuery,
						c2rQuery, c2dQuery, title, catAxis3, valAxis3, false, threshold);
			default:
				return null;
		}
	}
	
	/**
	 * Saves the charts to storage as a PNG file. Parameters like directory, width and
	 * height of the image are specified as static constants in the class attributes.
	 * @param chart - the chart to be saved (must implement the BirdFluChart interface)
	 * @return true if the chart was saved successfully, false if not
	 */
	
	public boolean save(BirdFluChart chart) {
		if(chart != null) {
			try {
				ChartUtils.saveChartAsPNG(new File("./output/" + chart.getFilename()
				+ ".png"), chart.getChart(), CHART_WIDTH, CHART_HEIGHT);
				Logger.getLogger("File Logger").finer
				("Chart for " + chart.getFilename() + " saved succesfully");
				return true;
			} catch (IOException e) {
				Logger.getLogger("File Logger").warning
				("Error while saving chart for " + chart.getFilename() + ": "
				+ e.getMessage());
				return false;
			}
		} else {
			Logger.getLogger("File Logger").severe
			("Could not save chart because it does not exist");
			return false;
		}
	}
}
