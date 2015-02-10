package com.group7.eece411.A2;

public class Application {

	public static int DEFAULT_PORT = 41170;

	private static String STOP_COMMAND = "stop";
	private static String START_COMMAND = "start";
	private static String EXIT_COMMAND = "exit";
	private static String ACTIVE_COMMAND = "active";

	public static void main(String[] args) throws Exception {
		System.setProperty("java.net.preferIPv4Stack", "true");
		UDPClient client = new UDPClient(DEFAULT_PORT);
		client.setTimeout(0);

		Service gossipService = new Service();

		String msg;
		do {
			byte[] receivedBytes = client.receive();
			msg = (new String(receivedBytes, "UTF-8")).trim();

			if (msg.equalsIgnoreCase(STOP_COMMAND)) {
				gossipService.stop();
			} else if (msg.equalsIgnoreCase(START_COMMAND)) {
				gossipService.start();
			} else if (msg.equalsIgnoreCase(ACTIVE_COMMAND)) {
				gossipService.startGossiping();
			} else {
				gossipService.processMessage(receivedBytes);
			}
		} while (!msg.equalsIgnoreCase(EXIT_COMMAND));

		gossipService.terminate();
	}

}
