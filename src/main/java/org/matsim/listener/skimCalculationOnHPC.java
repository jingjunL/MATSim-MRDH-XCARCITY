package org.matsim.listener;

//Script for running skim matrices analysis on HPC

import ch.sbb.matsim.analysis.skims.CalculateSkimMatrices;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Time;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Set;

import static ch.sbb.matsim.analysis.skims.CalculateSkimMatrices.ZONE_LOCATIONS_FILENAME;

public class skimCalculationOnHPC {

    public static void main(String[] args) throws IOException {

        String eventsFilenameDefault = "./input/skimAnalysis/matsimMRDHBaseline_extendedPT.output_events.xml.gz";
        String outputDirectoryDefault = "./MRDHEveningPeak";
        int numberOfThreadsDefault = 20;
        String timesCarStrDefault = "59400;61200;63000;64800";
//        PT Calculation 1 hour for calculation efficiency
        String timesPtStrDefault  = "59400;63000";

//        ZuidHolland is actually MRDH (Gebiden == 1 or 2)
        String zonesShapeFilename = "./input/skimAnalysis/MRDH.shp";
        String zonesIdAttributeName = "SUBZONE0";
//        String facilitiesFilename = args[2];
        String networkFilename = "./input/skimAnalysis/networkWithRideAndBike.xml.gz";
        String transitScheduleFilename = "./input/skimAnalysis/ptSchedule36Hour.xml";
//        String eventsFilename = "scenarios/skimMatrices/matsimMRDHBaseline_extendedPT.output_events.xml.gz";
//        String outputDirectory = "scenarios/skimMatrices/MRDHEveningPeak";
//        String outputDirectory = "scenarios/skimMatrices/MRDHEveningPeak";
//        String outputDirectory = "scenarios/skimMatrices/MRDHRestDag";

        int numberOfPointsPerZone = 5;
//        int numberOfThreads = 5;

//        For car time period
//        Morning peak period: 25200;27000;28800;30600;32400
//        Evening peak period: 57600;59400;61200;63000;64800
//        rest dag: 0;21600;43200;72000;86400
//        String[] timesCarStr = "57600;59400;61200;63000;64800".split(";");

//        For PT time period
//        Morning peak: 25200;32400
//        Evening peak: 57600;64800
//        rest dag: 36000;54000
//        String[] timesPtStr = "57600;64800".split(";");

//        config args lines for specifying in HPC
        String eventsFilename   = args.length > 0 ? args[0] : eventsFilenameDefault;
        String outputDirectory  = args.length > 1 ? args[1] : outputDirectoryDefault;
        int numberOfThreads     = args.length > 2 ? Integer.parseInt(args[2]) : numberOfThreadsDefault;
        String timesCarStrInput = args.length > 3 ? args[3] : timesCarStrDefault;
        String timesPtStrInput  = args.length > 4 ? args[4] : timesPtStrDefault;

        String[] timesCarStrArr = timesCarStrInput.split(";");
        String[] timesPtStrArr  = timesPtStrInput.split(";");

        Set<String> modes = CollectionUtils.stringToSet("car,pt");

        double[] timesCar = new double[timesCarStrArr.length];
        for (int i = 0; i < timesCarStrArr.length; i++) {
            timesCar[i] = Time.parseTime(timesCarStrArr[i]);
        }

        double[] timesPt = new double[timesPtStrArr.length];
        for (int i = 0; i < timesPtStrArr.length; i++) {
            timesPt[i] = Time.parseTime(timesPtStrArr[i]);
        }

        Config config = ConfigUtils.createConfig();
        Random r = new Random(4711);

        CalculateSkimMatrices skims = new CalculateSkimMatrices(outputDirectory, numberOfThreads);

//        skims.calculateSamplingPointsPerZoneFromFacilities(facilitiesFilename, numberOfPointsPerZone, zonesShapeFilename, zonesIdAttributeName, r, f -> 1);


        // alternative if you don't have facilities, use the network:
        skims.calculateSamplingPointsPerZoneFromNetwork(networkFilename, numberOfPointsPerZone, zonesShapeFilename, zonesIdAttributeName, r);

        skims.writeSamplingPointsToFile(new File(outputDirectory, ZONE_LOCATIONS_FILENAME));

        // or load pre-calculated sampling points from an existing file:
        // skims.loadSamplingPointsFromFile("coordinates.csv");

//        calculate the CAR Travel time for each interval and get the average value
        if (modes.contains(TransportMode.car)) {
            skims.calculateAndWriteNetworkMatrices(networkFilename, eventsFilename, timesCar, config, "", l -> true);
        }


//        PT travel time is to calculate the time from 9 to 10 (so travel time within 1 hour)
        if (modes.contains(TransportMode.pt)) {
            skims.calculateAndWritePTMatrices(networkFilename,
                    transitScheduleFilename,
                    timesPt[0],
                    timesPt[1],
                    config,
                    "",
                    (line, route) -> route.getTransportMode().equals("pt"));
        }

        skims.calculateAndWriteBeelineMatrix();
    }


}
