package de.briemla.matsim.generator;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;

public class City {

	private static final String DISTANCE_CSV = "Distance.csv";
	private static final String WORKER_CSV = "Worker.csv";
	private static final String SEPARATOR = ";";
	private static final double METER_TO_KILOMETER = 0.001;
	private final List<District> districts;
	private final CoordinateTransformation coordinateTransformation;
	private final Statistic statistic;
	private List<District> availableWorkDistricts;
	private List<District> availableHomeDistricts;

	public City(Statistic statistic, CoordinateTransformation coordinateTransformation) {
		this.statistic = statistic;
		this.coordinateTransformation = coordinateTransformation;
		districts = new ArrayList<>();
		availableWorkDistricts = new ArrayList<>();
		availableHomeDistricts = new ArrayList<>();
	}

	public void addDistricts(List<Placemark> placemarks) {
		placemarks.forEach(this::addDistrict);
	}

	private void addDistrict(Placemark placemark) {
		Geometry geometry = placemark.getGeometry();
		if (geometry instanceof Polygon) {
			String name = placemark.getName();
			add(name, coordinates((Polygon) geometry));
		}
	}

	private void add(String name, List<Coordinate> coordinates) {
		Census census = statistic.findCensus(name);
		District district = new District(name, census);
		coordinates.forEach(coordinate -> district.add(transformed(coordinate)));
		add(district);
	}

	private void add(District district) {
		districts.add(district);
		if (district.hasNonWorkingInhabitants()) {
			availableHomeDistricts.add(district);
		}
		if (district.hasFreeWorkplace()) {
			availableWorkDistricts.add(district);
		}
	}

	private Coord transformed(Coordinate coordinate) {
		return coordinateTransformation.transform(new CoordImpl(coordinate.getLongitude(), coordinate.getLatitude()));
	}

	private List<Coordinate> coordinates(Polygon geometry) {
		Boundary outerBoundaryIs = geometry.getOuterBoundaryIs();
		LinearRing linearRing = outerBoundaryIs.getLinearRing();
		return linearRing.getCoordinates();
	}

	public void addNodes(Map<Id<Node>, ? extends Node> nodes) {
		nodes.values().stream().forEach(node -> add(node));
	}

	/**
	 * Adds the {@link Node} to the first district which contains this
	 * {@link Node}
	 *
	 * @param node
	 *            {@link Node} to be added
	 */
	private void add(Node node) {
		for (District district : districts) {
			if (district.add(node)) {
				break;
			}
		}
	}

	public Boolean isInside(Node node) {
		return districts.stream().anyMatch(district -> district.isInside(node.getCoord()));
	}

	public List<District> getDistricts() {
		return Collections.unmodifiableList(districts);
	}

	public Stream<Node> nodes() {
		List<Node> nodes = new ArrayList<Node>();
		for (District district : districts) {
			district.nodes().forEach(node -> nodes.add(node));
		}
		return nodes.stream();
	}

	public District getRandomAvailableHomeDistrict() {
		int districtIndex = new Random().nextInt(availableHomeDistricts.size());
		return availableHomeDistricts.get(districtIndex);
	}

	public int getInhabitants() {
		return districts.stream().collect(Collectors.summingInt(District::getInhabitants));
	}

	public List<District> getAvailableWorkDistricts() {
		return availableWorkDistricts;
	}

	public void cleanUpAvailableDistricts() {
		cleanUpWorkDistricts();
		availableHomeDistricts = availableHomeDistricts.stream().filter(District::hasNonWorkingInhabitants)
				.collect(Collectors.toList());
	}

	private void cleanUpWorkDistricts() {
		availableWorkDistricts = availableWorkDistricts.stream().filter(District::hasFreeWorkplace)
				.collect(Collectors.toList());
	}

	/**
	 * Create all inhabitants of the {@link City}
	 *
	 * @param population
	 *            {@link Population} element to add {@link Person}s to
	 */
	public void createPopulation(Population population) {
		for (int inhabitant = 0; inhabitant < getInhabitants(); inhabitant++) {
			getRandomAvailableHomeDistrict().createPerson(population, districts);
			cleanUpAvailableDistricts();
		}
	}

	public void writeMatricesTo(File outputDirectory) {
		printDistanceMatrix(outputDirectory);
		printWorkerMatrix(outputDirectory);
	}

	private void printDistanceMatrix(File outputDirectory) {
		NumberFormat toDecimal = NumberFormat.getNumberInstance(Locale.GERMAN);
		try (BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputDirectory, DISTANCE_CSV)))) {
			districts.sort((district1, district2) -> district1.getName().compareTo(district2.getName()));
			printDistrictNamesTo(output);
			output.newLine();
			for (District from : districts) {
				Point2D fromCenter = from.getCenter();
				output.write(from.getName());
				for (District to : districts) {
					Point2D toCenter = to.getCenter();
					double distance = fromCenter.distance(toCenter) * METER_TO_KILOMETER;
					output.write(SEPARATOR + toDecimal.format(distance));
				}
				output.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printDistrictNamesTo(BufferedWriter output) {
		districts.stream().forEach(district -> {
			try {
				output.write(SEPARATOR + district.getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void printWorkerMatrix(File outputDirectory) {
		try (BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputDirectory, WORKER_CSV)))) {
			districts.sort((district1, district2) -> district1.getName().compareTo(district2.getName()));
			printDistrictNamesTo(output);
			output.write(SEPARATOR + "Gesamt");
			output.newLine();
			for (District from : districts) {
				from.printWorkerTo(output, districts);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
