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
	private final static double EXCHANGE_RANGE = 50;
	private final static int MAX_PASSENGERS = 7;		// max amoutn of passengers
	private final static int WAITTIME = 5000;
	private Route currentRoute;
	private RoutePoint currentTarget;
	private GPSComponent gps;
	private Resources res;
	private Thread gpsThread;
	private Map<String, Route> routeMap;

	private List<Passenger> passengerList = Collections.synchronizedList(new LinkedList<>());
	private List<Passenger> doneList = Collections.synchronizedList(new LinkedList<>());
	private Boolean passChange = false;
	
	// TODO: Sanity check when passengers are restored
	public Application() {
		super("AutonomousCar", "127.0.0.1");
		res = new Resources(C.RESOURCE_FROM.DB);
		
		routeMap = res.getRouteMap();
		Object[] Keys = routeMap.keySet().toArray();
		
		Random r = new Random();
		
		currentRoute = routeMap.get(Keys[r.nextInt(Keys.length)]);
		currentTarget = currentRoute.getRoute().get(r.nextInt(currentRoute.getRoute().size()));
		
		gps = new GPSComponent(C.GPS_MODE.FAKE, new Position(currentRoute.getRoute().get(r.nextInt(currentRoute.getRoute().size())).getlatVal(), currentRoute.getRoute().get(r.nextInt(currentRoute.getRoute().size())).getlongVal()));
		gps.setTarPos(new Position(currentTarget.getlatVal(), currentTarget.getlongVal()));
		gpsThread = new Thread(gps);
		gpsThread.start();
	}

	public Application(String name) {
		super("AutonomousCar", "127.0.0.1");
		myName = name;
		res = new Resources(C.RESOURCE_FROM.DB);
		
		routeMap = res.getRouteMap();
		Object[] Keys = routeMap.keySet().toArray();
		
		Random r = new Random();
		
		currentRoute = routeMap.get(Keys[r.nextInt(Keys.length)]);
		currentTarget = currentRoute.getRoute().get(r.nextInt(currentRoute.getRoute().size()));
		
		gps = new GPSComponent(C.GPS_MODE.FAKE, new Position(currentRoute.getRoute().get(r.nextInt(currentRoute.getRoute().size())).getlatVal(), currentRoute.getRoute().get(r.nextInt(currentRoute.getRoute().size())).getlongVal()));
		gps.setTarPos(new Position(currentTarget.getlatVal(), currentTarget.getlongVal()));
		gpsThread = new Thread(gps);
		gpsThread.start();
	}

	public Application(String name, Route route) {
		super("AutonomousCar", "127.0.0.1");
		myName = name;
		res = new Resources(C.RESOURCE_FROM.DB);

		routeMap = res.getRouteMap();
		
		Random r = new Random();
		
		currentRoute = route;
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
		new Thread(new Optimization(this)).start();
		// Optimization starten
		long begin;
		RoutePoint startTarget = null;
		long startTime = 0;
		
		// Superloop
		while (true) {
			double distanceLeft = gps.getDistance();
			
			synchronized (passengerList) {
				if (distanceLeft <= DROPOFF_RANGE) {
//					if (startTarget == null) {
//						startTime = System.currentTimeMillis();
//						startTarget = currentTarget;
//					} else if (startTarget == currentTarget) {
//						System.out.println("["+myName+"] Drove a round in " + (System.currentTimeMillis()-startTime)/1000 + " seconds");
//						startTarget = null;
//					}
					RoutePoint currentStation = currentTarget;
					String[] msg;

					// start exchange
					if (passengerList.size() > 0) {
						// Handshake with Passengers that want to get off
						exchangeQueue.clear();
						for (Passenger p : passengerList) {
							sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + p.pasName, C.CMD_STATIONEXCHANGE,
									currentStation.getName());
						}

						// Just a pseudo condition for starting the actual
						// exchange
						while (gps.getDistance() > EXCHANGE_RANGE) {
							Thread.sleep(100);
						}

//						long timeStart = System.currentTimeMillis();
						// Drop off all passengers
						begin = System.currentTimeMillis();
						do {
							msg = getNext(exchangeQueue, 1000);
							if (msg != null) {
								if (msg[C.I_CMD].equals(C.CMD_ACCEPTDROPOFF)) {
									List<Passenger> tmp = new LinkedList<>();
									for (Passenger p : passengerList) {
										if (p.pasName.equals(msg[C.I_ID])) {
											p.state = PassengerState.Arrived;
											p.currCar = myName;
											tmp.add(p);
//											System.out.println("[" + myName + "] " + p.pasName + " drop-off at "
//													+ currentStation.getName());
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
						} while (!exchangeQueue.isEmpty() || System.currentTimeMillis() - begin < WAITTIME);
//						if (System.currentTimeMillis()-timeStart > 6000) System.out.println("Hat ewig gedauert, "+ (System.currentTimeMillis()-timeStart)+"ms");
						// Check for passengers that must get off here and
						// forcefully drop them out
						List<Passenger> tmp = new LinkedList<>();
						for (Passenger p : passengerList) {
							if (p.target.equals(currentStation)) {
								p.state = PassengerState.Arrived;
								tmp.add(p);
								passChange = true;
								System.out.println("Passenger forced dropp-off at " + currentStation.getName());
								sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + p.pasName, C.CMD_FORCEDROPOFF,
										currentStation.getName());
							}
						}
						if (tmp.size() > 0) {
							doneList.addAll(tmp);
							passengerList.removeAll(tmp);
							passChange = true;
						}
					}

					// Get new Passengers on board
					exchangeQueue.clear();
//					System.out.println("["+myName+"] offering exchange at "+ currentStation.getName());
					sendMessage(C.EXCHANGE_NODE + C.TOPICLIMITER + currentStation.getName(), C.CMD_OFFEREXCHANGE,
							currentRoute.getName());

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
										pas.currCar = myName;
										passengerList.add(pas);
										passChange = true;
										sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + pas.pasName,
												C.CMD_EXCHANGESUCCESS, null);
									} else {
										sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + msg[C.I_ID], C.CMD_EXCHANGEFAIL, null);
									}
								} else {
									sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + msg[C.I_ID], C.CMD_EXCHANGEFAIL, null);
								}
							}
						}
					} while (!exchangeQueue.isEmpty() || System.currentTimeMillis() - begin < WAITTIME);

					// Tell all passengers that exchange is over
					for (Passenger p : passengerList) {
						sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + p.pasName, C.CMD_EXCHANGEDONE,
								currentStation.getName());
					}

					// Drive on
					currentTarget = currentRoute.getNext(currentTarget);
					gps.setTarPos(new Position(currentTarget.getlatVal(), currentTarget.getlongVal()));

					// if (passengerList.size() > 0 || doneList.size() > 0) {
