package org.matsim.project;

//Bicycle as network mode but used as teleportation (so reflect bicycle speed, but not influencing traffic)
//No need to downscale the pt capacity as we do not know it at the beginning from GTFS in any sense

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

public class runMRDHScenario {

    public static void main(String[] args) {

        Config config;
        if ( args==null || args.length==0 || args[0]==null ){
            config = ConfigUtils.loadConfig( "scenarios/MRDH/config.xml" );
        } else {
            config = ConfigUtils.loadConfig( args );
        }
        config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.kinematicWaves );
        config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.kinematicWaves );

        config.qsim().setFlowCapFactor(0.1);
        config.qsim().setStorageCapFactor(0.1);

        config.controller().setRoutingAlgorithmType(ControllerConfigGroup.RoutingAlgorithmType.SpeedyALT);

//        thread setting for HPC
        config.global().setNumberOfThreads(12);
        config.qsim().setNumberOfThreads(11);

        for (long i = 60; i <= 90000; i+=60) {
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("eatout_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("escort_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("home_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("others_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("school_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("shopping_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("social_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("univ_" + i).setTypicalDuration(i));
            config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("work_" + i).setTypicalDuration(i));
        }

        config.qsim().setPcuThresholdForFlowCapacityEasing(0.5);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

//        As bike is now a network mode, need to create bike vehicle (so that it is not using the speed as car)
        {
//            Add other network modes
            VehicleType carVehicle = scenario.getVehicles().getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
            carVehicle.setMaximumVelocity(120/3.6);
            carVehicle.setLength(5);
            carVehicle.setWidth(2);
            scenario.getVehicles().addVehicleType(carVehicle);

            VehicleType bikeVehicle = scenario.getVehicles().getFactory().createVehicleType(Id.create("bike", VehicleType.class));
//            assume average bike speed in NL is 18 km/h
            bikeVehicle.setMaximumVelocity(18/3.6);
            bikeVehicle.setLength(1.8);
            bikeVehicle.setWidth(0.5);
            bikeVehicle.setLength(2.5);
//            As there are mostly bike lanes in the Netherlands next to motorways, and bike is a network mode in the simulation
//            we set bike PCU to a very low value
            bikeVehicle.setPcuEquivalents(0.01);
            scenario.getVehicles().addVehicleType(bikeVehicle);

            VehicleType ebikeVehicle = scenario.getVehicles().getFactory().createVehicleType(Id.create("ebike", VehicleType.class));
//            assume average ebike speed in NL is 25 km/h
            bikeVehicle.setMaximumVelocity(25/3.6);
            bikeVehicle.setLength(1.8);
            bikeVehicle.setWidth(0.5);
            bikeVehicle.setLength(2.5);
//            same reason as bike
            bikeVehicle.setPcuEquivalents(0.01);
            scenario.getVehicles().addVehicleType(ebikeVehicle);

            VehicleType rideVehicle = scenario.getVehicles().getFactory().createVehicleType(Id.create(TransportMode.ride, VehicleType.class));
            rideVehicle.setMaximumVelocity(120/3.6);
            rideVehicle.setLength(5);
            rideVehicle.setWidth(2);
            scenario.getVehicles().addVehicleType(rideVehicle);
        }

        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);

        //        Change pt passenger car equivalents to the downsize value
        for (VehicleType vehicleType : scenario.getTransitVehicles().getVehicleTypes().values()) {
            vehicleType.setPcuEquivalents(vehicleType.getPcuEquivalents() * config.qsim().getFlowCapFactor());
        }

        Controler controler = new Controler( scenario );

//        No need to use this controler as we use the skim matrices calculation from sbb
//        controler.addControlerListener(new carFinalIterationTravelTimeListener());

//        Use the congested car travel time for the teleported ride mode

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
                addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );
            }
        });

        controler.run();

    }
}
