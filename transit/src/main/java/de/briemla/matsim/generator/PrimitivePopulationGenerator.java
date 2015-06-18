package de.briemla.matsim.generator;

import java.time.Duration;
import java.time.LocalTime;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

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

	private final Config config;
	private final Scenario scenario;
	private final Network network;
	private final Population population;
	private final boolean clearPopulation = true;

	public PrimitivePopulationGenerator() {
		config = ConfigUtils.loadConfig(CONFIG_FILE);
		scenario = ScenarioUtils.loadScenario(config);
		network = scenario.getNetwork();
		population = scenario.getPopulation();
		if (clearPopulation) {
			population.getPersons().clear();
		}
	}

	private void createSetup(Statistic statistics) {
		City karlsruhe = splitNetwork(network, statistics);
		createPopulation(karlsruhe);
		savePopulation();
	}

	/**
	 * Split the {@link Network} in its {@link District}s.
	 *
	 * @param statistic
	 *            provides information about inhabitants and work places
	 *
	 * @return {@link City} divided into several {@link District}s containing
	 *         {@link Node}s from the {@link Network}.
	 */
	private static City splitNetwork(Network network, Statistic statistic) {
		DistrictGenerator generator = new DistrictGenerator(network, statistic);
		return generator.createCity();
	}

	/**
	 * Create a person at each {@link Node}
	 *
	 * @param city
	 *            {@link City} to create population and plans for
	 */
	private void createPopulation(City city) {
		for (int inhabitant = 0; inhabitant < city.getInhabitants(); inhabitant++) {
			city.getRandomAvailableHomeDistrict().createPerson(population, city.getAvailableWorkDistricts());
			city.cleanUpAvailableDistricts();
		}
	}

	/**
	 * Store population in {@link PrimitivePopulationGenerator#POPULATION_FILE}.
	 */
	private void savePopulation() {
		MatsimWriter popWriter = new PopulationWriter(population, network);
		popWriter.write(POPULATION_FILE);
	}

	private void startSimulation() {
		Controler controler = new Controler(config);
		controler.run();
	}

	public static void main(String[] args) {
		LocalTime start = LocalTime.now();
		Statistic statistic = Statistic.karlsruhe();

		PrimitivePopulationGenerator generator = new PrimitivePopulationGenerator();
		generator.createSetup(statistic);
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
