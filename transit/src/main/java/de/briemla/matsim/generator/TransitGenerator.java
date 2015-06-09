package de.briemla.matsim.generator;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

/**
 * Create a {@link Population} where a {@link Person} is added at each
 * {@link Node}. The plan for each person contains the home {@link Node} as
 * start and end activity and a random {@link Node} as work location.
 *
 * @author lars
 *
 */
public class TransitGenerator {
	private static final String CONFIG_FILE = "./input/config.xml";
	private static final String TRANSIT_SCHEDULE_FILE = "./input/transitschedule.xml";
	private static final Duration MORNING_LEAVE_TIME = Duration.ofHours(6);
	private static final Duration WORK_LEAVE_TIME = Duration.ofHours(16);

	/*
	 * We enter coordinates in the WGS84 reference system, but we want them to
	 * appear in the population file projected to UTM33N, because we also
	 * generated the network that way.
	 */
	private static final CoordinateTransformation COORDINATE_TRANSFORMATION = TransformationFactory
			.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

	private final Config config;
	private final Scenario scenario;
	private final Network network;
	private final TransitSchedule transitSchedule;
	private final TransitScheduleFactory transitScheduleFactory;
	private final Vehicles transitVehicles;

	public TransitGenerator() {
		config = ConfigUtils.loadConfig(CONFIG_FILE);
		scenario = ScenarioUtils.loadScenario(config);
		network = scenario.getNetwork();
		transitSchedule = scenario.getTransitSchedule();
		transitScheduleFactory = transitSchedule.getFactory();
		transitVehicles = scenario.getTransitVehicles();
	}

	private void createSetup() {
		createTransitSchedule();
	}

	private void createTransitSchedule() {
		// network.getNodes().values().stream().forEach(this::createPerson);
		TransitStopFacility startStop = addStopFacility("1");
		TransitStopFacility endStop = addStopFacility("3");

		List<Id<Link>> routeIds = Arrays.asList(Id.createLinkId("11"), Id.createLinkId("12"), Id.createLinkId("23"),
				Id.createLinkId("33"));
		TransitRouteStop start = transitScheduleFactory.createTransitRouteStop(startStop, 0, 0);
		start.setAwaitDepartureTime(true);
		TransitRouteStop end = transitScheduleFactory.createTransitRouteStop(endStop, 9 * 60, 0);
		end.setAwaitDepartureTime(true);

		TransitLine transitLine = transitScheduleFactory.createTransitLine(Id.create("Tram 1", TransitLine.class));
		TransitRoute transitRoute = createRoute(routeIds, start, end);
		transitLine.addRoute(transitRoute);
		transitSchedule.addTransitLine(transitLine);

		saveTransitSchedule();
	}

	private TransitRoute createRoute(List<Id<Link>> routeIds, TransitRouteStop start, TransitRouteStop end) {
		NetworkRoute route = RouteUtils.createNetworkRoute(routeIds, network);
		List<TransitRouteStop> stops = Arrays.asList(start, end);
		TransitRoute transitRoute = transitScheduleFactory.createTransitRoute(Id.create("2", TransitRoute.class),
				route, stops, "pt");

		return addDeparturesTo(transitRoute);
	}

	private TransitRoute addDeparturesTo(TransitRoute transitRoute) {
		for (int departure = 0; departure < 10; departure++) {
			long offset = Duration.ofMinutes(5 * departure).getSeconds();
			long startTime = Duration.ofHours(6).getSeconds();
			Departure departureTime = transitScheduleFactory.createDeparture(Id.create(departure, Departure.class),
					startTime + offset);
			departureTime.setVehicleId(Id.create("tr_1", Vehicle.class));
			transitRoute.addDeparture(departureTime);
		}
		return transitRoute;
	}

	private TransitStopFacility addStopFacility(String id) {
		Node startNode = network.getNodes().get(Id.createNodeId(id));
		Id<TransitStopFacility> stopId = Id.create(id, TransitStopFacility.class);
		if (transitSchedule.getFacilities().containsKey(stopId)) {
			return transitSchedule.getFacilities().get(stopId);
		}
		TransitStopFacility startStop = transitScheduleFactory.createTransitStopFacility(stopId, startNode.getCoord(),
				false);
		startStop.setLinkId(Id.createLinkId(id + id));
		transitSchedule.addStopFacility(startStop);
		return startStop;
	}

	private void saveTransitSchedule() {
		TransitScheduleWriter popWriter = new TransitScheduleWriter(transitSchedule);
		popWriter.writeFile(TRANSIT_SCHEDULE_FILE);
	}

	private void startSimulation() {
		Controler controler = new Controler(config);
		controler.run();
	}

	public static void main(String[] args) {
		LocalTime start = LocalTime.now();

		TransitGenerator generator = new TransitGenerator();
		// generator.createSetup();
		LocalTime afterSetup = LocalTime.now();

		generator.startSimulation();

		LocalTime end = LocalTime.now();
		Duration setup = Duration.between(start, afterSetup);
		Duration simulation = Duration.between(afterSetup, end);
		Duration complete = Duration.between(start, end);

		System.out.println("Creation and simulation took: " + complete.getSeconds() + "s");
		System.out.println("Creation took: " + setup.getSeconds() + "s");
		System.out.println("Simulation took: " + simulation.getSeconds() + "s");
	}

}
