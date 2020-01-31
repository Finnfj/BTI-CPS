package com;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		int clientHandlers = 5;
		for (int i=0; i<clientHandlers; ++i) {
			new Thread(new ch.Application()).start();
		}
		
		int discoveryServices = 1;
		for (int i=0; i<discoveryServices; ++i) {
			new Thread(new ds.Application()).start();
		}
		
		int Registrys = 1;
		for (int i=0; i<Registrys; ++i) {
			new Thread(new reg.Application()).start();
		}
		
		int clients = 50;
		for (int i=0; i<clients; ++i) {
			new Thread(new ca.Application("Client-" + i)).start();
		}
		
		int carHandlers = 5;
		for (int i=0; i<carHandlers; ++i) {
			new Thread(new ach.Application()).start();
		}

		int cars = 20;
		for (int i=0; i<cars; ++i) {
			new Thread(new ac.Application()).start();
		}

		while (true) {
			Thread.sleep(500);
			//System.out.println(System.currentTimeMillis());
		}
	}
}
