package cpsLib;

public class RoutePoint {
	private String name;
	private double latVal;
	private double longVal;
	
	public RoutePoint(String name, double latVal, double longVal) {
		super();
		this.name = name;
		this.latVal = latVal;
		this.longVal = longVal;
	}
	
	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public double getlatVal() {
		return latVal;
	}


	public void setlatVal(double latVal) {
		this.latVal = latVal;
	}


	public double getlongVal() {
		return longVal;
	}


	public void setlongVal(double longVal) {
		this.longVal = longVal;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof RoutePoint) {
			RoutePoint other = (RoutePoint) o;
			return (other.getlatVal() == latVal && other.getlongVal() == longVal && other.getName() == name);
		} else {
			return false;	
		}
		
	}
}
