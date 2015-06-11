package de.briemla.matsim.generator;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * Create a {@link Population} where a {@link Person} is added at each
 * {@link Node}. The plan for each person contains the home {@link Node} as
 * start and end activity and a random {@link Node} as work location.
 *
 * @author lars
 *
 */
public class PrimitivePopulationGenerator {
	private static final String CONFIG_FILE = "./input/config_population_karlsruhe.xml";
	private static final String POPULATION_FILE = "./input/population.xml";
	private static final Duration MORNING_LEAVE_TIME = Duration.ofHours(6);
	private static final Duration WORK_LEAVE_TIME = Duration.ofHours(16);

	/*
	 * We enter coordinates in the WGS84 reference system, but we want them to
	 * appear in the population file projected to UTM33N, because we also
	 * generated the network that way.
	 */
	private static final CoordinateTransformation COORDINATE_TRANSFORMATION = TransformationFactory
			.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);
	private static final int DAXLANDEN = 11786 / 2;
	private static final int DURLACH = 29511 / 2;
	private int personId = 0;

	private final Config config;
	private final Scenario scenario;
	private final Network network;
	private final Population population;
	private final PopulationFactory populationFactory;
	private final boolean clearPopulation = true;

	public PrimitivePopulationGenerator() {
		config = ConfigUtils.loadConfig(CONFIG_FILE);
		scenario = ScenarioUtils.loadScenario(config);
		network = scenario.getNetwork();
		population = scenario.getPopulation();
		if (clearPopulation) {
			population.getPersons().clear();
		}
		populationFactory = population.getFactory();
	}

	private void createSetup() {
		City karlsruhe = splitNetwork();
		createPopulation(karlsruhe);
	}

	/**
	 * Split the {@link Network} in its {@link District}s.
	 *
	 * @return {@link City} divided into several {@link District}s containing
	 *         {@link Node}s from the {@link Network}.
	 */
	private City splitNetwork() {
		DistrictGenerator generator = new DistrictGenerator(network);
		return generator.createCity();
	}

	/**
	 * Create a person at each {@link Node}
	 *
	 * @param city
	 *            {@link City} to create population and plans for
	 */
	private void createPopulation(City city) {
		List<District> districts = city.getDistricts();
		if (districts.size() < 2) {
			throw new RuntimeException("Too few districts.");
		}
		createDaxlandenDurlach(districts);
		// createAll(districts);
		savePopulation();
	}

	private void createAll(List<District> districts) {
		for (int district = 0; district < districts.size(); district++) {
			District homeDistrict = districts.get(district);
			District workDistrict = districts.get((district + 1) % districts.size());
			homeDistrict.nodes().forEach(node -> createPerson(homeDistrict, workDistrict));
		}
	}

	private void createDaxlandenDurlach(List<District> districts) {
		District homeDistrict = districts.get(1);

		District workDistrict = districts.get(2);
		for (int inhabitant = 0; inhabitant < DAXLANDEN; inhabitant++) {
			createPerson(homeDistrict, workDistrict);
		}
		for (int inhabitant = 0; inhabitant < DURLACH; inhabitant++) {
			createPerson(workDistrict, homeDistrict);
		}
	}

	/**
	 * Create a new {@link Person} if there has no {@link Person} been created
	 * for the current {@link Node}
	 *
	 * @param homeDistrict
	 *            {@link District} where each person works
	 * @param workDistrict
	 */
	private void createPerson(District homeDistrict, District workDistrict) {
		if (population.getPersons().containsKey(nextPersonId())) {
			return;
		}
		Person person = populationFactory.createPerson(nextPersonId());
		population.addPerson(person);

		Plan plan = createPlanFrom(homeDistrict, workDistrict);
		person.addPlan(plan);
	}

	/**
	 * Convert {@link Node} id to {@link Person} id.
	 *
	 * @return new id for a {@link Person}
	 */
	private Id<Person> nextPersonId() {
		return Id.createPersonId(personId++);
	}

	/**
	 * Create a plan for the person. Start at node coordinates and travel to
	 * center of map. Assuming that node coordinates are already in correct
	 * coordinate system
	 *
	 * @param workDistrict
	 * @param workDistrict
	 *
	 * @return new plan which starts at node, travels to center of map and
	 *         travels back to node.
	 */
	private Plan createPlanFrom(District homeDistrict, District workDistrict) {
		Plan plan = populationFactory.createPlan();
		Activity homeMorning = populationFactory.createActivityFromCoord("home", coordinate(homeDistrict));
		homeMorning.setEndTime(morningLeaveTime());
		plan.addActivity(homeMorning);
		plan.addLeg(populationFactory.createLeg("car"));
		Activity workActivity = populationFactory.createActivityFromCoord("work", coordinate(workDistrict));
		workActivity.setEndTime(workLeaveTime());
		plan.addActivity(workActivity);
		plan.addLeg(populationFactory.createLeg("car"));
		Activity homeEvening = populationFactory.createActivityFromCoord("home", coordinate(homeDistrict));
		plan.addActivity(homeEvening);
		return plan;
	}

	private static Coord coordinate(District workDistrict) {
		List<Node> nodes = workDistrict.getNodes();
		int nodeIndex = (int) (Math.random() * nodes.size());
		return nodes.get(nodeIndex).getCoord();
	}

	private double workLeaveTime() {
		return randomize(WORK_LEAVE_TIME).getSeconds();
	}

	private double morningLeaveTime() {
		return randomize(MORNING_LEAVE_TIME).getSeconds();
	}

	/**
	 * Store population in {@link PrimitivePopulationGenerator#POPULATION_FILE}.
	 */
	private void savePopulation() {
		MatsimWriter popWriter = new PopulationWriter(population, network);
		popWriter.write(POPULATION_FILE);
	}

	/**
	 * Randomly add a time between -60 and + 60 minutes. The method will return
	 * a new {@link Duration} instance.
	 *
	 * @param time
	 *            base {@link Duration} to add minutes to
	 * @return new instance of {@link Duration} with added minutes
	 */
	private static Duration randomize(Duration time) {
		long minutes = (long) ((Math.random() * 120) - 60);
		return time.plusMinutes(minutes);
	}

	private void startSimulation() {
		Controler controler = new Controler(config);
		controler.run();
	}

	public static void main(String[] args) {
		LocalTime start = LocalTime.now();

		PrimitivePopulationGenerator generator = new PrimitivePopulationGenerator();
		generator.createSetup();
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
