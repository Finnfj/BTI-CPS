package cpsLib;

import java.io.Serializable;

public class Passenger implements Serializable {
	public enum PassengerState {
		Disconnected,
		Connected,
		Requested,
		Waiting,
		Seated,
		Arrived,
	}
	
	private static final long serialVersionUID = 5L;
	public PassengerState state;
	public String pasName;
	public String currHandler;
	public String currCar;
	public Route currRoute;
	public RoutePoint target;
	public RoutePoint start;

	public Passenger(String pasName, RoutePoint target, RoutePoint start) {
		super();
		this.pasName = pasName;
		this.target = target;
		this.start = start;
		this.state = PassengerState.Disconnected;
	}
	
	public Passenger(String pasName, RoutePoint target, RoutePoint start, PassengerState state, Route route) {
		super();
		this.pasName = pasName;
		this.target = target;
		this.start = start;
		this.state = state;
		this.currHandler = "AMIGO";
		this.currRoute = route;
		this.currCar = null;
	}
	
	@Override
	public String toString() {
		return "Name: " + pasName 
				+ "\nState: " + state.toString() 
				+ "\nHandler: " + currHandler
				+ "\nRoute: " + currRoute.getName()
				+ "\nStart: " + start.getName()
				+ "\nTarget: " + target.getName();
	}
}
