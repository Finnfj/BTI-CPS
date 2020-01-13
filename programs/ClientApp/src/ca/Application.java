package ca;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cpsLib.C;
import cpsLib.CPSApplication;

public class Application extends CPSApplication implements Runnable {
	
	public Application() {
		super("Client", "192.168.2.112");
	}

	public static void main(String[] args) throws InterruptedException {
		Application clientApplication = new Application();
		Thread t = new Thread(clientApplication);
		t.run();
	}

	@Override
	public void run() {
		try {
			runSequence();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void runSequence() throws InterruptedException {
		// Setup Connection
		mq.connect(myName, "simplepw");
		BlockingQueue<String> personalQueue = new LinkedBlockingQueue<>();
		mq.subscribe(C.CLIENTS_NODE + C.TOPICLIMITER + myName, personalQueue);
		
		// ask for connection
		sendMessage(C.DISCOVERY_TOPIC, C.CMD_WANTCONNECT, null);
		
		// wait for connection offer
		getNext(personalQueue);
		// go on

		System.out.println("Client " + myName + " succesfully connected.");
		mq.disconnect();
	}
}
