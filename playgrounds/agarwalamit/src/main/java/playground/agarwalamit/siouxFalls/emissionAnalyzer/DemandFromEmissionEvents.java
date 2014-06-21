/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.siouxFalls.emissionAnalyzer;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkWarmEventHandler;

/**
 * @author amit
 */
public class DemandFromEmissionEvents {
	private final Logger logger = Logger.getLogger(DemandFromEmissionEvents.class);

	private final String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMC/";
	private final int noOfTimeBins = 30;
	private double simulationEndTime;
	private String configFile =runDir+"run113"+"/output_config.xml"; 

	private static String [] runNumber =  {"run115","run116"};
	private final String netFile1 = runDir+"run113"+"/output_network.xml.gz";
	private Network network;
	public static void main(String[] args) {
		new DemandFromEmissionEvents().writeDemandData();
	}

	private void writeDemandData(){

		network = loadScenario(netFile1).getNetwork();

		this.simulationEndTime = getEndTime(configFile);

		Map<Double, Map<Id, Double>> demandBAU = filterLinks(processEmissionsAndReturnDemand(runNumber[0])); 
		Map<Double, Map<Id, Double>> demandPolicy = filterLinks(processEmissionsAndReturnDemand(runNumber[1]));

		writeAbsoluteDemand(runDir+runNumber[0]+"/analysis/emissionVsCongestion/hourlyNetworkDemand.txt", demandBAU);
		writeAbsoluteDemand(runDir+runNumber[1]+"/analysis/emissionVsCongestion/hourlyNetworkDemand.txt", demandPolicy);

//		writeChangeInDemand(runDir+runNumber[1]+"/analysis/emissionVsCongestion/hourlyChangeInNetworkDemandWRTBAU.txt", demandBAU, demandPolicy);

		logger.info("Writing file(s) is finished.");
	}

	private void writeAbsoluteDemand(String outputFolder,Map<Double, Map<Id, Double>> linkCountMap ){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder);
		try {
			writer.write("time \t linkCounts  \n");

			for(double time :linkCountMap.keySet()){
				double hrDemand =0;
				writer.write(time+"\t");
				for(Id id:linkCountMap.get(time).keySet()){
					hrDemand += linkCountMap.get(time).get(id);
				}
				writer.write(hrDemand+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written into file. Reason : "+e);
		}
	}

	private void writeChangeInDemand(String outputFolder,Map<Double, Map<Id, Double>> linkCountMapBAU, Map<Double, Map<Id, Double>> linkCountMapPolicy ){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder);
		try {
			writer.write("time \t %ChangeInlinkCounts  \n");

			for(double time :linkCountMapBAU.keySet()){
				double hrDemandBAU =0;
				double hrDemandPolicy=0;
				writer.write(time+"\t");
				for(Id id:linkCountMapBAU.get(time).keySet()){
					hrDemandBAU += linkCountMapBAU.get(time).get(id);
					double tempPolicyDemand =0;
					if(linkCountMapPolicy.get(time).get(id)!=null){
						tempPolicyDemand = linkCountMapPolicy.get(time).get(id);
					} else tempPolicyDemand =0;
					hrDemandPolicy += tempPolicyDemand; 

				}
				writer.write(percentageChange(hrDemandBAU, hrDemandPolicy)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written into file. Reason : "+e);
		}
	}

	private Map<Double, Map<Id,  Double>> filterLinks (Map<Double, Map<Id, Double>> time2LinksData) {
		Map<Double, Map<Id, Double>> time2LinksDataFiltered = new HashMap<Double, Map<Id, Double>>();

		for(Double endOfTimeInterval : time2LinksData.keySet()){
			Map<Id,  Double> linksData = time2LinksData.get(endOfTimeInterval);
			Map<Id, Double> linksDataFiltered = new HashMap<Id,  Double>();

			for(Link link : network.getLinks().values()){
				Id linkId = link.getId();

				if(linksData.get(linkId) == null){
					linksDataFiltered.put(linkId, 0.);
				} else {
					linksDataFiltered.put(linkId, linksData.get(linkId));
				}
			}
			time2LinksDataFiltered.put(endOfTimeInterval, linksDataFiltered);
		}
		return time2LinksDataFiltered;
	}
	private Map<Double, Map<Id, Double>> processEmissionsAndReturnDemand(String runNumber){
		String emissionFileBAU = runDir+runNumber+"/ITERS/it.100/100.emission.events.xml.gz";

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);

		EmissionsPerLinkWarmEventHandler warmHandler = new EmissionsPerLinkWarmEventHandler(this.simulationEndTime, noOfTimeBins);
		eventsManager.addHandler(warmHandler);
		emissionReader.parse(emissionFileBAU);
		return warmHandler.getTime2linkIdLeaveCount();
	}

	private Double getEndTime(String configfile) {
		Config config = ConfigUtils.createConfig();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.qsim().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (endTime / 3600 / noOfTimeBins) + " hour time bins.");
		return endTime;
	}
	private double percentageChange(double firstNr, double secondNr){
		if(firstNr!=0) return (secondNr-firstNr)*100/firstNr;
		else return 0.;
	}
	private Scenario loadScenario(String netFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
}
