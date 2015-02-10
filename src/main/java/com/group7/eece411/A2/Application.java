package com.group7.eece411.A2;

public class Application {

	public static int DEFAULT_PORT = 7777;

	private static String STOP_COMMAND = "stop monitor";
	private static String START_COMMAND = "start monitor";
	private static String EXIT_COMMAND = "exit service";

	public static void main(String[] args) throws Exception {
		System.setProperty("java.net.preferIPv4Stack", "true");
		UDPClient client = new UDPClient(DEFAULT_PORT);
		client.setTimeout(0);

		Service gossipService = new Service();

		String msg;
		do {
			byte[] receivedBytes = client.receive();
			msg = (new String(receivedBytes, "UTF-8")).trim();
			System.out.println("Received : " + msg);

			if (msg.equalsIgnoreCase(STOP_COMMAND)) {
				gossipService.stop();
			} else if (msg.equalsIgnoreCase(START_COMMAND)) {
				gossipService.start();
			}
			else
			{
				gossipService.processMessage(receivedBytes);
			}
		} while (!msg.equalsIgnoreCase(EXIT_COMMAND));

		gossipService.terminate();
	}

}
