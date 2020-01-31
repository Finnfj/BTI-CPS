package delg;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cpsLib.CPSApplication;
import cpsLib.CarStats;
import cpsLib.DatabaseHandler;
import cpsLib.Route;
import cpsLib.RouteStats;

public class Application extends CPSApplication implements Runnable {
	public final static int KEEPTIME = 60 * 60 * 1000;
	private Map<String, Route> routeMap;
	private DatabaseHandler db;
	private Map<String, RouteStats> routeStats = Collections.synchronizedMap(new HashMap<>());
	private Map<String, CarStats> carStats = Collections.synchronizedMap(new HashMap<>());

	public Application() {
		super("Delegator", "127.0.0.1");
	}

	public static void main(String[] args)  {
		Application delegator = new Application();
		Thread t = new Thread(delegator);
		t.run();
	}

	private void runSequence() throws InterruptedException {
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
		
		while (true) {
			synchronized (routeStats) {
				for (RouteStats rs : routeStats.values()) {
					Map<Integer, List<String>> drivingTo = new HashMap<>();
					synchronized (carStats) {
						for (CarStats cs : carStats.values()) {
							if (cs.routeID.equals(rs.routeID)) {
								if (!drivingTo.containsKey(cs.nextRoutePoint)) {
									drivingTo.put(cs.nextRoutePoint, new LinkedList<>());
								}
								drivingTo.get(cs.nextRoutePoint).add(cs.name);
							}
						}
						carStats.notifyAll();
					}
					// got lot of data for this route now, decide 
				}
				routeStats.notifyAll();
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
