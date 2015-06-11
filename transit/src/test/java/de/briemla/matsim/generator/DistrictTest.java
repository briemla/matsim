package de.briemla.matsim.generator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;

public class DistrictTest {

	@Test
	public void contains() throws Exception {
		District district = new District();

		assertThat(district.isInside(coord()), is(false));
	}

	@Test
	public void onePointBorder() throws Exception {
		District district = new District();
		district.add(new Coordinate(10.0, 20.0));

		assertThat(district.isInside(coord()), is(false));
	}

	@Test
	public void twoPointBorder() throws Exception {
		District district = new District();
		district.add(new Coordinate(10.0, 20.0));
		district.add(new Coordinate(20.0, 10.0));

		assertThat(district.isInside(coord()), is(false));
	}

	@Test
	public void threePointBorderCoordInside() throws Exception {
		District district = new District();
		district.add(new Coordinate(10.0, 20.0));
		district.add(new Coordinate(20.0, 10.0));
		district.add(new Coordinate(20.0, 20.0));

		assertThat(district.isInside(coord(19.0, 19.0)), is(true));
	}

	@Test
	public void threePointBorderCoordOutside() throws Exception {
		District district = new District();
		district.add(new Coordinate(10.0, 20.0));
		district.add(new Coordinate(20.0, 10.0));
		district.add(new Coordinate(20.0, 20.0));

		assertThat(district.isInside(coord(21.0, 19.0)), is(false));
	}

	@Test
	public void threePointBorderCoordOnLine() throws Exception {
		District district = new District();
		district.add(new Coordinate(10.0, 20.0));
		district.add(new Coordinate(20.0, 10.0));
		district.add(new Coordinate(20.0, 20.0));

		assertThat(district.isInside(coord(20.0, 19.0)), is(false));
	}

	@Test
	public void threePointBorderCoordOnPoint() throws Exception {
		District district = new District();
		district.add(new Coordinate(10.0, 20.0));
		district.add(new Coordinate(20.0, 10.0));
		district.add(new Coordinate(20.0, 20.0));

		assertThat(district.isInside(coord(20.0, 20.0)), is(false));
	}

	@Test
	public void nonConvexBorderCoordInside() throws Exception {
		District district = new District();
		district.add(new Coordinate(20.0, 20.0));
		district.add(new Coordinate(10.0, 20.0));
		district.add(new Coordinate(12.0, 12.0));
		district.add(new Coordinate(20.0, 10.0));

		assertThat(district.isInside(coord(19.0, 19.0)), is(true));
	}

	@Test
	public void nonConvexBorderCoordOutside() throws Exception {
		District district = new District();
		district.add(new Coordinate(20.0, 20.0));
		district.add(new Coordinate(10.0, 20.0));
		district.add(new Coordinate(12.0, 12.0));
		district.add(new Coordinate(20.0, 10.0));

		assertThat(district.isInside(coord(21.0, 19.0)), is(false));
	}

	private static Coord coord() {
		return coord(10.0, 20.0);
	}

	private static Coord coord(double x, double y) {
		return new CoordImpl(x, y);
	}
}
