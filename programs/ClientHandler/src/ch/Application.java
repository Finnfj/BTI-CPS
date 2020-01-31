package ch;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;
import cpsLib.DatabaseHandler;
import cpsLib.Passenger;
import cpsLib.Passenger.PassengerState;

public class Application extends CPSApplication implements Runnable {
	public final int maxClients = 512; 
	private boolean initialized;
	private BlockingQueue<String> offerQueue = new LinkedBlockingQueue<>();
	private BlockingQueue<String> personalQueue = new LinkedBlockingQueue<>();
	
	public Application() {
		super("ClientHandler", "broker.hivemq.com");
	}
	
	public static void main(String[] args) throws InterruptedException {
		Application thisClientHandler = new Application();
		thisClientHandler.run();
	}

	public void runSequence() throws InterruptedException {
		String[] msg;
		if (!initialized) {
			String handlingTopic = C.HANDLING_EUROPE_TOPIC;
			
			// Setup Connection
			mq.connect(myName, "simplepw");
			mq.subscribe(handlingTopic, offerQueue);
			mq.subscribe(C.CLIENTHANDLERS_NODE + C.TOPICLIMITER + myName, personalQueue);
			
			initialized = true;
			new Thread(this).start();
			
			// Superloop
			while (true) {
				// Discover Clients
				msg = getNext(offerQueue);
	
				if (msg != null) {
					if (msg[C.I_CMD].equals(C.CMD_INITIALHANDLING)) {
						// offer Handling
						sendMessage(C.DISCOVERYSERVICES_NODE + C.TOPICLIMITER
								+ msg[C.I_ID], C.CMD_OFFERHANDLING, null);
					}
				}
			}
		} else {
			// Superloop
			while (true) {
				DatabaseHandler db = new DatabaseHandler();
				// Personal messages
				msg = getNext(personalQueue);
	
				if (msg != null) {
					switch (msg[C.I_CMD]) {
					case C.CMD_PASSCLIENT:
						sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + msg[C.I_MSG],
								C.CMD_OFFERCONNECT, null);
						break;
					case C.CMD_PASSREQUEST:
						// deserialize passenger
						Passenger pas = null;
	
						Optional<Passenger> inOpt = convertFrom(msg[C.I_MSG]);
						pas = inOpt.get();
	
						if (pas != null) {
							pas.state = PassengerState.Requested;
							db.setClient(pas);
							sendMessage(C.CLIENTS_NODE + C.TOPICLIMITER + msg[C.I_ID],
									C.CMD_GOTREQUEST, null);
						}
						break;
					default:
						break;
					}
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
