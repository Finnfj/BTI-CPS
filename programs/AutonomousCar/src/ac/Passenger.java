package ac;

import java.io.Serializable;

import cpsLib.RoutePoint;

public class Passenger implements Serializable {
	private static final long serialVersionUID = 5L;
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
