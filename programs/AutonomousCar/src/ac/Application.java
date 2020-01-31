package ac;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;
import cpsLib.Passenger;
import cpsLib.Position;
import cpsLib.Resources;
import cpsLib.Route;
import cpsLib.RoutePoint;
import cpsLib.Passenger.PassengerState;

public class Application extends CPSApplication implements Runnable {
	private final static double DROPOFF_RANGE = 100;	// Range in which we drop off passengers
	private final static double EXCHANGE_RANGE = 10;
	private final static int MAX_PASSENGERS = 7;		// max amoutn of passengers
	private final static int WAITTIME = 5000;
	private Route currentRoute;
	private RoutePoint currentTarget;
	private Resources res;
	private GPSComponent gps;
	private Thread gpsThread;
	private List<Passenger> passengerList = Collections.synchronizedList(new LinkedList<>());
	private List<Passenger> doneList = Collections.synchronizedList(new LinkedList<>());
	private Boolean passChange = false;
	
	// TODO: Sanity check when passengers are restored
	
	public List<Passenger> getDoneList() {
		return doneList;
	}

	public Boolean getPassChange() {
		if (passChange) {
			passChange = false;
			return true;
		} else {
			return false;
		}
	}

	public List<Passenger> getPassengerList() {
		return passengerList;
	}

	public Application() {
		super("AutonomousCar", "127.0.0.1");
		res = new Resources(C.RESOURCE_FROM.DB);
		
		Map<String, Route> routeMap = res.getRouteMap();
		Object[] Keys = routeMap.keySet().toArray();
		
		Random r = new Random();
		
		currentRoute = routeMap.get(Keys[r.nextInt(Keys.length)]);
		currentTarget = currentRoute.getRoute().get(r.nextInt(currentRoute.getRoute().size()));
		
		gps = new GPSComponent(C.GPS_MODE.FAKE, new Position(currentRoute.getRoute().get(r.nextInt(currentRoute.getRoute().size())).getlatVal(), currentRoute.getRoute().get(r.nextInt(currentRoute.getRoute().size())).getlongVal()));
		gps.setTarPos(new Position(currentTarget.getlatVal(), currentTarget.getlongVal()));
		gpsThread = new Thread(gps);
		gpsThread.start();
	}

	public static void main(String[] args) {
		Application autonomousCar = new Application();
		Thread t = new Thread(autonomousCar);
		t.run();
	}
	
	public void runSequence() throws InterruptedException {
		// Setup Connection
		mq.connect(myName, "simplepw");
		BlockingQueue<String> exchangeQueue = new LinkedBlockingQueue<>();
		mq.subscribe(C.VEHICLES_NODE + C.TOPICLIMITER + myName + C.TOPICLIMITER + C.EXCHANGE_NODE, exchangeQueue);
		new Thread(new Synchronization(this)).start();
		long begin;
		
		// Superloop
		while (true) {
			double distanceLeft = gps.getDistance();
			
			if (distanceLeft <= DROPOFF_RANGE) {
				RoutePoint currentStation = currentTarget;
				String[] msg;
				
				// start exchange
				synchronized (passengerList) {
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
						begin = System.currentTimeMillis();
						do {
							msg = getNext(exchangeQueue, 5000);
							if (msg != null) {
								if (msg[C.I_CMD].equals(C.CMD_ACCEPTDROPOFF)) {
									List<Passenger> tmp = new LinkedList<>();
									for (Passenger p : passengerList) {
										if (p.pasName.equals(msg[C.I_ID])) {
											p.state = PassengerState.Arrived;
											tmp.add(p);
											System.out.println(p.pasName + " drop-off at " + currentStation.getName());
										}
									}
									if (tmp.size() > 0) {
										doneList.addAll(tmp);
										passengerList.removeAll(tmp);
										passChange = true;
									}
								} else if (msg[C.I_CMD].equals(C.CMD_DECLINEDROPOFF)) {
									// do nothing
								}
							}
						} while (!exchangeQueue.isEmpty() || System.currentTimeMillis()-begin < WAITTIME);
						
						// Check for passengers that must get off here and forcefully drop them out
						List<Passenger> tmp = new LinkedList<>();
						for (Passenger p : passengerList) {
							if (p.target.equals(currentStation)) {
								p.state = PassengerState.Arrived;
								tmp.add(p);
								passChange = true;
								System.out.println("Passenger forced dropp-off at " + currentStation.getName());
								sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + p.pasName, C.CMD_FORCEDROPOFF, currentStation.getName());
							}
						}
						if (tmp.size() > 0) {
							doneList.addAll(tmp);
							passengerList.removeAll(tmp);
							passChange = true;
						}
					}
					
					// Get new Passengers on board
					sendMessage(C.EXCHANGE_NODE + C.TOPICLIMITER + currentStation.getName(), C.CMD_OFFEREXCHANGE, currentRoute.getName());
					
					// Accept new Passengers
					begin = System.currentTimeMillis();
					do {
						msg = getNext(exchangeQueue, 1000);
						if (msg != null) {
							if (msg[C.I_CMD].equals(C.CMD_ACCEPTEXCHANGE)) {
								if (passengerList.size() < MAX_PASSENGERS) {
									// deserialize passenger

									Optional<Passenger> inOpt = convertFrom(msg[C.I_MSG]);
									Passenger pas = inOpt.get();
									
									if (pas != null) {
										pas.state = PassengerState.Seated;
										passengerList.add(pas);
										passChange = true;
										sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + pas.pasName, C.CMD_EXCHANGESUCCESS, null);
									} else {
										sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + msg[C.I_ID], C.CMD_EXCHANGEFAIL, null);
									}
								} else {
									sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + msg[C.I_ID], C.CMD_EXCHANGEFAIL, null);
								}
							}
						}
					} while (!exchangeQueue.isEmpty() || System.currentTimeMillis()-begin < WAITTIME);
					
					// Tell all passengers that exchange is over
					for (Passenger p : passengerList) {
						sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + p.pasName, C.CMD_EXCHANGEDONE, currentStation.getName());
					}
					
					if (passengerList.size() > 0 || doneList.size() > 0) {
						System.out.println("["+myName+"] Exchange at Station "+currentStation.getName()+" succesful.");
						for (Passenger p : passengerList) {
							System.out.println("["+myName+"] Passenger: " + p.pasName);
						}
						for (Passenger p : doneList) {
							System.out.println("["+myName+"] Done: " + p.pasName);
						}
					}
					passengerList.notifyAll();
				}
				
				// Drive on
				currentTarget = currentRoute.getNext(currentTarget);
				gps.setTarPos(new Position(currentTarget.getlatVal(), currentTarget.getlongVal()));
				
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
