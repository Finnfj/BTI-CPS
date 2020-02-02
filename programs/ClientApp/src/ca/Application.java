package ca;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;
import cpsLib.Passenger;
import cpsLib.Passenger.PassengerState;
import cpsLib.Resources;
import cpsLib.Route;
import cpsLib.RoutePoint;

public class Application extends CPSApplication implements Runnable {
	private final static int WAITTIME = 10*1000;
	private Passenger thisPassenger = null;
	private Map<String, Route> routeMap;

	public Application() {
		super("Client", "127.0.0.1");

		Resources res = new Resources(C.RESOURCE_FROM.DB);
		
		Map<String, Route> routeMap = res.getRouteMap();
		Object[] Keys = routeMap.keySet().toArray();
		
		Random r = new Random();
		
		Route route = routeMap.get(Keys[r.nextInt(Keys.length)]);
		RoutePoint target = route.getRoute().get(r.nextInt(route.getRoute().size()));
		RoutePoint start = target;
		while (target.equals(start)) {
			start = route.getRoute().get(r.nextInt(route.getRoute().size()));
		}

		thisPassenger = new Passenger(myName, target, start, route);
	}
	
	public Application(String name) {
		super("Client", "127.0.0.1");
		myName = name;
		
		Resources res = new Resources(C.RESOURCE_FROM.DB);
		
		Map<String, Route> routeMap = res.getRouteMap();
		Object[] Keys = routeMap.keySet().toArray();
		
		Random r = new Random();
		
		Route route = routeMap.get(Keys[r.nextInt(Keys.length)]);
		RoutePoint target = route.getRoute().get(r.nextInt(route.getRoute().size()));
		RoutePoint start = target;
		while (target.equals(start)) {
			start = route.getRoute().get(r.nextInt(route.getRoute().size()));
		}

		thisPassenger = new Passenger(myName, target, start, route);
	}
	
	public Application(String name, RoutePoint start, RoutePoint target, Route route) {
		super("Client", "127.0.0.1");
		myName = name;
		thisPassenger = new Passenger(myName, target, start, route);
	}
	
	public Application(String name, Route route) {
		super("Client", "127.0.0.1");
		myName = name;
		Random r = new Random();
		RoutePoint target = route.getRoute().get(r.nextInt(route.getRoute().size()));
		RoutePoint start = target;
		while (target.equals(start)) {
			start = route.getRoute().get(r.nextInt(route.getRoute().size()));
		}

		thisPassenger = new Passenger(myName, target, start, route);
	}

	public static void main(String[] args) throws InterruptedException {
		Application clientApplication = new Application();
		Thread t = new Thread(clientApplication);
		t.run();
	}

	@Override
	public void run() {
		try {
			runSequence();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void runSequence() throws InterruptedException {
		// Get resources
		Resources r = new Resources(C.RESOURCE_FROM.DB);
		routeMap = r.getRouteMap();
		
		// Choose start and target
		if (thisPassenger.start == null) {
			// ask for start and target in interface
		}
		
		// Setup Connection
		mq.connect(myName, "simplepw");
		BlockingQueue<String> personalQueue = new LinkedBlockingQueue<>();
		mq.subscribe(C.CLIENTS_NODE + C.TOPICLIMITER + myName, personalQueue);
		mq.subscribe(C.EXCHANGE_NODE + C.TOPICLIMITER + thisPassenger.start.getName(), personalQueue);
		
		// ask for connection
		sendMessage(C.DISCOVERY_TOPIC, C.CMD_WANTCONNECT, null);
		long timestamp = 0;
		
		String[] msg;
		while (true) {
			msg = getNext(personalQueue, 1000);
			
			if (msg != null) {
				switch (msg[C.I_CMD]) {
				case C.CMD_OFFERCONNECT:
					if (thisPassenger.state == PassengerState.Disconnected) {
						thisPassenger.currHandler = msg[C.I_ID];
						thisPassenger.state = PassengerState.Connected;
						
						Optional<String> outOpt = convertToString(thisPassenger);
	
						sendMessage(C.CLIENTHANDLERS_NODE + C.TOPICLIMITER + thisPassenger.currHandler,
								C.CMD_PASSREQUEST, outOpt.get());
					}
					break;
				case C.CMD_GOTREQUEST:
					if (thisPassenger.state == PassengerState.Connected) {
						thisPassenger.state = PassengerState.Requested;
					}
					break;
				case C.CMD_OFFEREXCHANGE:
					System.out.println("["+myName+"] got exchange offer from " + msg[C.I_ID] + " and my state is " + thisPassenger.state.toString());
					if (thisPassenger.state == PassengerState.Requested) {
						if (msg[C.I_MSG].equals(thisPassenger.currRoute.getName())) {
							thisPassenger.state = PassengerState.Waiting;
							Optional<String> outOpt = convertToString(thisPassenger);
							timestamp = System.currentTimeMillis();
							
							sendMessage(C.VEHICLES_NODE + C.TOPICLIMITER + msg[C.I_ID] 
									+ C.TOPICLIMITER + C.EXCHANGE_NODE, C.CMD_ACCEPTEXCHANGE, outOpt.get());
						}
					}
					break;
				case C.CMD_EXCHANGESUCCESS:
					if (thisPassenger.state == PassengerState.Waiting) {
						thisPassenger.currCar = msg[C.I_ID];
						thisPassenger.state = PassengerState.Seated;
						mq.unsubscribe(C.EXCHANGE_NODE + C.TOPICLIMITER + thisPassenger.start.getName());
						System.out.println("["+myName+"] got picked up from " + msg[C.I_ID] + " at " + thisPassenger.start.getName());
					}
					break;
				case C.CMD_EXCHANGEFAIL:
					if (thisPassenger.state == PassengerState.Waiting) {
						thisPassenger.state = PassengerState.Requested;
					}
					break;
				case C.CMD_STATIONEXCHANGE:
					if (thisPassenger.state == PassengerState.Seated) {
						if (msg[C.I_MSG].equals(thisPassenger.target.getName())) {
							thisPassenger.state = PassengerState.Arrived;
							sendMessage(C.VEHICLES_NODE + C.TOPICLIMITER + thisPassenger.currCar + C.TOPICLIMITER + C.EXCHANGE_NODE, C.CMD_ACCEPTDROPOFF, null);
						} else {
							sendMessage(C.VEHICLES_NODE + C.TOPICLIMITER + thisPassenger.currCar + C.TOPICLIMITER + C.EXCHANGE_NODE, C.CMD_DECLINEDROPOFF, null);
						}
					}
					break;
				case C.CMD_FORCEDROPOFF:
					thisPassenger.state = PassengerState.Arrived;
					break;
				default:
					break;
				}
			}
			
			if (thisPassenger.state == PassengerState.Arrived) {
				System.out.println(myName + ": Arrived at Station " + thisPassenger.target.getName());
				break;
			}
			
			if (thisPassenger.state == PassengerState.Waiting && System.currentTimeMillis()-timestamp > WAITTIME) {
				System.out.println("["+myName+"] Didn't hear from car we wanted to exchange with, abort");
				thisPassenger.state = PassengerState.Requested;
			}
		}
		mq.disconnect();
	}
}
