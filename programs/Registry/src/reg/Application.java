package reg;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import cpsLib.C;
import cpsLib.CPSApplication;
import cpsLib.DatabaseHandler;
import cpsLib.Passenger;
import cpsLib.RouteStats;

public class Application extends CPSApplication implements Runnable {
	private final static int CYCLE_TIME = 10000;

	public Application() {
		super("Registry", "127.0.0.1");
	}

	public static void main(String[] args)  {
		Application registry = new Application();
		Thread t = new Thread(registry);
		t.run();
	}

	private void runSequence() throws InterruptedException {
		List<Passenger> clients = new LinkedList<>();
		List<Passenger> doneClients = new LinkedList<>();
		DatabaseHandler db = new DatabaseHandler();
		List<RouteStats> clientDistribution = new LinkedList<>();

		// Setup Connection
		mq.connect(myName, "simplepw");
		
		while (true) {
			long timestamp = System.currentTimeMillis();
			clientDistribution.clear();
			doneClients.clear();
			clients = db.getClients();
			
			for (Passenger p : clients) {
				switch (p.state) {
				case Requested:
					RouteStats tmp = null;
					for (RouteStats rs : clientDistribution) {
						if (rs.routeID.equals(p.currRoute.getID()))	{
							tmp = rs;
							break;
						}
					}
					if (tmp == null) {
						tmp = new RouteStats(p.currRoute);
						clientDistribution.add(tmp);
					}
					tmp.waitingAt[p.currRoute.getRoute().indexOf(p.start)]++;
					break;
				case Arrived:
					doneClients.add(p);
					db.remClient(p.pasName);
					break;
				default:
					break;
				}
			}
			
			// Send RouteStats one by one to Delegator
			for (RouteStats rs : clientDistribution) {
				System.out.print(rs.toString());
				Optional<String> o = convertToString(rs);
				sendMessage(C.PASSENGERSTATS_NODE, C.CMD_OFFERPASSENGERSTATS, o.get());
			}
			
			// Remove finished Clients to prevent clogging of database
			for (Passenger p : doneClients) {
				System.out.println("Removing " + p.pasName + " from Database");
				db.remClient(p.pasName);
			}
			
			while (System.currentTimeMillis() - timestamp < CYCLE_TIME) {
				Thread.sleep(1000);
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
}
