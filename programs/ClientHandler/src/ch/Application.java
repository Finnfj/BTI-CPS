package ch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;

public class Application extends CPSApplication implements Runnable {
	public final int maxClients = 512; 
	public Map<String, Client> clientMap = new HashMap<>();
	
	public Application() {
		super("ClientHandler", "192.168.2.112");
	}
	
	public static void main(String[] args) throws InterruptedException {
		Application thisClientHandler = new Application();
		thisClientHandler.run();
	}

	public void runSequence() throws InterruptedException {
		String handlingTopic = C.HANDLING_EUROPE_TOPIC;
		
		// Setup Connection
		mq.connect(myName, "simplepw");
		BlockingQueue<String> offerQueue = new LinkedBlockingQueue<>();
		BlockingQueue<String> personalQueue = new LinkedBlockingQueue<>();
		mq.subscribe(handlingTopic, offerQueue);
		mq.subscribe(C.CLIENTHANDLERS_NODE + C.TOPICLIMITER + myName, personalQueue);
		
		// Superloop
		while (true) {
			// Discover a Client
			String[] msg = getNext(offerQueue);
			
			// Do nothing if we handle enough clients already
			if (clientMap.size() >= maxClients) {
				continue;
			}
			
			if (msg[C.I_CMD].equals(C.CMD_INITIALHANDLING)) {
				// offer Handling
				sendMessage(C.DISCOVERYSERVICES_NODE + C.TOPICLIMITER + msg[C.I_ID], C.CMD_OFFERHANDLING, null);
				
				// wait for answer
				msg = getNext(personalQueue);
				
				if (msg[C.I_CMD].equals(C.CMD_PASSCLIENT)) {
					String clientName = msg[C.I_MSG];
					//System.out.println("Handle Client " + clientName);
					clientMap.put(clientName, new Client());
					// other initial stuff
					
					sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + clientName,null, "Du wurdest auserwählt zu connecten");
				} else if (msg[C.I_CMD].equals(C.CMD_BEENHANDLED)) {
					// Client has already been handled
					continue;
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
}
