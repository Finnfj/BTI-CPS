package com;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cpsLib.DatabaseHandler;
import cpsLib.Route;
import javafx.util.Pair;

public class Main {
	enum TestType {
		GENERIC,
		SPECIFIC,
		CONTINUOUS
	}
	public static void main(String[] args) throws InterruptedException {
		DatabaseHandler db = new DatabaseHandler();
		Map<String, Route> routeMap = db.getRoutes();
		TestType tt = TestType.CONTINUOUS;
		int testTime = 5 * 60 * 1000;
		int clientHandlers = 5;
		int discoveryServices = 1;
		int Registrys = 1;
		int Delegators = 1;
		int carHandlers = 5;
		int speedfactor = 25;

		// Clients when using GENERIC
		int clients = 10;
		// Clients when using SPECIFIC
		List<Pair<String, Integer>> listPass = new LinkedList<>();
		listPass.add(new Pair<String, Integer>("luebeck-altstadt", 0));
		listPass.add(new Pair<String, Integer>("hamburg-grosserunde", 100));
		
		// Clients when using CONTINUOUS, spawn per minute
		List<Pair<String, Integer>> listPassCon = new LinkedList<>();
		listPassCon.add(new Pair<String, Integer>("luebeck-altstadt", 5));
		listPassCon.add(new Pair<String, Integer>("hamburg-grosserunde", 1));

		// Cars when using GENERIC
		int cars = 20;
		// Cars when using SPECIFIC OR CONTINUOUS
		List<Pair<String, Integer>> listCars = new LinkedList<>();
		listCars.add(new Pair<String, Integer>("luebeck-altstadt", 5));
		listCars.add(new Pair<String, Integer>("hamburg-grosserunde", 20));
		
		long startTime = System.currentTimeMillis();
		switch (tt) {
		case GENERIC:
			for (int i=0; i<clientHandlers; ++i) {
				new Thread(new ch.Application()).start();
			}
			
			for (int i=0; i<discoveryServices; ++i) {
				new Thread(new ds.Application()).start();
			}
			
			for (int i=0; i<Registrys; ++i) {
				new Thread(new reg.Application()).start();
			}
			
			for (int i=0; i<Delegators; ++i) {
				new Thread(new delg.Application()).start();
			}
			
			for (int i=0; i<clients; ++i) {
				new Thread(new ca.Application("Client-" + i)).start();
			}
			
			for (int i=0; i<carHandlers; ++i) {
				new Thread(new ach.Application()).start();
			}
	
			for (int i=0; i<cars; ++i) {
				new Thread(new ac.Application("Car-"+i)).start();
			}
	
			while (true) {
				Thread.sleep(500);
				if (System.currentTimeMillis()-startTime >= testTime)	return;
				//System.out.println(System.currentTimeMillis());
			}
		case SPECIFIC:
			for (int i=0; i<clientHandlers; ++i) {
				new Thread(new ch.Application()).start();
			}
			
			for (int i=0; i<discoveryServices; ++i) {
				new Thread(new ds.Application()).start();
			}
			
			for (int i=0; i<Registrys; ++i) {
				new Thread(new reg.Application()).start();
			}
			
			for (int i=0; i<Delegators; ++i) {
				new Thread(new delg.Application()).start();
			}
			
			int j=0;
			for (Pair<String, Integer> pair : listPass) {
				Route r = routeMap.get(pair.getKey());
				for (int i=j; i<pair.getValue()+j; ++i) {
					new Thread(new ca.Application("Client-" + i, r)).start();
				}
				j += pair.getValue();
			}
			
			for (int i=0; i<carHandlers; ++i) {
				new Thread(new ach.Application()).start();
			}

			j=0;
			for (Pair<String, Integer> pair : listCars) {
				Route r = routeMap.get(pair.getKey());
				for (int i=j; i<pair.getValue()+j; i++) {
					new Thread(new ac.Application("Car-" + i, r)).start();
				}
				j += pair.getValue();
			}
	
			while (true) {
				Thread.sleep(500);
				if (System.currentTimeMillis()-startTime >= testTime)	return;
				//System.out.println(System.currentTimeMillis());
			}
		case CONTINUOUS:
			for (int i=0; i<clientHandlers; ++i) {
				new Thread(new ch.Application()).start();
			}
			
			for (int i=0; i<discoveryServices; ++i) {
				new Thread(new ds.Application()).start();
			}
			
			for (int i=0; i<Registrys; ++i) {
				new Thread(new reg.Application()).start();
			}
			
			for (int i=0; i<Delegators; ++i) {
				new Thread(new delg.Application()).start();
			}
			
			for (int i=0; i<carHandlers; ++i) {
				new Thread(new ach.Application()).start();
			}

			j=0;
			for (Pair<String, Integer> pair : listCars) {
				Route r = routeMap.get(pair.getKey());
				for (int i=j; i<pair.getValue()+j; i++) {
					new Thread(new ac.Application("Car-" + i, r)).start();
				}
				j += pair.getValue();
			}

			j=0;
			while (true) {
				for (Pair<String, Integer> pair : listPassCon) {
					Route r = routeMap.get(pair.getKey());
					for (int i=j; i<pair.getValue()+j; ++i) {
						new Thread(new ca.Application("Client-" + i, r)).start();
					}
					j += pair.getValue();
				}
				Thread.sleep(60000/speedfactor);
				if (System.currentTimeMillis()-startTime >= testTime)	{
					System.out.println("5 Minuten sind rum !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					return;
				}
				//System.out.println(System.currentTimeMillis());
			}
		default:
			break;
		}
	}
}
