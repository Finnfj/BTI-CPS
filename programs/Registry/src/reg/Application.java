package reg;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.opencsv.CSVWriter;

import cpsLib.C;
import cpsLib.CPSApplication;
import cpsLib.DatabaseHandler;
import cpsLib.Passenger;
import cpsLib.Passenger.PassengerState;
import cpsLib.Route;
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

	private void runSequence() throws InterruptedException, IOException {
		List<Passenger> clients = new LinkedList<>();
		List<Passenger> doneClients = new LinkedList<>();
		DatabaseHandler db = new DatabaseHandler();
		List<RouteStats> clientDistribution = new LinkedList<>();
		Map<String, Route> routeMap = db.getRoutes();
		for (Route r : routeMap.values()) {
			clientDistribution.add(new RouteStats(r));
		}

		// Setup Connection
		mq.connect(myName, "simplepw");
		
		while (true) {
			long timestamp = System.currentTimeMillis();
			for (RouteStats rs : clientDistribution) {
				rs.reset();
			}
			doneClients.clear();
			clients = db.getClientsWithTimestamps();

			String csv = "Passengers.csv";
			CSVWriter writer = new CSVWriter(new FileWriter(csv));
			List<String[]> data = new ArrayList<String[]>();
			for (Passenger p : clients) {
				data.add(new String[] { p.pasName, p.state.toString(), p.currCar, p.currRoute.getName(), p.start.getName(), p.target.getName(), String.valueOf(p.tstamps[0]), String.valueOf(p.tstamps[1]), String.valueOf(p.tstamps[2]) });
			}

			writer.writeAll(data);
			writer.close();
			
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
					//doneClients.add(p);
					//db.remClient(p.pasName);
					break;
				default:
					break;
				}
			}
			
			// Send RouteStats one by one to Delegator
			for (RouteStats rs : clientDistribution) {
				//System.out.print(rs.toString());
				Optional<String> o = convertToString(rs);
				sendMessage(C.PASSENGERSTATS_NODE, C.CMD_OFFERPASSENGERSTATS, o.get());
			}
			
			// Remove finished Clients to prevent clogging of database
//			for (Passenger p : doneClients) {
//				System.out.println("Removing " + p.pasName + " from Database");
//				db.remClient(p.pasName);
//			}
			
			while (System.currentTimeMillis() - timestamp < CYCLE_TIME) {
				Thread.sleep(1000);
			}
		}
	}

	@Override
	public void run() {
		try {
			runSequence();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
