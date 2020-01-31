package cpsLib;

import java.io.Serializable;

public class CarStats implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3456274283989025248L;
	public String name;
	public String routeID;
	public int Passengers;
	public int nextRoutePoint;
	public long lastChange;
	
	public CarStats(String name, String routeID, int passengers, String nextRoutePoint) {
		super();
		this.routeID = routeID;
		Passengers = passengers;
		this.nextRoutePoint = nextRoutePoint;
	}
}
