package de.briemla.matsim.converter;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

import de.briemla.matsim.generator.City;
import de.briemla.matsim.generator.DistrictGenerator;

public class NetworkConverter {

	private static final CoordinateTransformation TRANSFORM_TO_WSG84_UTM33N = TransformationFactory
			.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

	public static void main(String[] args) {
		LocalTime start = LocalTime.now();
		Network osmNetwork = loadOsmNetwork();
		LocalTime afterLoad = LocalTime.now();
		// shrinkToKarlsruhe(osmNetwork);
		LocalTime afterShrink = LocalTime.now();

		Node startNode = osmNetwork.getNodes().get(Id.createNodeId(1670591290));
		osmNetwork.getNodes().get(Id.createLinkId(131484444));
		new NetworkCleaner().run(osmNetwork);
		LocalTime afterClean = LocalTime.now();
		write(osmNetwork);
		LocalTime end = LocalTime.now();

		Duration load = Duration.between(start, afterLoad);
		Duration shrink = Duration.between(afterLoad, afterShrink);
		Duration clean = Duration.between(afterShrink, afterClean);
		Duration write = Duration.between(afterClean, end);
		Duration complete = Duration.between(start, end);

		System.out.println("Overall time: " + complete.getSeconds() + "s");
		System.out.println("Load took: " + load.getSeconds() + "s");
		System.out.println("Shrink took: " + shrink.getSeconds() + "s");
		System.out.println("Clean took: " + clean.getSeconds() + "s");
		System.out.println("Write took: " + write.getSeconds() + "s");
	}

	private static void write(Network karlsruheNetwork) {
		new NetworkWriter(karlsruheNetwork).write("./input/karlsruhe.xml");
	}

	private static void shrinkToKarlsruhe(Network osmNetwork) {
		DistrictGenerator districtGenerator = new DistrictGenerator(osmNetwork);
		City karlsruhe = districtGenerator.createCity();
		List<Id<Node>> nodesToRemove = osmNetwork.getNodes().values().stream()
				.filter(node -> !karlsruhe.isInside(node)).map(Node::getId).collect(Collectors.toList());
		nodesToRemove.stream().forEach(osmNetwork::removeNode);
	}

	private static Network loadOsmNetwork() {
		String osm = "/home/lars/Downloads/karlsruhe-regbez-latest.xml";
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network administrativeRegion = sc.getNetwork();
		OsmNetworkReader onr = new OsmNetworkReader(administrativeRegion, TRANSFORM_TO_WSG84_UTM33N);
		// TODO change OSMNetworkReader to convert unused nodes. Otherwise
		// transit nodes will be removed.
		onr.parse(osm);
		return administrativeRegion;
	}

}
