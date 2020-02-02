package delg;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.MQTTWrapper;
import cpsLib.RouteStats;
import cpsLib.CPSApplication;
import cpsLib.CarStats;

public class ClientData implements Runnable {
	private Map<String, RouteStats> routeStats;
	private BlockingQueue<String> queue = new LinkedBlockingQueue<>();
	private MQTTWrapper mq;
	private delg.Application a;

	public ClientData(Application a) {
		this.routeStats = a.getPassengerList();
		this.mq = a.getMQTT();
		this.a = a;
	}
	@Override
	public void run() {
		mq.subscribe(C.PASSENGERSTATS_NODE, queue);
		String[] msg = null;
		
		while (true) {
			try {
				msg = a.getNext(queue);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (msg != null) {
				if (msg[C.I_CMD].equals(C.CMD_OFFERPASSENGERSTATS)) {
					// Got new Stats
					Optional<RouteStats> o = CPSApplication.convertFrom(msg[C.I_MSG]);
					synchronized (routeStats) {
						if (o.isPresent()) {
							RouteStats rs = o.get();
							rs.lastChange = System.currentTimeMillis();
							routeStats.put(rs.routeID, rs);
						}
						for (RouteStats cs : routeStats.values()) {
							if (System.currentTimeMillis()-cs.lastChange > Application.KEEPTIME)	routeStats.remove(cs.routeID);
						}
						routeStats.notifyAll();
					}
				}
			}
		}
	}
}
