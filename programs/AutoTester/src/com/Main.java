package com;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		/*
//		ch.Application clientHandler = new ch.Application();
//		Thread ch = new Thread(clientHandler);
//		ch.start();
		int clientHandlers = 10;
		for (int i=0; i<clientHandlers; ++i) {
			new Thread(new ch.Application()).start();
		}
		
//		ds.Application discoveryService = new ds.Application();
//		Thread ds = new Thread(discoveryService);
//		ds.start();
		int discoveryServices = 2;
		for (int i=0; i<discoveryServices; ++i) {
			new Thread(new ds.Application()).start();
		}
		
//		ca.Application clientApp = new ca.Application();
//		Thread ca = new Thread(clientApp);
//		ca.start();
		int clients = 1000;
		for (int i=0; i<clients; ++i) {
			new Thread(new ca.Application()).start();
		}

		while (true) {
			Thread.sleep(500);
			System.out.println(System.currentTimeMillis());
		}*/
		
		ac.Application car = new ac.Application(0, 0);
		Thread ac = new Thread(car);
		ac.start();
		
		ach.Application carHandler = new ach.Application();
		Thread ach = new Thread(carHandler);
		ach.start();
		
		while (true) {
			
		}
	}
}
