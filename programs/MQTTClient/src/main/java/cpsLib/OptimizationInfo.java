package cpsLib;

import java.io.Serializable;

public class OptimizationInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6881470484913471079L;
	public String routeID;
	public long lastChange = 0;
	public int waitingAt[];
	public int waiting;
	public int carsOnRoute;
	public int cost;
	public RoutePoint closestRP = null;
	
	public OptimizationInfo (String routeID, int[] waitingAt, int carsOnRoute, int cost) {
		this.routeID = routeID;
		this.waitingAt = waitingAt;
		this.carsOnRoute = carsOnRoute;
		this.cost = cost;
	}
	
	@Override
	public String toString() {
		String tmp = new String();
		return tmp;
	}
}