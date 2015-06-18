package de.briemla.matsim.generator;

import java.awt.geom.Path2D;
import java.awt.geom.Path2D.Double;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;

public class District {

	private final Double border;
	private final List<Node> nodes;
	private final String name;
	private final Census census;

	public District(String name, Census census) {
		this.name = name;
		this.census = census;
		border = new Path2D.Double(Path2D.WIND_EVEN_ODD);
		nodes = new ArrayList<>();
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

}
