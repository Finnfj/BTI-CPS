package ac;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;
import cpsLib.Position;
import cpsLib.Resources;
import cpsLib.Route;
import cpsLib.RoutePoint;

public class Application extends CPSApplication implements Runnable {
	private final static double DROPOFF_RANGE = 100;	// Range in which we drop off passengers
	private final static double EXCHANGE_RANGE = 10;
	private final static int MAX_PASSENGERS = 7;		// max amoutn of passengers
	private Route currentRoute;
	private RoutePoint currentTarget;
	private Resources res;
	private GPSComponent gps;
	private Thread gpsThread;
	private List<Passenger> passengerList = new LinkedList<>();

	public Application(double la, double lo) {
		super("AutonomousCar", "127.0.0.1");
		res = new Resources(C.RESOURCE_FROM.FILE);
		gps = new GPSComponent(C.GPS_MODE.FAKE, new Position(la, lo));
		currentRoute = res.getRouteMap().get("HAMBURG-HAFENRUNDE");
		currentTarget = currentRoute.getRoute().get((new Random()).nextInt(currentRoute.getRoute().size()));
		gps.setTarPos(new Position(currentTarget.getlatVal(), currentTarget.getlongVal()));
		gpsThread = new Thread(gps);
		gpsThread.start();
	}
	
	public Application() {
		super("AutonomousCar", "192.168.2.112");
//		res = new Resources(C.RESOURCE_FROM.FILE);
//		gps = new GPSComponent(C.GPS_MODE.REAL, null);
//		currentRoute = res.getRouteMap().get("HAMBURG-HAFENRUNDE");
	}
	
	public static void main(String[] args) {
		Application autonomousCar = new Application();
		Thread t = new Thread(autonomousCar);
		t.run();
	}
	
	public void runSequence() throws InterruptedException {
		// Setup Connection
		mq.connect(myName, "simplepw");
		BlockingQueue<String> personalQueue = new LinkedBlockingQueue<>();
		mq.subscribe(C.VEHICLES_NODE + C.TOPICLIMITER + myName, personalQueue);
		BlockingQueue<String> exchangeQueue = new LinkedBlockingQueue<>();
		mq.subscribe(C.VEHICLES_NODE + C.TOPICLIMITER + myName + C.TOPICLIMITER + C.EXCHANGE_NODE, exchangeQueue);
		
		// Superloop
		while (true) {
			double distanceLeft = gps.getDistance();
			
			if (distanceLeft <= DROPOFF_RANGE) {
				RoutePoint currentStation = currentTarget;
				String[] msg;
				
				// start exchange
				if (passengerList.size() > 0) {
					// Handshake with Passengers that want to get off
					for (Passenger p : passengerList) {
						sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + p.pasName, C.CMD_STATIONEXCHANGE, currentStation.getName());
					}
					
					// Just a pseudo condition for starting the actual exchange
					while (gps.getDistance() > EXCHANGE_RANGE) {
						Thread.sleep(1000);
					}
					
					// Drop off all passengers
					while (!exchangeQueue.isEmpty()) {
						msg = getNext(exchangeQueue);
						if (msg[C.I_CMD].equals(C.CMD_ACCEPTDROPOFF)) {
							for (Passenger p : passengerList) {
								if (p.pasName.equals(msg[C.I_ID])) {
									passengerList.remove(p);
								}
							}
						} else if (msg[C.I_CMD].equals(C.CMD_DECLINEDROPOFF)) {
							// do nothing
						}
					}
					
					// Check for passengers that must get off here and forcefully drop them out
					for (Passenger p : passengerList) {
						if (p.target.equals(currentStation)) {
							passengerList.remove(p);
							sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + p.pasName, C.CMD_FORCEDROPOFF, currentStation.getName());
						}
					}
				}
				
				// Get new Passengers on board
				sendMessage(C.EXCHANGE_NODE + C.TOPICLIMITER + currentStation.getName(), C.CMD_OFFEREXCHANGE, currentRoute.getName());
				
				// Accept new Passengers
				do {
					msg = getNext(exchangeQueue, 5000);
					if (msg[C.I_CMD].equals(C.CMD_ACCEPTEXCHANGE)) {
						if (passengerList.size() < MAX_PASSENGERS) {
							Passenger p = new Passenger(msg[C.I_ID], currentRoute.getRoutePoint(msg[C.I_MSG]), currentStation);
							passengerList.add(p);
							sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + p.pasName, C.CMD_EXCHANGESUCCESS, null);
						} else {
							sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + msg[C.I_ID], C.CMD_EXCHANGEFAIL, null);
						}
					}
				} while (exchangeQueue.isEmpty());
				
				
				// Drive on
				currentTarget = currentRoute.getNext(currentTarget);
				gps.setTarPos(new Position(currentTarget.getlatVal(), currentTarget.getlongVal()));
				
				// Tell all passengers that exchange is over
				for (Passenger p : passengerList) {
					sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + p.pasName, C.CMD_EXCHANGEDONE, currentStation.getName());
				}
				
				System.out.println("["+myName+"] Exchange at Station "+currentStation.getName()+" succesful.");
			}
			
			// TODO: get recommendations from delegator and communicate passenger statuses
			Thread.sleep(5000);
		}
	}

	@Override
	public void run() {
		try {
			runSequence();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
