package ac;

import cpsLib.C;
import cpsLib.C.GPS_MODE;
import cpsLib.Position;

import java.lang.Math;
import java.util.Random;
import java.util.concurrent.locks.Lock;

public class GPSComponent implements Runnable {
	private Position myPos;
	private Position tarPos;
	private double speed;
	private double distance;
	private C.GPS_MODE mode;
	private boolean running;
	private boolean newDist = false;
	
	public GPSComponent(GPS_MODE mode, Position p) {
		super();
		this.mode = mode;
		
		if (mode == C.GPS_MODE.FAKE) {
			myPos = p;
			tarPos= new Position(p.latval, p.longval);
		} else if (mode == C.GPS_MODE.REAL) {
			myPos = new Position(0.0, 0.0);
			tarPos = new Position(0.0, 0.0);
		}
		running = true;
	}
	
	public void stop() {
		running = false;
	}

	public double getSpeed() {
		return speed;
	}

	public synchronized double getDistance() {
		return newDist ? distance : distanceOnGeoid(myPos.latval, myPos.longval, tarPos.latval, tarPos.longval);
	}

	public Position getMyPos() {
		return myPos;
	}

	public void setMyPos(Position myPos) {
		synchronized (this) {
			this.myPos = myPos;
			this.notifyAll();
		}
	}

	public synchronized Position getTarPos() {
		return tarPos;
	}

	public void setTarPos(Position tarPos) {
		synchronized (this) {
			this.tarPos = tarPos;
			newDist = false;
			this.notifyAll();
		}
	}

	private void runSequence() throws InterruptedException {
		while (running) {
			if (mode == C.GPS_MODE.FAKE) {
				final double seconds = 0.1;
				double onStart = System.currentTimeMillis();
				synchronized (this) {
					if (((Double)myPos.latval).isNaN() || ((Double)tarPos.latval).isNaN() || ((Double)myPos.longval).isNaN() || ((Double)tarPos.longval).isNaN()) {
						System.out.println("öhm hier ist nan");
						continue;
					}
					double remainingDistance = distanceOnGeoid(myPos.latval, myPos.longval, tarPos.latval, tarPos.longval);
					final int base = 8;
					final int add = 9;
					speed = base + ((new Random()).nextDouble() * add);
					if (remainingDistance < 1000) {
						if (remainingDistance < 250) {
							if (remainingDistance < 100) {
								speed *= 1;
							} else {
								speed *= 10;
							}
						} else {
							speed *= 25;
						}
					} else {
						speed *= 25;
					}
					double distPercentage = remainingDistance == 0.0 ? 0 : speed / remainingDistance;
					myPos.latval += (tarPos.latval - myPos.latval) * distPercentage * seconds;
					myPos.longval += (tarPos.longval - myPos.longval) * distPercentage * seconds;
					distance = distanceOnGeoid(myPos.latval, myPos.longval, tarPos.latval, tarPos.longval);
					newDist = true;

					if (((Double)myPos.latval).isNaN()) {
						System.out.println("öhm hier ist nan");
					}
					//System.out.println("My position: lat=" + myPos.latval + ", long=" + myPos.longval + "\nTarget Position: lat="+tarPos.latval+", long="+tarPos.longval);
					this.notifyAll();
				}
				while ((System.currentTimeMillis() - onStart) < 1000*seconds) {
					Thread.sleep(100);
				}
			} else if (mode == C.GPS_MODE.REAL) {
				// TODO: implement
			}
			
		}
	}
	
	@Override
	public void run() {
		try {
			runSequence();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Author: Kevin Godden
	// URL: https://www.ridgesolutions.ie/index.php/2013/11/14/algorithm-to-calculate-speed-from-two-gps-latitude-and-longitude-points-and-time-difference/
	public static double distanceOnGeoid(double lat1, double lon1, double lat2, double lon2) {
		 
		// Convert degrees to radians
		lat1 = lat1 * Math.PI / 180.0;
		lon1 = lon1 * Math.PI / 180.0;
	 
		lat2 = lat2 * Math.PI / 180.0;
		lon2 = lon2 * Math.PI / 180.0;
		if (((Double)lat1).isNaN() || ((Double)lon1).isNaN() || ((Double)lat2).isNaN() || ((Double)lon2).isNaN()) {
			System.out.println("rechnung kommt auf nan");
		}
	 
		// radius of earth in metres
		double r = 6378100;
	 
		// P
		double rho1 = r * Math.cos(lat1);
		double z1 = r * Math.sin(lat1);
		double x1 = rho1 * Math.cos(lon1);
		double y1 = rho1 * Math.sin(lon1);
		if (((Double)rho1).isNaN() || ((Double)z1).isNaN() || ((Double)x1).isNaN() || ((Double)y1).isNaN()) {
			System.out.println("rechnung kommt auf nan");
		}
	 
		// Q
		double rho2 = r * Math.cos(lat2);
		double z2 = r * Math.sin(lat2);
		double x2 = rho2 * Math.cos(lon2);
		double y2 = rho2 * Math.sin(lon2);

		if (((Double)rho2).isNaN() || ((Double)z2).isNaN() || ((Double)x2).isNaN() || ((Double)y2).isNaN()) {
			System.out.println("rechnung kommt auf nan");
		}
		
		// Dot product
		double dot = (x1 * x2 + y1 * y2 + z1 * z2);
		double cos_theta = dot / (r * r);
		if (((Double)dot).isNaN() || ((Double)cos_theta).isNaN()) {
			System.out.println("rechnung kommt auf nan");
		}
		double theta = Math.acos(Math.min(1.0,cos_theta));
	 
		// Distance in Metres
		if (((Double)r).isNaN() || ((Double)theta).isNaN()) {
			System.out.println("rechnung kommt auf nan");
		}
		return r * theta;
	}
}
