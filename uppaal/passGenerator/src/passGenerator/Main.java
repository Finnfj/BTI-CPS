package passGenerator;

import java.util.Random;

public class Main {
	public static void main(String[] args) {
		Random zufall = new Random();
		int noPass = 10;

		System.out.println("System declaration");
		for (int i=0; i<noPass; i++) {
			System.out.println("Pass" + i + " = Passenger_s(" + i + "," + zufall.nextInt(5) + "," + zufall.nextInt(5) + "," + zufall.nextInt(100) + ");");
		}
		System.out.print("system Taxi1,Serv1,Serv2");
		for (int i=0; i<noPass; i++) {
			System.out.print(",Pass" + i);
		}
		System.out.print(";\n\n");

		System.out.println("Ziel 1");
		System.out.print("A[] not (Pass0.WaitTimeout");
		for (int i=1; i<noPass; i++) {
			System.out.print(" or ");
			System.out.print("Pass" + i + ".WaitTimeout");
		}
		System.out.print(")\n\n");

		System.out.println("Ziel 2");
		System.out.print("A[] not (Pass0.SeatedTimeout");
		for (int i=1; i<noPass; i++) {
			System.out.print(" or ");
			System.out.print("Pass" + i + ".SeatedTimeout");
		}
		System.out.print(")\n\n");

		System.out.println("Ziel 5");
		System.out.print("A[] (seats[1] < 7) imply not (Pass0.SeatsFull");
		for (int i=1; i<noPass; i++) {
			System.out.print(" or ");
			System.out.print("Pass" + i + ".SeatsFull");
		}
		System.out.print(")\n\n");
		
		System.out.println("Ziel 4");
		System.out.print("A[] (taxiRounds > 4 and taxiRounds < 6) imply (Pass0.Arrival");
		for (int i=1; i<noPass; i++) {
			System.out.print(" and ");
			System.out.print("Pass" + i + ".Arrival");
		}
		System.out.print(")\n");
	}

}
