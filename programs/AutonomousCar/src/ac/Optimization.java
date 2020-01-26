package ac;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.MQTTWrapper;

public class Optimization implements Runnable {
	ac.Application a;

	public Optimization(Application a) {
		super();
		this.a = a;
	}


	@Override
	public void run() {
		MQTTWrapper mq = a.getMQTT();
		BlockingQueue<String> optimizationNode = new LinkedBlockingQueue<>();
		mq.subscribe(C.VEHICLES_NODE + C.TOPICLIMITER + C.OPTI_NODE, optimizationNode);
		
		while (true) {
			String[] msg = null;
			try {
				msg = a.getNext(optimizationNode);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (msg != null) {
				switch (msg[C.I_CMD]) {
				default:
					break;
				}
			}
		}
	}
}
