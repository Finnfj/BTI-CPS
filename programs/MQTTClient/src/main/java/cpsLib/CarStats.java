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
	
	public CarStats(String name, String routeID, int passengers, int nextRoutePoint) {
		super();
		this.name = name;
		this.routeID = routeID;
		this.Passengers = passengers;
		this.nextRoutePoint = nextRoutePoint;
	}
}
