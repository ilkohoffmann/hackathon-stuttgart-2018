package Hackathon2018.MQTTDemo;

import org.eclipse.paho.client.mqttv3.*;

import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;

public class AdamosApp2 {

	public static void main(String[] args) throws Exception {
		final String clientId = "1984"; // UUID.randomUUID().toString();
		System.out.println("Connect with UUID " + clientId);
		final String serverURI = "tcp://shakeandbake.adamos-dev.com:1883";

		String tenant = "shakeandbake";
		String username = "mkintz@gmail.com";
		String password = "maxouAdamos";

		// configure MQTT connection

		final MqttConnectOptions options = new MqttConnectOptions();
		options.setUserName(tenant + "/" + username);

		options.setPassword(password.toCharArray());
		final MqttClient client = new MqttClient(serverURI, clientId, null);

		// connect to the Cumulocity
		client.connect(options);

		// create device
		client.publish("s/us", "100,Maxou MQTT device,c8y_MQTTDevice".getBytes(), 2, false);

		// set hardware information
		client.publish("s/us", "110,S123456789,MQTT Maxou test model,Rev0.1".getBytes(), 2, false);

		client.publish("s/us", new MqttMessage(("114,c8y_Command,c8y_Configuration").getBytes()));

		// listen for operation
		client.subscribe("s/ds", new IMqttMessageListener() {
			public void messageArrived(final String topic, final MqttMessage message) throws Exception {
				final String payload = new String(message.getPayload());
				System.out.println("Received operation " + payload);
				if (payload.startsWith("510")) {
					System.out.println("Simulating device restart...");
					client.publish("s/us", "501,c8y_Restart".getBytes(), 2, false);
					System.out.println("...restarting...");
					Thread.sleep(TimeUnit.SECONDS.toMillis(1));
					client.publish("s/us", "503,c8y_Restart".getBytes(), 2, false);
					System.out.println("...done...");
				} else if (payload.startsWith("511")) {
					try {
						String setHueto = payload.substring(payload.lastIndexOf(",") + 1);
						int newHue = Integer.parseInt(setHueto);
						System.out.println("Set hue to " + newHue);
						Hue.setHue(newHue);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(new Runnable() {
			public void run() {
				try {

					// send temperature measurement
					// 211 = temperature
					int temperature = 20 + new Random().nextInt(5);
					client.publish("s/us", new MqttMessage(("211," + temperature).getBytes()));
					System.out.println(new Date() + " - sent tempreature " + temperature);
					client.publish("s/us",
							new MqttMessage("303,minorAlarmType,Hello Hackathon Stuttgart 2018".getBytes()));
					client.publish("s/us",
							new MqttMessage("400,HackathonEvent,It is late at night".getBytes()));
					client.publish("s/us",
							new MqttMessage(("200,MaxouFragment,HackathonSeries," + (temperature / 2) + ",cm").getBytes()));
				} catch (MqttException e) {
					e.printStackTrace();
				}
			}
		}, 1, 3, TimeUnit.SECONDS);
	}
}
