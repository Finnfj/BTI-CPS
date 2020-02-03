package cpsLib;

public class C {
	public final static int SSLPORT = 8883;
	public final static int NOSSLPORT = 1883;
	
	// ***TOPIC-SECTION***
	public final static String TOPICLIMITER = "/";
	public final static String DISCOVERY_TOPIC = "firstDiscovery";
	public final static String CARHANDLING_TOPIC = "carHandling";
	public final static String DISCOVERYSERVICES_NODE = "DiscoveryServices";
	public final static String CLIENTHANDLERS_NODE = "ClientHandlers";
	public final static String CARHANDLERS_NODE = "CarHandlers";
	public final static String CLIENTS_NODE = "Clients";
	public final static String VEHICLES_NODE = "Vehicles";
	public final static String EXCHANGE_NODE = "Exchange";
	public final static String SYNCH_NODE = "Synchronization";
	public final static String PASSENGERSTATS_NODE = "PassengerStats";
	public final static String CARSTATS_NODE = "CarStats";
	public final static String OPTI_NODE = "Optimization";
	public final static String REQUEST_NODE = "Request";
	
	// Topics for passing Client handling
	public final static String HANDLING_EUROPE_TOPIC = "passHandling_EU";
	public final static String HANDLING_ASIA_TOPIC = "passHandling_AS";
	public final static String HANDLING_AFRIKA_TOPIC = "passHandling_AF";
	public final static String HANDLING_AMERIKA_TOPIC = "passHandling_AM";
	
	// ***MESSAGE-SECTION***
	// Message syntax
	public final static String LIMITER = "!";
	public final static String CMD_INITIALHANDLING = "handleInitial";
	public final static String CMD_PASSCLIENT = "passClient";
	public final static String CMD_BEENHANDLED = "beenHandled";
	public final static String CMD_OFFERHANDLING = "offerHandling";
	public final static String CMD_WANTCONNECT = "wantConnect";
	public final static String CMD_PASSREQUEST = "passRequest";
	public final static String CMD_GOTREQUEST = "gotRequest";
	public final static String CMD_OFFERCONNECT = "offerConnect";
	public final static String CMD_STATIONEXCHANGE = "stationExchange";
	public final static String CMD_DECLINEDROPOFF = "declineDropoff";
	public final static String CMD_ACCEPTDROPOFF = "acceptDropoff";
	public final static String CMD_OFFEREXCHANGE = "offerExchange";
	public final static String CMD_ACCEPTEXCHANGE = "acceptExchange";
	public final static String CMD_EXCHANGESUCCESS = "exchangeSuccess";
	public final static String CMD_EXCHANGEFAIL = "exchangeFail";
	public final static String CMD_EXCHANGEDONE = "exchangeDone";
	public final static String CMD_FORCEDROPOFF = "forceDropoff";
	public final static String CMD_CARINITIAL = "carInitial";
	public final static String CMD_CAROFFER = "carOffer";
	public final static String CMD_CARDATA = "carData";
	public final static String CMD_CARRECEIVED = "carReceived";
	public final static String CMD_DEBUG = "carReceived";
	public final static String CMD_OFFERPASSENGERSTATS = "offerPassengerStats";
	public final static String CMD_OFFERCARSTATS = "offerPassengerStats";
	public final static String CMD_OFFERSTATS = "offerStats";
	public final static String CMD_CHANGEROUTEREQUEST = "changeRouteRequest";
	public final static String CMD_CHANGEROUTEALLOW = "changeRouteAllow";
	public final static String CMD_CHANGEROUTEDECLINE = "changeRouteDecline";
	
	// ***SEMANTICS-Section***
	public final static short I_CMD = 0;
	public final static short I_ID = 1;
	public final static short I_MSG = 2;
	
	public final static int MULT = 10;
	public final static int DESIRED_RATIO_MAX = 10*MULT;
	public static int calcCost(int passengers, int cars) {
		return cars <= 0 ? (passengers * MULT) : (passengers * MULT / cars);
	}

	public enum RESOURCE_FROM {
		FILE,
		DB
	}
	
	public enum GPS_MODE {
		REAL,
		FAKE
	}
}
