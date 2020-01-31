package ach;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;
import cpsLib.Passenger;
import cpsLib.DatabaseHandler;

public class Application extends CPSApplication implements Runnable {
	private boolean initialized;
	public final int maxCars = 512; 
	public Map<String, AutonomousVehicle> carMap = new HashMap<>();
	private BlockingQueue<String> handleQueue;
	private BlockingQueue<String> personalQueue;
	private DatabaseHandler db;

	public Application() {
		super("CarHandler", "192.168.2.112");
		this.initialized = false;
		this.db = new DatabaseHandler();
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

	@SuppressWarnings("unchecked")
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
			new Thread(this).start();
			
			// Superloop
			System.out.println("Listening for Cars");
			while (true) {
				// Discover a Car
				String[] msg = getNext(handleQueue);
				
				if (msg[C.I_CMD].equals(C.CMD_CARINITIAL)) {
					if (carMap.size() < maxCars) {
						// offer Handling
						sendMessage(C.VEHICLES_NODE + C.TOPICLIMITER + msg[C.I_ID] + C.TOPICLIMITER + C.SYNCH_NODE, C.CMD_CAROFFER, null);
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

					Optional<Passenger> inOpt = convertFrom(msg[C.I_MSG]);
					
					if (inOpt.isPresent()) {
						List<Passenger> pasList = (List<Passenger>) inOpt.get();
						for (Passenger p : pasList) {
							db.setClient(p);
						}
					}
					
					sendMessage(C.VEHICLES_NODE + C.TOPICLIMITER + msg[C.I_ID] + C.TOPICLIMITER + C.SYNCH_NODE,C.CMD_CARRECEIVED, "Ich habe deine Daten gefressen");
					break;
				default:
					break;
				}
			}
		}
	}
}
