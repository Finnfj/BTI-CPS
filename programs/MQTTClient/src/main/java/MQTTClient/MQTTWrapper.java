package MQTTClient;

import java.nio.charset.StandardCharsets;
import java.util.UUID;


import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.*;

public class MQTTWrapper {
	Mqtt5AsyncClient client;
	
	public MQTTWrapper(String host, boolean useSSL) {
		if (useSSL) {
			client = Mqtt5Client.builder()
					.identifier(UUID.randomUUID().toString())
					.serverHost(host)
					.serverPort(Constants.SSLPORT)
					.sslWithDefaultConfig()
					.buildAsync();
		} else {
			client = Mqtt5Client.builder()
					.identifier(UUID.randomUUID().toString())
					.serverHost(host)
					.serverPort(Constants.NOSSLPORT)
					.buildAsync();
		}
	}
	public static void main(String[] args) throws InterruptedException {
		MQTTWrapper myClient = new MQTTWrapper("test.mosquitto.org", true);

		myClient.client.connectWith().simpleAuth()
				.username("Finnbo").password("simplepassword".getBytes()).applySimpleAuth()
			.send()
			.whenComplete((connAck, thwrowable) -> {
				System.out.println("Connection succeeded");
			});
		
		while (!myClient.client.getState().isConnected()) {}
		
		myClient.client.subscribeWith()
	        .topicFilter("test/topic")
	        .callback(publish -> {
	            System.out.println(StandardCharsets.UTF_8.decode(publish.getPayload().get()));
	        })
	        .send()
	        .whenComplete((subAck, throwable) -> {
	            if (throwable != null) {
	                // Handle failure to subscribe
	                System.out.println("Failed to subscribe");
	            } else {
	                // Handle successful subscription, e.g. logging or incrementing a metric
	                System.out.println("Succeeded to subscribe");
	            }
	        });
		
		while (true) {
			myClient.client.publishWith()
		        .topic("test/topic")
		        .payload("Hello everyone".getBytes())
		        .qos(MqttQos.EXACTLY_ONCE)
		        .send()
		        .whenComplete((mqtt5Publish, throwable) -> {
		            if (throwable != null) {
		                System.out.println("Failed to publish message");
		            } else {
		                System.out.println("Succeeded to publish message");
		                // Handle successful publish, e.g. logging or incrementing a metric
		            }
		        });
			Thread.sleep(1000);
		}
	}

}
