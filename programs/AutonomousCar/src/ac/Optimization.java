package ac;

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
import cpsLib.MQTTWrapper;
import cpsLib.OptimizationInfo;
import cpsLib.Passenger;
import cpsLib.Position;
import cpsLib.Route;
import cpsLib.RoutePoint;

public class Optimization implements Runnable {
	private final static int RECENT_DATA = 5*60*1000;	// Data is recent within 5 mins
	private final static int CYCLE_TIME = 10*1000;		// Time for a cycle is 10 seconds
	private final static double SWITCHROUTE_FREE_DIST = 30*1000;	// within 30km there is no distance penalty for switching
	private final static double ADEQUATE_SWITCH_DIST = 5*1000;	// within 5km the routechange is done
	private final static int PENALTY_PER_KM = 1;
	ac.Application a;

	public Optimization(Application a) {
		super();
		this.a = a;
	}


	@Override
	public void run() {
		MQTTWrapper mq = a.getMQTT();
		BlockingQueue<String> optimizationNode = new LinkedBlockingQueue<>();
		BlockingQueue<String> optiPersonal = new LinkedBlockingQueue<>();
		Map<String, OptimizationInfo> optiMap = new HashMap<>();
		Map<String, Route> routeMap = a.getRouteMap();
		List<Passenger> passengerList = a.getPassengerList();
		GPSComponent gps = a.getGps();
		mq.subscribe(C.OPTI_NODE, optimizationNode);
		mq.subscribe(C.VEHICLES_NODE + C.TOPICLIMITER + a.getName() + C.TOPICLIMITER + C.OPTI_NODE, optiPersonal);
		
		long timestamp = 0;
		
		while (true) {
			String[] msg = null;
			try {
				msg = a.getNext(optimizationNode, 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			
			if (msg != null) {
				switch (msg[C.I_CMD]) {
				case C.CMD_OFFERSTATS:
					Optional<OptimizationInfo> o = CPSApplication.convertFrom(msg[C.I_MSG]);
					if (o.isPresent()) {
						OptimizationInfo opti = o.get();
						opti.lastChange = System.currentTimeMillis();
						optiMap.put(opti.routeID, opti);
					}
					break;
				default:
					break;
				}
			}
			
			if (System.currentTimeMillis()-timestamp > CYCLE_TIME) {
//				long startTime = System.currentTimeMillis();
				synchronized (passengerList) {
					sendCarStats();
					if (passengerList.size() == 0) {
						Route currRoute = a.getCurrentRoute();
						OptimizationInfo currRouteStats = optiMap.get(a.getCurrentRoute().getID());
						
						
						// Don't try to just change routes when there is no data on our own
						if (currRouteStats != null && System.currentTimeMillis()-currRouteStats.lastChange < RECENT_DATA) {
							// wenn wir die kosten unserer strecke nicht zu krass verändern würden
							if (C.calcCost(currRouteStats.waiting, currRouteStats.carsOnRoute-1) < C.DESIRED_RATIO_MAX
									&& currRouteStats.waitingAt[currRoute.getRoute().indexOf(a.getCurrentTarget())] == 0) {
								List<OptimizationInfo> possibilities = new LinkedList<>();
								OptimizationInfo closestPossibility = null;
								for (OptimizationInfo o : optiMap.values()) {
									// Skip our own route
									if (o.routeID.equals(currRoute.getID()))	continue;
									
									Route r = routeMap.get(o.routeID);
									
									// Check if we know of route
									if (r == null) {
										routeMap = new DatabaseHandler().getRoutes();
										r = routeMap.get(o.routeID);
										if (r == null) {
											System.out.println("Route "+o.routeID + " was not found, skip.");
											continue;
										}
									}
									
									// Find nearest Routepoint
									double nearestDistance = Double.MAX_VALUE;
									Position myPos = gps.getMyPos();
									for (RoutePoint R : r.getRoute()) {
										double distToRP = GPSComponent.distanceOnGeoid(R.getlatVal(), R.getlongVal(), myPos.latval, myPos.longval);
										if (distToRP < nearestDistance) {
											o.closestRP = R;
											nearestDistance = distToRP;
										}
									}
									
									// calculate minusCost
									if (o.closestRP == null)	continue;
								
									nearestDistance = Math.max(0, nearestDistance-SWITCHROUTE_FREE_DIST);
									int minusCost = (int) ((nearestDistance/1000) * PENALTY_PER_KM);
									o.cost = o.cost-minusCost;
									
									if (o.cost > C.DESIRED_RATIO_MAX) {
										// route is worth switching to, put it on our list of possibilities
										possibilities.add(o);
									}
								}
								
								while (possibilities.size() > 0) {
									// Find best Possibility
									for (OptimizationInfo o : possibilities) {
										if (closestPossibility == null || o.cost > closestPossibility.cost)	closestPossibility = o;
									}
									
									// If there is a closest Possibility to switch then ask Delegator if we may
									if (closestPossibility != null) {
										Optional<String> obj = CPSApplication.convertToString(closestPossibility);
										if (obj.isPresent()) {
											a.sendMessage(C.OPTI_NODE + C.TOPICLIMITER + C.REQUEST_NODE, C.CMD_CHANGEROUTEREQUEST, obj.get());
										} else {
											timestamp = System.currentTimeMillis();
											passengerList.notifyAll();
											continue;
										}
										
										optiPersonal.clear();
										try {
											msg = a.getNext(optiPersonal, 20000);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
										
										if (msg != null) {
											if (msg[C.I_CMD].equals(C.CMD_CHANGEROUTEALLOW)) {
												// CHANGE ROUTE
												// set new route
												a.setCurrentRoute(routeMap.get(closestPossibility.routeID));
												a.setCurrentTarget(closestPossibility.closestRP);
												gps.setTarPos(new Position(a.getCurrentTarget().getlatVal(), a.getCurrentTarget().getlongVal()));

												System.out.println("["+a.getName() + "] Changing Route to " + a.getCurrentRoute().getName());
												
												sendCarStats();
												
												// loop sleep until distance is adequate
												while (gps.getDistance() > ADEQUATE_SWITCH_DIST) {
													try {
														Thread.sleep(1000);
													} catch (InterruptedException e) {
														e.printStackTrace();
													}
												}
												System.out.println("["+a.getName() + "] Succesfully changed Route");
												break;
											} else {
												possibilities.remove(closestPossibility);
												closestPossibility = null;
											}
										}
									}
								}
							}
						}
					}
					timestamp = System.currentTimeMillis();
					passengerList.notifyAll();
				}
//				System.out.println("["+a.getName()+"] OPTI LOOP TOOK "+ (System.currentTimeMillis()-startTime) + "ms");
			}
		}
	}


	private void sendCarStats() {
		// update carstat on delegator
		CarStats myCarStats = new CarStats(a.getName(), a.getCurrentRoute().getID(), a.getPassengerList().size(), a.getCurrentRoute().getRoute().indexOf(a.getCurrentTarget()));
		Optional<String> carStats = CPSApplication.convertToString(myCarStats);
		a.sendMessage(C.CARSTATS_NODE, C.CMD_OFFERCARSTATS, carStats.get());
	}
}
