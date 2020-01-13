package cpsLib;

import java.util.HashMap;
import java.util.Map;

public final class Resources {
	private Map<String, Route> routeMap;

	public Resources(C.RESOURCE_FROM mode) {
		if (mode == C.RESOURCE_FROM.FILE) {
			// TODO: file auslesen, xml?
			routeMap = new HashMap<>();
			
			routeMap.put("HAMBURG-HAFENRUNDE", new Route("Hamburg-Hafenrunde", 
					new RoutePoint("Strand-Pauli", 53.3247, 9.57415),
					new RoutePoint("Dammtor", 53.563446, 9.985735),
					new RoutePoint("Planetarium Hamburg", 53.59726, 10.00895),
					new RoutePoint("Mundsburg", 53.56930, 10.02740),
					new RoutePoint("Hamburg-Hauptbahnhof", 53.33158, 10.00255)));
		} else if (mode == C.RESOURCE_FROM.DB) {
			// TODO: setup Database
			routeMap = new HashMap<>();
		}
	}

	public Map<String, Route> getRouteMap() {
		return routeMap;
	}
}
