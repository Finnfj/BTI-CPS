package ac;

import cpsLib.RoutePoint;

public class Passenger {
	public String pasName;
	public RoutePoint target;
	public RoutePoint start;
	public Passenger(String pasName, RoutePoint target, RoutePoint start) {
		super();
		this.pasName = pasName;
		this.target = target;
		this.start = start;
	}
}
