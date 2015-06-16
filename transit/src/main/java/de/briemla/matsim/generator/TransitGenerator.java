package de.briemla.matsim.generator;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
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
	private static final String NETWORK_FILE = "./input/karlsruhe.xml";
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
		// TransitStopFacility startStop = addStopFacility("1");
		// TransitStopFacility endStop = addStopFacility("3");

		List<TransitRouteStop> stops = createStops();
		List<Id<Link>> routeIds = createRouteIds();// Arrays.asList(Id.createLinkId("11"),
		// Id.createLinkId("12"),
		// Id.createLinkId("23"),
		// Id.createLinkId("33"));
		// TransitRouteStop start =
		// transitScheduleFactory.createTransitRouteStop(startStop, 0, 0);
		// start.setAwaitDepartureTime(true);
		// TransitRouteStop end =
		// transitScheduleFactory.createTransitRouteStop(endStop, 9 * 60, 0);
		// end.setAwaitDepartureTime(true);

		TransitLine transitLine = transitScheduleFactory.createTransitLine(Id.create("Tram 1", TransitLine.class));
		TransitRoute transitRoute = createRoute(routeIds, stops);
		transitLine.addRoute(transitRoute);
		transitSchedule.addTransitLine(transitLine);

		saveTransitSchedule();
	}

	private List<TransitRouteStop> createStops() {
		ArrayList<TransitRouteStop> stops = new ArrayList<>();
		int timeToStart = 0;
		Coord startCoordinate = COORDINATE_TRANSFORMATION.transform(scenario.createCoord(8.4785858, 48.9991233));
		Coord endCoordinate = COORDINATE_TRANSFORMATION.transform(scenario.createCoord(8.4731161, 48.999838));
		stops.add(createStop(1670591290, Id.createLinkId(999990), startCoordinate, timeToStart));
		stops.add(createStop(1447845533, Id.createLinkId(999993), endCoordinate, ++timeToStart));
		// stops.add(createStop(31100886, timeToStart++));
		// stops.add(createStop(1447726306, timeToStart++));
		// stops.add(createStop(1717702175, timeToStart++));
		// stops.add(createStop(1138408754, timeToStart++));
		// stops.add(createStop(322658556, timeToStart++));
		// stops.add(createStop(1138378490, timeToStart++));
		// stops.add(createStop(1744171523, timeToStart++));
		// stops.add(createStop(1135823865, timeToStart++));
		// stops.add(createStop(3547526508l, timeToStart++));
		// stops.add(createStop(187432355, timeToStart++));
		// stops.add(createStop(1741790339, timeToStart++));
		// stops.add(createStop(1743158354, timeToStart++));
		// stops.add(createStop(1124934741, timeToStart++));
		// stops.add(createStop(25504894, timeToStart++));
		// stops.add(createStop(3181300893l, timeToStart++));
		// stops.add(createStop(312276685, timeToStart++));
		// stops.add(createStop(3170124330l, timeToStart++));
		// stops.add(createStop(311375937, timeToStart++));
		// stops.add(createStop(1637997527, timeToStart++));
		// stops.add(createStop(1637997548, timeToStart++));
		// stops.add(createStop(1716361371, timeToStart++));
		// stops.add(createStop(1697538980, timeToStart++));
		// stops.add(createStop(1697372665, timeToStart++));
		// stops.add(createStop(1717771246, timeToStart++));
		// stops.add(createStop(1516245037, timeToStart++));
		// stops.add(createStop(1516245033, timeToStart++));
		// stops.add(createStop(1516245041, timeToStart++));
		// stops.add(createStop(1516246112, timeToStart++));
		// stops.add(createStop(1516245035, timeToStart++));
		return stops;
	}

	private TransitRouteStop createStop(long id, Id<Link> linkReference, Coord coordinates, int timeToStart) {
		TransitStopFacility facility = addStopFacility(id, linkReference, coordinates);
		TransitRouteStop stop = transitScheduleFactory.createTransitRouteStop(facility, Duration.ofMinutes(timeToStart)
				.getSeconds(), 0);
		stop.setAwaitDepartureTime(true);
		return stop;
	}

	private List<Id<Link>> createRouteIds() {
		ArrayList<Id<Link>> ids = new ArrayList<>();
		Node from = network.getNodes().get(Id.createNodeId(1670591290));
		Node to = network.getNodes().get(Id.createNodeId(1447845533));
		Link fromFrom = networkFactory().createLink(Id.createLinkId(999990), from, from);
		Link fromTo = networkFactory().createLink(Id.createLinkId(999991), from, to);
		Link toFrom = networkFactory().createLink(Id.createLinkId(999992), to, from);
		Link toTo = networkFactory().createLink(Id.createLinkId(999993), to, to);
		network.addLink(fromFrom);
		network.addLink(fromTo);
		network.addLink(toFrom);
		network.addLink(toTo);
		ids.add(Id.createLinkId(999990));
		ids.add(Id.createLinkId(999991));
		ids.add(Id.createLinkId(999993));
		// ids.add(Id.createLinkId(999992));
		// ids.add(Id.createLinkId(131484444));
		// ids.add(Id.createLinkId(131484453));
		// ids.add(Id.createLinkId(131564490));
		// ids.add(Id.createLinkId(131567340));
		// ids.add(Id.createLinkId(131567348));
		// ids.add(Id.createLinkId(305237622));
		// ids.add(Id.createLinkId(131484457));
		// ids.add(Id.createLinkId(98394762));
		// ids.add(Id.createLinkId(292345753));
		// ids.add(Id.createLinkId(98394759));
		// ids.add(Id.createLinkId(98394760));
		// ids.add(Id.createLinkId(245985155));
		// ids.add(Id.createLinkId(98391975));
		// ids.add(Id.createLinkId(98391978));
		// ids.add(Id.createLinkId(134547036));
		// ids.add(Id.createLinkId(134547034));
		// ids.add(Id.createLinkId(152815243));
		// ids.add(Id.createLinkId(98241445));
		// ids.add(Id.createLinkId(134765876));
		// ids.add(Id.createLinkId(152815241));
		// ids.add(Id.createLinkId(134547038));
		// ids.add(Id.createLinkId(243421504));
		// ids.add(Id.createLinkId(251682930));
		// ids.add(Id.createLinkId(219456549));
		// ids.add(Id.createLinkId(173809971));
		// ids.add(Id.createLinkId(234094222));
		// ids.add(Id.createLinkId(234094232));
		// ids.add(Id.createLinkId(243421500));
		// ids.add(Id.createLinkId(150385932));
		// ids.add(Id.createLinkId(187680846));
		// ids.add(Id.createLinkId(191735769));
		// ids.add(Id.createLinkId(219438648));
		// ids.add(Id.createLinkId(150385890));
		// ids.add(Id.createLinkId(352645649));
		// ids.add(Id.createLinkId(303353664));
		// ids.add(Id.createLinkId(352645645));
		// ids.add(Id.createLinkId(352645642));
		// ids.add(Id.createLinkId(352645652));
		// ids.add(Id.createLinkId(97218664));
		// ids.add(Id.createLinkId(150939178));
		// ids.add(Id.createLinkId(167081407));
		// ids.add(Id.createLinkId(248464613));
		// ids.add(Id.createLinkId(308727886));
		// ids.add(Id.createLinkId(308727887));
		// ids.add(Id.createLinkId(309066198));
		// ids.add(Id.createLinkId(297369893));
		// ids.add(Id.createLinkId(150939199));
		// ids.add(Id.createLinkId(308727889));
		// ids.add(Id.createLinkId(150939171));
		// ids.add(Id.createLinkId(165380097));
		// ids.add(Id.createLinkId(150939212));
		// ids.add(Id.createLinkId(165380407));
		// ids.add(Id.createLinkId(151052580));
		// ids.add(Id.createLinkId(151052578));
		// ids.add(Id.createLinkId(138293880));
		return ids;
	}

	private TransitRoute createRoute(List<Id<Link>> routeIds, List<TransitRouteStop> stops) {
		NetworkRoute route = RouteUtils.createNetworkRoute(routeIds, network);
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

	private TransitStopFacility addStopFacility(long id, Id<Link> linkReference, Coord coordinates) {
		Node startNode = networkFactory().createNode(Id.createNodeId(id), coordinates);
		network.addNode(startNode);
		;// getNodes().get(Id.createNodeId(id));
		Id<TransitStopFacility> stopId = Id.create(id, TransitStopFacility.class);
		if (transitSchedule.getFacilities().containsKey(stopId)) {
			return transitSchedule.getFacilities().get(stopId);
		}
		TransitStopFacility startStop = transitScheduleFactory.createTransitStopFacility(stopId, startNode.getCoord(),
				false);
		startStop.setLinkId(linkReference);
		// startStop.setLinkId(Id.createLinkId(id + id));
		transitSchedule.addStopFacility(startStop);
		return startStop;
	}

	private NetworkFactory networkFactory() {
		return network.getFactory();
	}

	private void saveTransitSchedule() {
		TransitScheduleWriter popWriter = new TransitScheduleWriter(transitSchedule);
		popWriter.writeFile(TRANSIT_SCHEDULE_FILE);
		NetworkWriter networkWriter = new NetworkWriter(network);
		networkWriter.write(NETWORK_FILE);
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
