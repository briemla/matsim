package de.briemla.matsim.generator;

import java.util.HashMap;
import java.util.Map;

public class Statistic {

	private static final int SCALE_FACTOR = 50;

	public static Statistic karlsruhe() {
		Statistic karlruhe = new Statistic();
		karlruhe.add(newCensus("Innenstadt Ost", 4692, 1457));
		karlruhe.add(newCensus("Innenstadt West", 7608, 2187));
		karlruhe.add(newCensus("Südstadt", 13469, 1875));
		karlruhe.add(newCensus("Südweststadt", 13792, 1744));
		karlruhe.add(newCensus("Weststadt", 13848, 1630));
		karlruhe.add(newCensus("Nordweststadt", 7072, 237));
		karlruhe.add(newCensus("Oststadt", 15254, 11489));
		karlruhe.add(newCensus("Mühlburg", 10711, 25168));
		karlruhe.add(newCensus("Daxlanden", 6995, 17598));
		karlruhe.add(newCensus("Knielingen", 6319, 51442));
		karlruhe.add(newCensus("Grünwinkel", 6525, 7716));
		karlruhe.add(newCensus("Oberreut", 5818, 2848));
		karlruhe.add(newCensus("Beiertheim-Bulach", 4470, 2159));
		karlruhe.add(newCensus("Weiherfeld-Dammerstock", 3458, 106));
		karlruhe.add(newCensus("Rüppurr", 6340, 1476));
		karlruhe.add(newCensus("Waldstadt", 7289, 290));
		karlruhe.add(newCensus("Rintheim", 3551, 7783));
		karlruhe.add(newCensus("Hagsfeld", 4810, 16677));
		karlruhe.add(newCensus("Durlach", 18716, 41673));
		karlruhe.add(newCensus("Grötzingen", 5606, 2222));
		karlruhe.add(newCensus("Stupferich", 1735, 1365));
		karlruhe.add(newCensus("Hohenwettersbach", 1835, 868));
		karlruhe.add(newCensus("Wolfartsweier", 2047, 421));
		karlruhe.add(newCensus("Grünwettersbach", 2416, 1267));
		karlruhe.add(newCensus("Palmbach", 1187, 289));
		karlruhe.add(newCensus("Neureut", 11682, 14901));
		karlruhe.add(newCensus("Nordstadt", 6506, 1553));
		return karlruhe;
	}

	private static Census newCensus(String name, int inhabitants, int workplaces) {
		return new Census(name, inhabitants / SCALE_FACTOR, workplaces / SCALE_FACTOR);
	}

	private final Map<String, Census> districts;

	public Statistic() {
		districts = new HashMap<String, Census>();
	}

	private void add(Census census) {
		districts.put(census.getName(), census);
	}

	public Census findCensus(String districtName) {
		if (districts.containsKey(districtName)) {
			return districts.get(districtName);
		}
		throw new IllegalArgumentException("No district available named: " + districtName);
	}
}
