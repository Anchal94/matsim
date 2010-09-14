/* *********************************************************************** *
 * project: org.matsim.*
 * CountsAnalyser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.anhorni.kti;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.matsim.analysis.CalcLinkStats;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.ComparisonErrorStatsCalculator;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.counts.algorithms.CountsGraphWriter;
import org.matsim.counts.algorithms.graphs.CountsErrorGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24GraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraphCreator;

import playground.dgrether.utils.DoubleArrayTableWriter;

/*
 * This class is able to compare traffic counts with traffic in the simulation.
 * The results are written to file in a format which has to be specified.
 *
 * original author: dgrether
 * changed version: anhorni
 *
 */
public class CountsAnalyser {

	private String linkstatsfile;
	/**
	 * name of the counts module in config
	 */
	public static final String COUNTS = "counts";

	/**
	 * name of the output format parameter in config
	 */
	public static final String OUTPUTFORMAT = "outputformat";

	/**
	 * name of the output file parameter in config
	 */
	public static final String OUTFILE = "outputCountsFile";

	/**
	 * name of the timefilter parameter in config
	 */
	public static final String TIMEFILTER = "timeFilter";

	/**
	 * name of the distancefilter parameter in config
	 */
	public static final String DISTANCEFILTER = "distanceFilter";

	/**
	 * name of the distancefilterCenterNode parameter in config
	 */
	public static final String DISTANCEFITLERCENTERNODE = "distanceFilterCenterNode";

	private Scenario scenario;
	/**
	 * the network
	 */
	private NetworkImpl network;

	/**
	 * the name(path) to the output file
	 */
	private String outputFile;

	/**
	 * the output format
	 */
	private String outputFormat;

	/**
	 * the distance filter in m
	 */
	private Double distanceFilter;

	/**
	 * the id of the node used as center for the distance filter
	 */
	private String distanceFilterCenterNode;
	/**
	 * the CalcLinkStats read for the analysis
	 */
	private CalcLinkStats linkStats;

	private String coordSystem;

	final private Counts counts =  new Counts();

	/**
	 *
	 * @param config
	 */
	public CountsAnalyser(final String linkstatsfile) {

		this.linkstatsfile = linkstatsfile;

		try {
			this.readConfig("../../matsim/input/config.xml");
			this.writeCountsComparisonList(this.outputFile, this.outputFormat);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(
					"Something wrong in config file, execution aborted!");
		}
	}

	/**
	 * Reads the parameters from the config file
	 *
	 * @param configFile
	 * @throws Exception
	 */
	private void readConfig(final String configFile) throws Exception {
		this.scenario = new ScenarioLoaderImpl(configFile).getScenario();
		Config config = this.scenario.getConfig();
		System.out.println("  reading counts xml file... ");
		MatsimCountsReader counts_parser = new MatsimCountsReader(counts);
		counts_parser.readFile(config.counts().getCountsFileName());
		System.out.println("  reading counts done.");
		System.out.println("  reading network...");
		this.network = loadNetwork();
		System.out.println("  done.");
		// reading config parameters
		System.out.println("Reading parameters...");
		String linksAttributeFilename = this.linkstatsfile;
		System.out.println("  Linkattribute File: " + linksAttributeFilename);
		this.outputFormat = config.getParam(COUNTS, OUTPUTFORMAT);
		this.outputFile = config.getParam(COUNTS, OUTFILE);
		System.out.println("  Output file: " + this.outputFile);
		this.distanceFilterCenterNode = config.counts().getDistanceFilterCenterNode();
		System.out.println("  Distance filter center node: " + this.distanceFilterCenterNode);
		this.distanceFilter = config.counts().getDistanceFilter();
		System.out.println("  Distance filter: " + this.distanceFilter);
		System.out.println("  Scale Factor: " + config.counts().getCountsScaleFactor());
		this.coordSystem = config.global().getCoordinateSystem();
		System.out.println("  Coordinate System: " + this.coordSystem);
		System.out.println("  reading LinkAttributes from: " + linksAttributeFilename);
		this.linkStats = new CalcLinkStats(this.network);
		this.linkStats.readFile(linksAttributeFilename);

		System.out.println("  done.");
	}

	/**
	 *
	 * @param calcLinkStats
	 * @return The table containing the count and sim values, link id and the
	 *         relative error.
	 */
	private List<CountSimComparison> createCountsComparisonList(
			final CalcLinkStats calcLinkStats) {
		// processing counts
		CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(this.linkStats,
				counts, this.network, this.scenario.getConfig().counts().getCountsScaleFactor());
		if ((this.distanceFilter != null) && (this.distanceFilterCenterNode != null))
			cca.setDistanceFilter(this.distanceFilter, this.distanceFilterCenterNode);
		cca.setCountsScaleFactor(this.scenario.getConfig().counts().getCountsScaleFactor());
		cca.run();
		return cca.getComparison();
	}

