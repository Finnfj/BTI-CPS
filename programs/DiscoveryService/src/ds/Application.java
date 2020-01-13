package ds;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;

public class Application extends CPSApplication implements Runnable {
	
	public Application() {
		super("DiscoveryService", "192.168.2.112");
	}

	public static void main(String[] args)  {
		Application discoveryService = new Application();
		Thread t = new Thread(discoveryService);
		t.run();
	}
	
	public void runSequence() throws InterruptedException {
		String handlingTopic = C.HANDLING_EUROPE_TOPIC;
		
		// Setup Connection
		mq.connect(myName, "simplepw");
		BlockingQueue<String> discoveryQueue = new LinkedBlockingQueue<>();
		BlockingQueue<String> personalQueue = new LinkedBlockingQueue<>();
		Queue<String> offerQueue = new LinkedList<>();
		mq.subscribe(C.DISCOVERY_TOPIC, discoveryQueue);
		mq.subscribe(C.DISCOVERYSERVICES_NODE + C.TOPICLIMITER + myName, personalQueue);
		
		// Superloop
		while (true) {
			// Discover a Client
			String[] msg = getNext(discoveryQueue);
			String clientName = msg[C.I_ID];
			
			if (!msg[C.I_CMD].equals(C.CMD_WANTCONNECT)) {
				continue;
			}
			
			while(!offerQueue.isEmpty()) {
				String[] tmp = getNext(offerQueue);
				if (tmp[C.I_MSG].equals(msg[C.I_MSG])) {
					continue;
				}
			}
			
			// Post job for clienthandlers
			sendMessage(handlingTopic, C.CMD_INITIALHANDLING, null);
			
			// Give job to the first clienthandler that replies
			boolean beenHandled = false;
			do {
				msg = getNext(personalQueue);
				
				if (beenHandled) {
					sendMessage(C.CLIENTHANDLERS_NODE + C.TOPICLIMITER + msg[C.I_ID], C.CMD_BEENHANDLED, null);
				} else {
					if (msg[C.I_CMD].equals(C.CMD_OFFERHANDLING)) {
						sendMessage(C.CLIENTHANDLERS_NODE + C.TOPICLIMITER + msg[C.I_ID], C.CMD_PASSCLIENT, clientName);
						beenHandled = true;
					}
				}
			} while (!personalQueue.isEmpty());
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
