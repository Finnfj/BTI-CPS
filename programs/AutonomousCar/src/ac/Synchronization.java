package ac;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;
import cpsLib.MQTTWrapper;
import cpsLib.Passenger;

public class Synchronization implements Runnable {
	enum connectionState {
		Disconnected,
		Connected
	};
	
	enum synchState {
		Sent,
		Failed,
		Idle
	};

	private final static int TIMEOUT = 1000;
	private final static int RESPONSE_TIMEOUT = 10000;
	private String filePasName;
	private String fileDoneName;
	private MQTTWrapper mq;
	private BlockingQueue<String> personalQueue;
	private List<Passenger> sentDone = new LinkedList<>();
	private connectionState conn;
	private synchState synch;
	private String currentHandler;
	private long lastSynch = 0;
	private ac.Application a;
	
	
	public Synchronization(ac.Application a) {
		super();
		this.a = a;
		mq = a.getMQTT();
		filePasName = a.getName() + "-Passengers.txt";
		fileDoneName = a.getName() + "-Done.txt";
		personalQueue = new LinkedBlockingQueue<>();
		conn = connectionState.Disconnected;
		synch = synchState.Idle;
	}


	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		mq.subscribe(C.VEHICLES_NODE + C.TOPICLIMITER + a.getName() + C.TOPICLIMITER + C.SYNCH_NODE, personalQueue);
		List<Passenger> passengers = a.getPassengerList();
		List<Passenger> done = a.getDoneList();
		Boolean needSynch = false;

		/*
		synchronized (passengers) {
			try {
				// Read done Passengers
		        FileInputStream fileIn = new FileInputStream(fileDoneName); 
		        ObjectInputStream objectIn = new ObjectInputStream(fileIn); 
		        try {  done.addAll((List<Passenger>) objectIn.readObject());
		        } catch (Exception o) {}
		        objectIn.close();
		        fileIn.close();
		        // Read current Passengers
		    	fileIn = new FileInputStream(filePasName); 
		    	objectIn = new ObjectInputStream(fileIn);
		        try {  passengers.addAll((List<Passenger>) objectIn.readObject());
		        } catch (Exception o) {}
		        objectIn.close();
		        fileIn.close();
			} catch (Exception e) {  }
		}
		*/
		System.out.println("Synchronization started");
		while (true) {
			if (conn == connectionState.Disconnected) {
				a.sendMessage(C.CARHANDLING_TOPIC, C.CMD_CARINITIAL, null);
			} else if (conn == connectionState.Connected) {
				if (a.getPassChange() || needSynch) {
//					long startTime = System.currentTimeMillis();
					synchronized (passengers) {
						// Save file
						try {
			            	// Save current Passengers
							FileOutputStream file = new FileOutputStream(filePasName);
							ObjectOutputStream out = new ObjectOutputStream(file);
							out.writeObject(passengers);
							out.close();
							file.close();
							// Save current done 
							file = new FileOutputStream(fileDoneName);
							out = new ObjectOutputStream(file);
							out.writeObject(done);
							out.close();
							file.close();
						} catch (Exception e) {
							System.out.println("Writing to file failed!");
							e.printStackTrace();
						}
	
						if (synch == synchState.Idle) {
							// Send Data
							try {
								List<Passenger> allPass = new LinkedList<>();
								allPass.addAll(passengers);
								allPass.addAll(done);
								Optional<String> outOpt = CPSApplication.convertToString((Serializable) allPass);
								
								a.sendMessage(C.CARHANDLERS_NODE + C.TOPICLIMITER + currentHandler, C.CMD_CARDATA, outOpt.get());
								sentDone.addAll(done);
								synch = synchState.Sent;
								lastSynch = System.currentTimeMillis();
								needSynch = false;
								//System.out.println("Synching Data...");
							} catch (Exception e) {
								System.out.println("Sending failed!");
								e.printStackTrace();
								conn = connectionState.Disconnected;
								needSynch = true;
							}
						} else if (synch == synchState.Sent) {
							// Try to synch asap after current Data was received or timed out
							needSynch = true;
						}
						passengers.notifyAll();
					}
//					System.out.println("["+a.getName()+"] SYNCH LOOP TOOK "+ (System.currentTimeMillis()-startTime) + "ms");
				}
			}
			
			// Get Messages
			String[] msg = null;
			try {
				msg = a.getNext(personalQueue, TIMEOUT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Process message
			if (msg != null) {
				switch (msg[C.I_CMD]) {
				case C.CMD_CAROFFER:
					if (conn == connectionState.Disconnected) {
						conn = connectionState.Connected;
						currentHandler = msg[C.I_ID];
					}
					break;
				case C.CMD_CARRECEIVED:
					if (msg[C.I_ID].equals(currentHandler)) {
						synchronized (passengers) {
							done.removeAll(sentDone);
							sentDone.clear();
							passengers.notifyAll();
						}
						synch = synchState.Idle;
						//System.out.println("Data synchronized.");
					}
				default:
					break;
				}
			}
			
			// check for Timeout
			if (conn == connectionState.Connected 
					&& synch == synchState.Sent 
					&& System.currentTimeMillis()-lastSynch > RESPONSE_TIMEOUT) {
				conn = connectionState.Disconnected;
				synch = synchState.Idle;
				needSynch = true;
			}
			
		}
	}
}