	/**
	 * Writes the results of the comparison to a file
	 *
	 * @param filename
	 *          the path to the kml file
	 * @param format
	 *          the format kml or txt
	 */
	private void writeCountsComparisonList(final String filename, final String format) {
		List<CountSimComparison> countsComparisonList = createCountsComparisonList(this.linkStats);
		if ((format.compareToIgnoreCase("kml") == 0) || (format.compareToIgnoreCase("all") == 0)) {
			CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
					countsComparisonList, this.network, TransformationFactory.getCoordinateTransformation(this.coordSystem, TransformationFactory.WGS84));
			kmlWriter.writeFile(filename);
		}
		
		if (format.compareToIgnoreCase("txt") == 0 || (format.compareToIgnoreCase("all") == 0)) {
			CountSimComparisonTableWriter writer = new CountSimComparisonTableWriter(countsComparisonList, Locale.US);
			writer.writeFile(filename);
			
			ComparisonErrorStatsCalculator errorStats = new ComparisonErrorStatsCalculator(countsComparisonList);

			double[] hours = new double[24];
			for (int i = 1; i < 25; i++) {
				hours[i-1] = i;
			}
			DoubleArrayTableWriter tableWriter = new DoubleArrayTableWriter();
			tableWriter.addColumn(hours);
			tableWriter.addColumn(errorStats.getMeanRelError());
			tableWriter.writeFile(filename.replace(".", "_") + "errortable.txt");
		}
		CountsGraphWriter writer = new CountsGraphWriter("output", countsComparisonList, 0 , true , true);
		writer.setGraphsCreator(new CountsSimRealPerHourGraphCreator("sim and real volumes"));
		writer.setGraphsCreator(new CountsErrorGraphCreator("errors"));
		writer.setGraphsCreator(new CountsLoadCurveGraphCreator("link volumes"));
		writer.setGraphsCreator(new CountsSimReal24GraphCreator("average working day sim and count volumes"));
		writer.createGraphs();
		writer.createGraphs();		
	}

	/**
	 * load the network
	 *
	 * @return the network layer
	 */
	protected NetworkImpl loadNetwork() {
		// - read network: which buildertype??
		printNote("", "  creating network layer... ");
		NetworkImpl network = NetworkImpl.createNetwork();
		printNote("", "  done");

		printNote("", "  reading network xml file... ");
		new MatsimNetworkReader(this.scenario).readFile(this.scenario.getConfig().network().getInputFile());
		printNote("", "  done");

		return network;
	}

	/**
	 * an internal routine to generated some (nicely?) formatted output. This
	 * helps that status output looks about the same every time output is written.
	 *
	 * @param header
	 *          the header to print, e.g. a module-name or similar. If empty
	 *          <code>""</code>, no header will be printed at all
	 * @param action
	 *          the status message, will be printed together with a timestamp
	 */
	private final void printNote(final String header, final String action) {
		if (header != "") {
			System.out.println();
			System.out.println("===============================================================");
			System.out.println("== " + header);
			System.out.println("===============================================================");
		}
		if (action != "") {
			System.out.println("== " + action + " at " + (new Date()));
		}
		if (header != "") {
			System.out.println();
		}
	}

	/**
	 * help output
	 *
	 */
	private static void printHelp() {
		System.out.println("This tool needs one config argument: The path to the linkstats.txt file!");
		System.out.println("A config file must be provided under the following path: input/config.xml");
		System.out.println("The config file must contain the following attributes: \n\n");

		System.out.println("  - The path to the file to which the output is written (mandatory)");
		System.out.println("  - The time filter (mandatory) 0 for 0 to 1 am, 1 for 1 to 2 am...");
		System.out.println("  - The distance filter (optional) the distance in km to filter the counts around a node that must be given in the subsequent argument.");
		System.out.println("  - The node id for the center of the distance filter (optinal, however mandatory if distance filter is set)");
	}

	/**
	 * See printHelp() method
	 *
	 * @param args
	 */
	public static void main(String[] args) {

		CountsAnalyser ca = null;
		if (args.length == 0) {
			printHelp();
		}
		else {
			ca = new CountsAnalyser(args[0]);
			System.out.println("File written to " + ca.outputFile);
		}
	}
}
