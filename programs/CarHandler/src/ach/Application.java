package ach;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;

public class Application extends CPSApplication implements Runnable {

	public Application() {
		super("ClientHandler", "192.168.2.112");
	}

	@Override
	public void run() {
		try {
			runSequence();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		Application thisCarHandler = new Application();
		thisCarHandler.run();
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
			
			if (msg[C.I_CMD].equals(C.CMD_INITIALHANDLING)) {
				// offer Handling
				sendMessage(C.DISCOVERYSERVICES_NODE + C.TOPICLIMITER + msg[C.I_ID], C.CMD_OFFERHANDLING, null);
				
				// wait for answer
				msg = getNext(personalQueue);
				
				if (msg[C.I_CMD].equals(C.CMD_PASSCLIENT)) {
					String clientName = msg[C.I_MSG];
					
					sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + clientName,null, "Du wurdest auserwählt zu connecten");
				} else if (msg[C.I_CMD].equals(C.CMD_BEENHANDLED)) {
					// Client has already been handled
					continue;
				}
			}
		}
	}
}
