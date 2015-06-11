package de.briemla.matsim.generator;

import java.awt.geom.Path2D;
import java.awt.geom.Path2D.Double;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;

public class District {

	private final Double border;
	private final List<Node> nodes;

	public District() {
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
	 * Add a {@link Coordinate} of the border to this district
	 *
	 * @param coordinate
	 */
	public void add(Coordinate coordinate) {
		if (coordinate == null) {
			return;
		}
		if (border.getCurrentPoint() == null) {
			border.moveTo(coordinate.getLongitude(), coordinate.getLatitude());
			return;
		}
		border.lineTo(coordinate.getLongitude(), coordinate.getLatitude());
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

}
