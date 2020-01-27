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
	static final String DB_URL = "jdbc:mysql://localhost/cps";
	static final String PASSENGER_QUERY = "SELECT * FROM clients WHERE id = ";
	static final String ROUTE_QUERY = "SELECT * FROM routes WHERE id = ";
	static final String INSERTROUTE_QUERY = "INSERT INTO routes VALUES ";
	static final String CREATEROUTE_QUERY_1 = "CREATE TABLE `";
	static final String CREATEROUTE_QUERY_2 = "` (name VARCHAR(50), lat FLOAT, lon FLOAT, PRIMARY KEY (name))";
	static final String GETROUTE_QUERY = "SELECT * FROM ";
	static final String ALLROUTE_QUERY = "SELECT * FROM routes";
	static final String INSERT_QUERY = "INSERT INTO ";
	static final String DELETEROUTE_QUERY = "DELETE FROM routes WHERE id = ";
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
			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			stmt = conn.createStatement();
			System.out.println("Connection succesful");
		} catch (SQLException s) {
			s.printStackTrace();
		}
	}

	public Passenger getClient(String id) {
		Passenger pas = null;
		try {
			resultSet = stmt.executeQuery(PASSENGER_QUERY + "\""+id+"\"");
			if (resultSet.next()) {
				String start = resultSet.getString("start");
				String target = resultSet.getString("target");
				PassengerState state = PassengerState.values()[resultSet.getInt("state")];
				Route r = getRoute(resultSet.getString("route"));
				pas = new Passenger(id, 
						r.getRoutePoint(start), 
						r.getRoutePoint(target), 
						state, r);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pas;
	}
	
	public Boolean setClient(Passenger pas) {
		Boolean ret = false;
		
		try {
			resultSet = stmt.executeQuery(PASSENGER_QUERY + "\""+pas.pasName+"\"");
			if (resultSet.next()) {
				// update
				stmt.execute(UPDATECLIENT_QUERY 
						+ "state = " + pas.state.ordinal()
						+ ", handler = '" + pas.currHandler
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
		Route route = null;
		try {
			resultSet = stmt.executeQuery(ROUTE_QUERY + "\""+id+"\"");
			if (resultSet.next()) {
				String name = resultSet.getString("name");
				resultSet = stmt.executeQuery(GETROUTE_QUERY + "`" + id + "`");
				List<RoutePoint> rps = new LinkedList<>();
				while (resultSet.next()) {
					rps.add(new RoutePoint(resultSet.getString("name"), resultSet.getFloat("lat"), resultSet.getFloat("lon")));
				}
				route = new Route(id, name, rps);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return route;
	}
	
	public Map<String, Route> getRoutes() {
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
		Resources r = new Resources(C.RESOURCE_FROM.FILE);
		Route route = r.getRouteMap().get("hamburg-grosserunde");
		db.addRoute("hamburg-grosserunde", route);
		//db.remRoute(route.getName().toLowerCase());
		
		Passenger p = new Passenger("James Bond", route.getRoute().get(1), route.getRoute().get(3), PassengerState.Requested, route);
		db.setClient(p);
		Passenger p2 = db.getClient("James Bond");
		System.out.println(p2);
	}// end main
}
