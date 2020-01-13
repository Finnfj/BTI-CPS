package ac;

import cpsLib.C;
import cpsLib.C.GPS_MODE;
import cpsLib.Position;

import java.lang.Math;
import java.util.Random;

public class GPSComponent implements Runnable {
	private Position myPos;
	private Position tarPos;
	private double speed;
	private double distance;
	private C.GPS_MODE mode;
	private boolean running;
	
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

	public double getDistance() {
		return distance;
	}

	public Position getMyPos() {
		return myPos;
	}

	public void setMyPos(Position myPos) {
		this.myPos = myPos;
	}

	public Position getTarPos() {
		return tarPos;
	}

	public void setTarPos(Position tarPos) {
		this.tarPos = tarPos;
	}

	private void runSequence() throws InterruptedException {
		while (running) {
			if (mode == C.GPS_MODE.FAKE) {
				final double seconds = 5;
				double onStart = System.currentTimeMillis();
				speed = 8 + ((new Random()).nextDouble() * 9);
				double remainingDistance = distanceOnGeoid(myPos.latval, myPos.longval, tarPos.latval, tarPos.longval);
				double distPercentage = speed / remainingDistance;
				myPos.latval = (tarPos.latval - myPos.latval) * distPercentage * seconds;
				myPos.longval = (tarPos.longval - myPos.longval) * distPercentage * seconds;
				distance = distanceOnGeoid(myPos.latval, myPos.longval, tarPos.latval, tarPos.longval);
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
	 
		// radius of earth in metres
		double r = 6378100;
	 
		// P
		double rho1 = r * Math.cos(lat1);
		double z1 = r * Math.sin(lat1);
		double x1 = rho1 * Math.cos(lon1);
		double y1 = rho1 * Math.sin(lon1);
	 
		// Q
		double rho2 = r * Math.cos(lat2);
		double z2 = r * Math.sin(lat2);
		double x2 = rho2 * Math.cos(lon2);
		double y2 = rho2 * Math.sin(lon2);
	 
		// Dot product
		double dot = (x1 * x2 + y1 * y2 + z1 * z2);
		double cos_theta = dot / (r * r);
	 
		double theta = Math.acos(cos_theta);
	 
		// Distance in Metres
		return r * theta;
	}
}
