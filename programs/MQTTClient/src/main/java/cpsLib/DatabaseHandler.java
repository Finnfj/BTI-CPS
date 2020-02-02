package cpsLib;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cpsLib.Passenger.PassengerState;

public class DatabaseHandler {
	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost/cps?autoReconnect=true";
	static final String PASSENGER_QUERY = "SELECT * FROM clients WHERE id = ";
	static final String ROUTE_QUERY = "SELECT * FROM routes WHERE id = ";
	static final String INSERTROUTE_QUERY = "INSERT INTO routes VALUES ";
	static final String CREATEROUTE_QUERY_1 = "CREATE TABLE `";
	static final String CREATEROUTE_QUERY_2 = "` (name VARCHAR(50), lat FLOAT, lon FLOAT, PRIMARY KEY (name))";
	static final String GETROUTE_QUERY = "SELECT * FROM ";
	static final String ALLROUTE_QUERY = "SELECT * FROM routes";
	static final String ALLCLIENTS_QUERY = "SELECT * FROM clients";
	static final String INSERT_QUERY = "INSERT INTO ";
	static final String DELETEROUTE_QUERY = "DELETE FROM routes WHERE id = ";
	static final String DELETECLIENT_QUERY = "DELETE FROM clients WHERE id = ";
	static final String REMOVEROUTE_QUERY = "DROP TABLE ";
	static final String UPDATECLIENT_QUERY = "UPDATE clients SET ";

	// Database credentials
	static final String USER = "user";
	static final String PASS = "password";
	static final String CLIENT_TABLE = "clients";
	static final String ROUTE_TABLE = "routes";

	// Variables
	private Connection conn = null;
	private Statement stmt = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;

	public DatabaseHandler() {
		try {
			// Open a connection
			//System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			stmt = conn.createStatement();
			//System.out.println("Connection succesful");
		} catch (SQLException s) {
			s.printStackTrace();
		}
	}
	
