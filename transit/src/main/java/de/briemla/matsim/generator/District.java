package de.briemla.matsim.generator;

import java.awt.geom.Path2D;
import java.awt.geom.Path2D.Double;
import java.awt.geom.Point2D;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;

public class District {

	private static final int MINUTES_OF_PAUSE = 30;
	private static final int HOURS_OF_WORK = 8;
	private static final int MINUTES_TO_WORK = 30;
	private static final int MINUTES_IN_HOUR = 60;
	private static final int[] HOME_LEAVE_HOURS = IntStream.range(0, 24).toArray();
	private static final double[] HOME_LEAVE_TIME_PROBABILITIES = new double[] { 0.0, 0.005, 0.005, 0.005, 0.015,
		0.075, 0.235, 0.305, 0.12, 0.04, 0.03, 0.01, 0.03, 0.05, 0.02, 0.01, 0.005, 0.01, 0.005, 0.005, 0.01,
		0.005, 0.0, 0.005 };

	private final Double border;
	private final List<Node> nodes;
	private final String name;
	private final Census census;
	private int workingInhabitants = 0;
	private int workers = 0;
	private final EnumeratedIntegerDistribution homeLeaveTimeDistribution;

	public District(String name, Census census) {
		this.name = name;
		this.census = census;
		border = new Path2D.Double(Path2D.WIND_EVEN_ODD);
		nodes = new ArrayList<>();
		homeLeaveTimeDistribution = new EnumeratedIntegerDistribution(HOME_LEAVE_HOURS, HOME_LEAVE_TIME_PROBABILITIES);
	}

	/**
	 * Test if the given {@link Coord} is inside this {@link District}. If it is
	 * on the border line, the {@link Coord} will be marked as outside.
	 *
	 * @param coordinate
	 * @return
	 */
	public Boolean isInside(Coord coordinate) {
		return border.contains(pointFrom(coordinate));
	}

	/**
	 * Add a {@link Coord} of the border to this district
	 *
	 * @param coord
	 */
	public void add(Coord coord) {
		if (coord == null) {
			return;
		}
		if (border.getCurrentPoint() == null) {
			border.moveTo(coord.getX(), coord.getY());
			return;
		}
		border.lineTo(coord.getX(), coord.getY());
	}

	/**
	 * Add this {@link Node} to the district, if it lies inside the border.
	 *
	 * @param node
	 *            {@link Node} to be added
	 * @return <code>true</code> if the {@link Node} lies inside this district,
	 *         <code>false</code> otherwise.
	 */
	public Boolean add(Node node) {
		if (isInside(node.getCoord())) {
			nodes.add(node);
			return true;
		}
		return false;
	}

	/**
	 * Convert matsim {@link Coord} to {@link Point2D}
	 *
	 * @param coordinate
	 *            coordinate of matsim {@link Node}
	 * @return {@link Point2D} representing the coordinates of a matsim
	 *         {@link Node}
	 */
	private static Point2D pointFrom(Coord coordinate) {
		return new Point2D.Double(coordinate.getX(), coordinate.getY());
	}

