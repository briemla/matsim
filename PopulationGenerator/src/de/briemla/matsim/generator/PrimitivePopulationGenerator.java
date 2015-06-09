package de.briemla.matsim.generator;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;

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
	private static final String CONFIG_FILE = "./input/config.xml";
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
		createPopulation();
	}

	/**
	 * Create a person at each {@link Node}
	 */
	private void createPopulation() {
		network.getNodes().values().stream().forEach(this::createPerson);
		savePopulation();
	}

	private void savePopulation() {
		MatsimWriter popWriter = new PopulationWriter(population, network);
		popWriter.write(POPULATION_FILE);
	}

	/**
	 * Create a new {@link Person} if there has no {@link Person} been created
	 * for the current {@link Node}
	 *
	 * @param node
	 *            to derive {@link Person} from
	 */
	private void createPerson(Node node) {
		if (population.getPersons().containsKey(idFrom(node))) {
			return;
		}
		Person person = populationFactory.createPerson(idFrom(node));
		population.addPerson(person);

		Plan plan = createPlanFrom(node);
		person.addPlan(plan);
	}

	/**
	 * Create a plan for the person. Start at node coordinates and travel to
	 * center of map. Assuming that node coordinates are already in correct
	 * coordinate system
	 *
	 * @param node
	 *            start node for plan
	 * @return new plan which starts at node, travels to center of map and
	 *         travels back to node.
	 */
	private Plan createPlanFrom(Node node) {
		Plan plan = populationFactory.createPlan();
		Activity homeMorning = populationFactory.createActivityFromCoord("home", node.getCoord());
		homeMorning.setEndTime(morningLeaveTime());
		plan.addActivity(homeMorning);
		plan.addLeg(populationFactory.createLeg("car"));
		Activity workActivity = populationFactory.createActivityFromCoord("work", workCoordinate());
		workActivity.setEndTime(workLeaveTime());
		plan.addActivity(workActivity);
		plan.addLeg(populationFactory.createLeg("car"));
		Activity homeEvening = populationFactory.createActivityFromCoord("home", node.getCoord());
		plan.addActivity(homeEvening);
		return plan;
	}

	private Coord workCoordinate() {
		ArrayList<? extends Node> nodes = new ArrayList<>(network.getNodes().values());
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
	 * Randomly add a time between -60 and + 60 minutes. The method will return
	 * a new {@link Duration} instance.
	 *
	 * @param time
	 *            base {@link Duration} to add minutes to
	 * @return new instance of {@link Duration} with added minutes
	 */
	private static Duration randomize(Duration time) {
		long minutes = (long) ((Math.random() * 60) - 30);
		return time.plusMinutes(minutes);
	}

	/**
	 * Convert {@link Node} id to {@link Person} id.
	 *
	 * @param node
	 *            to take id from
	 * @return new id for a {@link Person}
	 */
	private static Id<Person> idFrom(Node node) {
		return Id.createPersonId(node.getId().toString());
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
