package de.briemla.matsim.generator;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;

/**
 * Create a {@link Population} where a {@link Person} is added at each
 * {@link Node}. The plan for each person contains the home {@link Node} as
 * start and end activity and a random {@link Node} as work location.
 *
 * @author lars
 *
 */
public class DistrictGenerator {
	private static final String KML_FILE = "./input/doc.kml";

	/*
	 * We enter coordinates in the WGS84 reference system, but we want them to
	 * appear in the population file projected to UTM33N, because we also
	 * generated the network that way.
	 */
	private static final CoordinateTransformation COORDINATE_TRANSFORMATION = TransformationFactory
			.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

	private final Network network;

	public DistrictGenerator(Network network) {
		this.network = network;
	}

	/**
	 * Creates a {@link City} from kml {@link District}s and the {@link Network}
	 *
	 * @return generated {@link City} where {@link Node}s are inside their
	 *         {@link District}s
	 */
	City createCity() {
		Folder folder = getFolderFromKml();
		List<Placemark> placemarks = folder.getFeature().stream().map((feature) -> (Placemark) feature)
				.filter(placemark -> !"Landkreisgrenze".equals(placemark.getName())).collect(Collectors.toList());
		return moveNodesIntoDistricts(network.getNodes(), placemarks);
	}

	private Folder getFolderFromKml() {
		Kml kml = Kml.unmarshal(new File(KML_FILE));
		if (kml.getFeature() instanceof Document) {
			Document document = (Document) kml.getFeature();
			if (document.getFeature().isEmpty()) {
				throw new RuntimeException("Empty document!");
			}
			if (document.getFeature().get(0) instanceof Folder) {
				return (Folder) document.getFeature().get(0);
			}
			throw new RuntimeException("No folder found inside kml: " + KML_FILE);
		}
		throw new RuntimeException("No document found inside kml: " + KML_FILE);
	}

	/**
	 * Create a person at each {@link Node}
	 *
	 * @param nodes
	 *            nodes to be grouped into districts
	 * @param placemarks
	 *            coordinates of districts
	 * @return generated {@link City} where {@link Node}s are inside their
	 *         {@link District}s
	 */
	private City moveNodesIntoDistricts(Map<Id<Node>, ? extends Node> nodes, List<Placemark> placemarks) {
		City karlsruhe = createDistrictsFrom(placemarks);
		karlsruhe.addNodes(nodes);
		return karlsruhe;
	}

	private City createDistrictsFrom(List<Placemark> placemarks) {
		City karlsruhe = new City(COORDINATE_TRANSFORMATION);
		karlsruhe.addDistricts(placemarks);
		return karlsruhe;
	}

}