	@Override
	public String toString() {
		return "District [name=" + name + "]";
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public Stream<Node> nodes() {
		return nodes.stream();
	}

	public int getInhabitants() {
		return census.getInhabitants();
	}

	boolean hasNonWorkingInhabitants() {
		return workingInhabitants < census.getInhabitants();
	}

	boolean hasFreeWorkplace() {
		return workers < census.getWorkplaces();
	}

	public void createPerson(Population population, List<District> districts) {
		if (!hasNonWorkingInhabitants()) {
			return;
		}
		District workDistrict = findWorkDistrict(districts);
		createPerson(this, workDistrict, population);
	}

	/**
	 * Randomly select one {@link District}. All {@link District}s must have at
	 * least one workplace left. Otherwise {@link IllegalArgumentException} will
	 * be thrown
	 *
	 * @param districts
	 *            {@link List} containing {@link District}s with workplaces
	 * @return selected {@link District}
	 * @throws IllegalArgumentException
	 *             when selected district does not have a workplace left.
	 */
	private District findWorkDistrict(List<District> districts) {
		int districtIndex = new Random().nextInt(districts.size());
		District district = districts.get(districtIndex);
		if (district.hasFreeWorkplace()) {
			return district;
		}
		throw new IllegalArgumentException("Selected district does not have a workplace left: " + district);
	}

	/**
	 * Create a new {@link Person} if there has no {@link Person} been created
	 * for the current {@link Node}
	 *
	 * @param homeDistrict
	 *            {@link District} where each person works
	 * @param workDistrict
	 * @param population
	 */
	private void createPerson(District homeDistrict, District workDistrict, Population population) {
		PopulationFactory factory = population.getFactory();
		Person person = factory.createPerson(nextPersonId());
		population.addPerson(person);

		Plan plan = createPlanFrom(homeDistrict, workDistrict, factory);
		person.addPlan(plan);
	}

	/**
	 * Convert {@link Node} id to {@link Person} id.
	 *
	 * @return new id for a {@link Person}
	 */
	private Id<Person> nextPersonId() {
		return Id.createPersonId(name + workingInhabitants++);
	}

	/**
	 * Create a plan for the person. Start at node coordinates and travel to
	 * center of map. Assuming that node coordinates are already in correct
	 * coordinate system
	 *
	 * @param workDistrict
	 * @param workDistrict
	 * @param populationFactory
	 *
	 * @return new plan which starts at node, travels to center of map and
	 *         travels back to node.
	 */
	private Plan createPlanFrom(District homeDistrict, District workDistrict, PopulationFactory populationFactory) {
		Plan plan = populationFactory.createPlan();
		Activity homeMorning = populationFactory.createActivityFromCoord("home", coordinate(homeDistrict));
		Duration homeLeaveTime = homeLeaveTime();
		homeMorning.setEndTime(homeLeaveTime.getSeconds());
		plan.addActivity(homeMorning);
		plan.addLeg(populationFactory.createLeg("car"));
		Activity workActivity = populationFactory.createActivityFromCoord("work", coordinate(workDistrict));
		workActivity.setEndTime(workLeaveTime(homeLeaveTime).getSeconds());
		workDistrict.increaseNumberOfWorkers();
		plan.addActivity(workActivity);
		plan.addLeg(populationFactory.createLeg("car"));
		Activity homeEvening = populationFactory.createActivityFromCoord("home", coordinate(homeDistrict));
		plan.addActivity(homeEvening);
		return plan;
	}

	private void increaseNumberOfWorkers() {
		workers++;
	}

	private static Coord coordinate(District workDistrict) {
		List<Node> nodes = workDistrict.getNodes();
		int nodeIndex = (int) (Math.random() * nodes.size());
		return nodes.get(nodeIndex).getCoord();
	}

	private Duration workLeaveTime(Duration homeLeaveTime) {
		return randomizeInNextHour(homeLeaveTime.plusMinutes(MINUTES_TO_WORK).plusHours(HOURS_OF_WORK)
				.plusMinutes(MINUTES_OF_PAUSE));
	}

	private Duration homeLeaveTime() {
		int leaveHour = homeLeaveTimeDistribution.sample();
		return randomizeInNextHour(Duration.ofHours(leaveHour));
	}

	/**
	 * Return a new {@link Duration} within the next hour based on the given
	 * {@link Duration}.
	 *
	 * @param leaveTime
	 *            start hour to add a random amount of time within an hour
	 * @return new {@link Duration} which is within an hour after the given
	 *         {@link Duration}
	 */
	private static Duration randomizeInNextHour(Duration leaveTime) {
		int leaveMinute = new Random().nextInt(MINUTES_IN_HOUR);
		return leaveTime.plusMinutes(leaveMinute);
	}

	public String getName() {
		return name;
	}

}
