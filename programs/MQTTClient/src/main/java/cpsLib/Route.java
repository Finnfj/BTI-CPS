package cpsLib;

import java.util.LinkedList;
import java.util.List;

public class Route {
	private List<RoutePoint> route = new LinkedList<>();
	private String name;
	
	public Route(String name, LinkedList<RoutePoint> route) {
		super();
		this.route = route;
		this.name = name;
	}
	
	public Route(String name) {
		super();
		this.name = name;
	}
	
	public Route(String name, RoutePoint... points) {
		this.name = name;
		
		for (RoutePoint p : points) {
			route.add(p);
		}
	}
	
	public List<RoutePoint> getRoute() {
		return route;
	}
	
	public void setRoute(List<RoutePoint> route) {
		this.route = route;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public RoutePoint getNext(RoutePoint rp) {
		RoutePoint next = null;
		for (RoutePoint r : route) {
			if (r.equals(rp)) {
				int i = route.indexOf(r) + 1;
				next = i == route.size() ? route.get(0) : route.get(i) ;
			}
		}
		return next;
	}
	
	public RoutePoint getRoutePoint(String s) {
		for (RoutePoint rp : route) {
			if (rp.getName().equals(s)) {
				return rp;
			}
		}
		return null;
	}
}
