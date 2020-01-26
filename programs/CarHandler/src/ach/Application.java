package ach;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;

public class Application extends CPSApplication implements Runnable {
	boolean initialized;
	public final int maxCars = 512; 
	public Map<String, AutonomousVehicle> carMap = new HashMap<>();
	BlockingQueue<String> handleQueue;
	BlockingQueue<String> personalQueue;

	public Application() {
		super("CarHandler", "192.168.2.112");
		this.initialized = false;
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
		if (!initialized) {
			String handlingTopic = C.CARHANDLING_TOPIC;
			
			// Setup Connection
			mq.connect(myName, "simplepw");
			handleQueue = new LinkedBlockingQueue<>();
			personalQueue = new LinkedBlockingQueue<>();
			mq.subscribe(handlingTopic, handleQueue);
			mq.subscribe(C.CARHANDLERS_NODE + C.TOPICLIMITER + myName, personalQueue);
			
			initialized = true;
			new Thread(this).start();;
			
			// Superloop
			System.out.println("Listening for Cars");
			while (true) {
				// Discover a Car
				String[] msg = getNext(handleQueue);
				
				if (msg[C.I_CMD].equals(C.CMD_CARINITIAL)) {
					if (carMap.size() < maxCars) {
						// offer Handling
						sendMessage(C.VEHICLES_NODE + C.TOPICLIMITER + msg[C.I_ID], C.CMD_CAROFFER, null);
					}
				}
			}
		} else {
			System.out.println("Listening for personal messages");
			while (true) {
				// wait for answer
				String[] msg = getNext(personalQueue);
				
				switch(msg[C.I_CMD]) {
				case C.CMD_CARDATA:
					String Data = msg[C.I_MSG];
					System.out.println("Ich habe Daten gefressen: " + Data);
					sendMessage(C.VEHICLES_NODE + C.TOPICLIMITER + msg[C.I_ID],null, "Ich habe deine Daten gefressen");
					break;
				default:
					break;
				}
			}
		}
	}
}
