package cpsLib;

import java.io.Serializable;

public class RouteStats implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2768678713828764731L;
	public String routeID;
	public int[] waitingAt;
	public long lastChange = 0;
	
	public RouteStats(Route route) {
		this.routeID = route.getID();
		this.waitingAt = new int[route.getRoute().size()];
	}
	
	@Override
	public String toString() {
		String tmp = "Routestats for " + routeID + ":\n";
		for (int i=0; i<waitingAt.length; ++i) {
			tmp += i + "=" + waitingAt[i] + "\n";
		}
		return tmp;
	}
}