	private void checkConnection() {
		try {
			if (!conn.isValid(5000)) {
				conn.close();
				conn = DriverManager.getConnection(DB_URL, USER, PASS);
				stmt = conn.createStatement();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Passenger getClient(String id) {
		checkConnection();
		Passenger pas = null;
		try {
			resultSet = stmt.executeQuery(PASSENGER_QUERY + "\""+id+"\"");
			if (resultSet.next()) {
				String handler = resultSet.getString("handler");
				String car = resultSet.getString("car");
				String start = resultSet.getString("start");
				String target = resultSet.getString("target");
				PassengerState state = PassengerState.values()[resultSet.getInt("state")];
				Route r = getRoute(resultSet.getString("route"));
				pas = new Passenger(id, 
						r.getRoutePoint(start), 
						r.getRoutePoint(target), 
						r, state);
				pas.currHandler = handler;
				pas.currCar = car;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pas;
	}

	public boolean remClient(String id) {
		checkConnection();
		try {
			return stmt.execute(DELETECLIENT_QUERY + "\""+id+"\" AND state = 5");	// only remove with finished state
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public List<Passenger> getClients() {
		checkConnection();
		List<Passenger> pasList = new LinkedList<>();
		
		try {
			resultSet = stmt.executeQuery(ALLCLIENTS_QUERY);
			while (resultSet.next()) {
				String name = resultSet.getString("id");
				String handler = resultSet.getString("handler");
				String car = resultSet.getString("car");
				String start = resultSet.getString("start");
				String target = resultSet.getString("target");
				PassengerState state = PassengerState.values()[resultSet.getInt("state")];
				Route r = getRoute(resultSet.getString("route"));
				Passenger pas = new Passenger(name, 
						r.getRoutePoint(target), 
						r.getRoutePoint(start), 
						r, state);
				pas.currHandler = handler;
				pas.currCar = car;
				pasList.add(pas);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pasList;
	}
	
	public Boolean setClient(Passenger pas) {
		checkConnection();
		Boolean ret = false;
		
		try {
			resultSet = stmt.executeQuery(PASSENGER_QUERY + "\""+pas.pasName+"\"");
			if (resultSet.next()) {
				// update
				stmt.execute(UPDATECLIENT_QUERY 
						+ "state = " + pas.state.ordinal()
						+ ", handler = '" + pas.currHandler
						+ "', car = '" + pas.currCar
						+ "', start = '" + pas.start.getName()
						+ "', target = '" + pas.target.getName()
						+ "', route = '" + pas.currRoute.getID()
						+ "' WHERE id = '" + pas.pasName + "'");
			} else {
				// create new
				stmt.execute(INSERT_QUERY + "clients VALUES ('"
						+ pas.pasName + "', "
						+ pas.state.ordinal() + ", '"
						+ pas.currHandler + "', '"
						+ pas.currCar + "', '"
						+ pas.start.getName() + "', '"
						+ pas.target.getName() + "', '"
						+ pas.currRoute.getID() + "')");
			}
			ret = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
	public Route getRoute(String id) {
		checkConnection();
		Route route = null;
		ResultSet tmp;
		Statement tmpstmt = null;
		try {
			tmpstmt = conn.createStatement();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		try {
			tmp = tmpstmt.executeQuery(ROUTE_QUERY + "\""+id+"\"");
			if (tmp.next()) {
				String name = tmp.getString("name");
				tmp = tmpstmt.executeQuery(GETROUTE_QUERY + "`" + id + "`");
				List<RoutePoint> rps = new LinkedList<>();
				while (tmp.next()) {
					rps.add(new RoutePoint(tmp.getString("name"), tmp.getFloat("lat"), tmp.getFloat("lon")));
				}
				route = new Route(id, name, rps);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return route;
	}
	
	public Map<String, Route> getRoutes() {
		checkConnection();
		Map<String, Route> routeMap = new HashMap<>();
		
		try {
			resultSet = stmt.executeQuery(ALLROUTE_QUERY);
			while (resultSet.next()) {
				routeMap.put(resultSet.getString("id"), getRoute(resultSet.getString("id")));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return routeMap;
	}
	
	public Boolean addRoute(String id, Route r) {
		checkConnection();
		try {
			resultSet = stmt.executeQuery(ROUTE_QUERY + "\""+id+"\"");
			if (resultSet.next()) {
				// There already is a route with this id
				return false;
			} else {
				stmt.execute(INSERTROUTE_QUERY + "('" + id + "', '" + r.getName() + "')");
				stmt.execute(CREATEROUTE_QUERY_1 + id + CREATEROUTE_QUERY_2);
				
				preparedStatement = conn.prepareStatement("INSERT INTO `" + id + "` VALUES (?, ?, ?)");
				for (RoutePoint p : r.getRoute()) {
					preparedStatement.setString(1, p.getName());
					preparedStatement.setFloat(2, (float)p.getlatVal());
					preparedStatement.setFloat(3, (float)p.getlongVal());
					preparedStatement.executeUpdate();
				}
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public Boolean remRoute(String id) {
		checkConnection();
		try {
			resultSet = stmt.executeQuery(ROUTE_QUERY + "\""+id+"\"");
			if (resultSet.next()) {
				// There already is a route with this id, remove it
				stmt.execute(DELETEROUTE_QUERY + "'" + id + "'");
				stmt.execute(REMOVEROUTE_QUERY + "`" + id + "`");
				return false;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static void main(String[] args) {
		DatabaseHandler db = new DatabaseHandler();


		Route addRoute = new Route("luebeck-altstadt", "Luebeck - Altstadt", 
				new RoutePoint("Museum", 53.860095, 10.685331),
				new RoutePoint("Holstenbruecke", 53.866245, 10.680825),
				new RoutePoint("Clemensstrasse", 53.870407, 10.680460),
				new RoutePoint("Burgtorbruecke", 53.874392, 10.691639),
				new RoutePoint("Rehderbruecke", 53.863929, 10.693055));
		db.remRoute("luebeck-altstadt");
		db.addRoute(addRoute.getID(), addRoute);
//		Resources r = new Resources(C.RESOURCE_FROM.FILE);
//		Route route = r.getRouteMap().get("hamburg-grosserunde");
//		db.remRoute("hamburg-grosserunde");
//		db.addRoute("hamburg-grosserunde", route);
		//db.remRoute(route.getName().toLowerCase());
		
	}// end main
}
