package delg;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;
import cpsLib.CarStats;
import cpsLib.MQTTWrapper;

public class CarData implements Runnable {
	private Map<String, CarStats> carStats;
	private BlockingQueue<String> queue;
	private MQTTWrapper mq;
	private delg.Application a;

	public CarData(Application a) {
		this.carStats = a.getCarStats();
		this.mq = a.getMQTT();
		this.a = a;
	}
	@Override
	public void run() {
		mq.subscribe(C.CARSTATS_NODE, queue);
		String[] msg = null;
		
		while (true) {
			try {
				msg = a.getNext(queue);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (msg != null) {
				if (msg[C.I_CMD].equals(C.CMD_OFFERCARSTATS)) {
					// Got new Stats
					Optional<CarStats> o = CPSApplication.convertFrom(msg[C.I_MSG]);
					synchronized (carStats) {
						if (o.isPresent()) {
							CarStats cs = o.get();
							cs.lastChange = System.currentTimeMillis();
							carStats.put(cs.name, cs);
						}
						for (CarStats cs : carStats.values()) {
							if (System.currentTimeMillis()-cs.lastChange > Application.KEEPTIME)	carStats.remove(cs.name);
						}
						carStats.notifyAll();
					}
				}
			}
		}
	}
}