//					 System.out.println("["+myName+"] Exchange at Station "+currentStation.getName()+" succesful. Next Target " + currentTarget.getName() + " ("+gps.getDistance()+"m)");
					// for (Passenger p : passengerList) {
					// System.out.println("["+myName+"] Passenger: " +
					// p.pasName);
					// }
					// for (Passenger p : doneList) {
					// System.out.println("["+myName+"] Done: " + p.pasName);
					// }
					// }
				}
				passengerList.notifyAll();
			}
			//System.out.println("["+myName+"] MAIN LOOP TOOK "+ (System.currentTimeMillis()-startTime) + "ms, Remaining " + gps.getDistance()+"m");

			// TODO: get recommendations from delegator and communicate
			// passenger statuses
			Thread.sleep(1000);
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

	public RoutePoint getCurrentTarget() {
		return currentTarget;
	}

	public void setCurrentTarget(RoutePoint currentTarget) {
		this.currentTarget = currentTarget;
	}
	
	public GPSComponent getGps() {
		return gps;
	}
	public List<Passenger> getDoneList() {
		return doneList;
	}
	
	public Map<String, Route> getRouteMap() {
		return routeMap;
	}
	
	public Route getCurrentRoute() {
		return currentRoute;
	}

	public void setCurrentRoute(Route currentRoute) {
		this.currentRoute = currentRoute;
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
}
