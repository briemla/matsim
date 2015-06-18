package de.briemla.matsim.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;

public class City {

	private final List<District> districts;
	private final CoordinateTransformation coordinateTransformation;
	private final Statistic statistic;

	public City(Statistic statistic, CoordinateTransformation coordinateTransformation) {
		this.statistic = statistic;
		this.coordinateTransformation = coordinateTransformation;
		districts = new ArrayList<>();
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
		districts.add(district);
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
}
