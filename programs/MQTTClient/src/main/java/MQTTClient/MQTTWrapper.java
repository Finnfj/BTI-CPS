package MQTTClient;

import com.hivemq.client.*;
import com.hivemq.client.mqtt.mqtt5.*;

public class MQTTWrapper {
	UUID asd;
	Mqtt5Client client = Mqtt5Client.builder()
	        .identifier(UUID.randomUUID().toString())
	        .serverHost("broker.hivemq.com")
	        .useMqttVersion5()
	        .build();
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
