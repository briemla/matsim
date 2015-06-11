package de.briemla.matsim.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;

public class City {

	private final List<District> districts;

	public City() {
		districts = new ArrayList<>();
	}

	public void addDistricts(List<Placemark> placemarks) {
		placemarks.forEach(this::addDistrict);
	}

	private void addDistrict(Placemark placemark) {
		Geometry geometry = placemark.getGeometry();
		if (geometry instanceof Polygon) {
			add(coordinates((Polygon) geometry));
		}
	}

	private void add(List<Coordinate> coordinates) {
		District district = new District();
		coordinates.forEach(coordinate -> district.add(coordinate));
		districts.add(district);
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
}
