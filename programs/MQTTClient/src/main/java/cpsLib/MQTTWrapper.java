package cpsLib;

import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.UUID;

import com.hivemq.client.mqtt.datatypes.MqttQos;
//import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
//import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

public class MQTTWrapper {
	private Mqtt5AsyncClient client;
	private UUID myUUID;

	public UUID getMyUUID() {
		return myUUID;
	}

	public MQTTWrapper(String host, boolean useSSL) {
		myUUID = UUID.randomUUID();
		
		if (useSSL) {
			client = Mqtt5Client.builder()
					.identifier(myUUID.toString())
					.serverHost(host)
					.serverPort(C.SSLPORT)
					.sslWithDefaultConfig()
					.buildAsync();
		} else {
			client = Mqtt5Client.builder()
					.identifier(myUUID.toString())
					.serverHost(host)
					.serverPort(C.NOSSLPORT)
					.buildAsync();
		}
	}
	
	public void connect(String username, String passwd) {
		client.connectWith().simpleAuth()
				.username(username).password(passwd.getBytes()).applySimpleAuth()
			.send()
			.whenComplete((connAck, throwable) -> {
				System.out.println("Connection failed: " + throwable.toString());
			});
		

		while (!client.getState().isConnected()) {
			//System.out.println("Not yet connected. Client State: " + client.getState().toString());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		//System.out.println("Connected to Broker");
	}
	
	public void subscribe(String topic, Queue<String> msgQueue) {
		client.subscribeWith()
	        .topicFilter(topic)
	        .callback(publish -> {
	            msgQueue.add(StandardCharsets.UTF_8.decode(publish.getPayload().get()).toString());
	        })
	        .send()
	        .whenComplete((subAck, throwable) -> {
	            if (throwable != null) {
	                // Handle failure to subscribe
	                System.out.println("Failed to subscribe");
	            } else {
	                // Handle successful subscription, e.g. logging or incrementing a metric
	                // System.out.println("Succeeded to subscribe");
	            }
	        });
	}
	
	public void unsubscribe(String topic) {
		client.unsubscribeWith().topicFilter(topic).send();
	}
	
	public void disconnect() {
		client.disconnect();
	}
	
	public void publish(String topic, String text) {
		client.publishWith()
	        .topic(topic)
	        .payload(text.getBytes())
	        .qos(MqttQos.AT_LEAST_ONCE)
	        .send()
	        .whenComplete((mqttPublish, throwable) -> {
	            if (throwable != null) {
	                System.out.println("Failed to publish message");
	            } else {
	                // Handle successful publish, e.g. logging or incrementing a metric
	            }
	        });
	}
	
	public static void main(String[] args) throws InterruptedException {
		MQTTWrapper myClient = new MQTTWrapper("192.168.2.112", false);

		myClient.connect("Finnbo", "simplepw");
		//Queue<String> testQueue = new LinkedList<>();
		//myClient.subscribe("test", testQueue);
		
		while (true) {
			myClient.publish("test", "HALLOOOO");
			Thread.sleep(1000);
		}
	}

}
