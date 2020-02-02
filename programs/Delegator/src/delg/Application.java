package delg;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;
import cpsLib.CarStats;
import cpsLib.DatabaseHandler;
import cpsLib.OptimizationInfo;
import cpsLib.Route;
import cpsLib.RoutePoint;
import cpsLib.RouteStats;

public class Application extends CPSApplication implements Runnable {
	private final static int CYCLE_TIME = 10000;
	public final static int KEEPTIME = 60 * 60 * 1000;
	private Map<String, Route> routeMap;
	private DatabaseHandler db;
	private Map<String, RouteStats> routeStats = Collections.synchronizedMap(new HashMap<>());
	private Map<String, CarStats> carStats = Collections.synchronizedMap(new HashMap<>());
	private Map<String, OptimizationInfo> optiInfo = Collections.synchronizedMap(new HashMap<>());
	private boolean initialized = false;

	public Application() {
		super("Delegator", "127.0.0.1");
	}

	public static void main(String[] args)  {
		Application delegator = new Application();
		Thread t = new Thread(delegator);
		t.run();
	}

	private void runSequence() throws InterruptedException {
		if (!initialized) {
			db = new DatabaseHandler();
			routeMap = db.getRoutes();

			// Setup Connection
			mq.connect(myName, "simplepw");

			// Start synchronization Threads
			ClientData cd = new ClientData(this);
			Thread cdThread = new Thread(cd);
			cdThread.start();
			CarData cad = new CarData(this);
			Thread cadThread = new Thread(cad);
			cadThread.start();
			
			// initialized, start Request-Response
			initialized = true;
			new Thread(this).start();

			// collect data and share it every x seconds
			while (true) {
				long timestamp = 0;
				synchronized (routeStats) {
					timestamp = System.currentTimeMillis();
					for (Route r : routeMap.values()) {
						Map<Integer, List<String>> drivingTo = new HashMap<>();
						int thisRouteCars = 0;
						synchronized (carStats) {
							for (CarStats cs : carStats.values()) {
								if (cs.routeID.equals(r.getID())) {
									if (!drivingTo.containsKey(cs.nextRoutePoint)) {
										drivingTo.put(cs.nextRoutePoint, new LinkedList<>());
									}
									drivingTo.get(cs.nextRoutePoint).add(cs.name);
									thisRouteCars++;
								}
							}
							carStats.notifyAll();
						}

						// got lot of data for this route now, decide
						RouteStats thisRouteStats = routeStats.get(r.getID());

						int cost = 0;
						int waiting = 0;
						if (thisRouteStats != null) {
							for (int i = 0; i < thisRouteStats.waitingAt.length; ++i) {
								waiting += thisRouteStats.waitingAt[i];
							}
						} else {
							thisRouteStats = new RouteStats(r);
						}
						cost = C.calcCost(waiting, thisRouteCars);
						synchronized (optiInfo) {
							OptimizationInfo opti = new OptimizationInfo(r.getID(), thisRouteStats.waitingAt,
									thisRouteCars, cost);
							opti.waiting = waiting;

							Optional<String> obj = convertToString(opti);
							if (obj.isPresent()) {
								sendMessage(C.OPTI_NODE, C.CMD_OFFERSTATS, obj.get());
							}

							optiInfo.put(opti.routeID, opti);
							optiInfo.notifyAll();
						}
						
						System.out.println("---------------------------\n" +
								r.getName() + ": WAITING=" + waiting + ", DRIVING=" + thisRouteCars + ", COST=" + cost
								);
						for (RoutePoint rp : r.getRoute()) {
							int i = r.getRoute().indexOf(rp);
							List<String> rpDrivingTo = drivingTo.get(i);
							System.out.println(rp.getName() + ": WAITING=" + thisRouteStats.waitingAt[i]
									+ ", DRIVING_TO=" + (rpDrivingTo != null ? rpDrivingTo.size() : "0"));
						}
						System.out.println("---------------------------");
					}
					routeStats.notifyAll();
				}

				while (System.currentTimeMillis() - timestamp < CYCLE_TIME) {
					Thread.sleep(1000);
				}
			}
		} else {
			// Request-Response
			BlockingQueue<String> requests = new LinkedBlockingQueue<>();
			mq.subscribe(C.OPTI_NODE + C.TOPICLIMITER + C.REQUEST_NODE, requests);
			
			String[] msg = null;
			
			while (true) {
				msg = getNext(requests);
				
				if (msg != null) {
					switch (msg[C.I_CMD]) {
					case C.CMD_CHANGEROUTEREQUEST:
						Optional<OptimizationInfo> o = convertFrom(msg[C.I_MSG]);
						if (o.isPresent()) {
							OptimizationInfo carOpti = o.get();
							
							synchronized (optiInfo) {
								OptimizationInfo correspondingOpti = optiInfo.get(carOpti.routeID);
								if (correspondingOpti != null && correspondingOpti.cost > C.DESIRED_RATIO_MAX) {
									sendMessage(C.VEHICLES_NODE + C.TOPICLIMITER + msg[C.I_ID] + C.TOPICLIMITER + C.OPTI_NODE, C.CMD_CHANGEROUTEALLOW, null);
									correspondingOpti.cost = C.calcCost(correspondingOpti.waiting, correspondingOpti.carsOnRoute+1);
									System.out.println("["+myName+"] " + msg[C.I_ID] + " requests to change route to " + routeMap.get(carOpti.routeID).getName() + " ACCEPTED");
								} else {
									sendMessage(C.VEHICLES_NODE + C.TOPICLIMITER + msg[C.I_ID] + C.TOPICLIMITER + C.OPTI_NODE, C.CMD_CHANGEROUTEDECLINE, null);
									System.out.println("["+myName+"] " + msg[C.I_ID] + " requests to change route to " + routeMap.get(carOpti.routeID).getName() + " DECLINED");
								}
								optiInfo.notifyAll();
							}
						}
						break;
					default:
						break;
					}
				}
			}
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

	public Map<String, Route> getRouteMap() {
		return routeMap;
	}

	public DatabaseHandler getDb() {
		return db;
	}

	public Map<String, RouteStats> getPassengerList() {
		return routeStats;
	}
	
	public Map<String, CarStats> getCarStats() {
		return carStats;
	}
}
