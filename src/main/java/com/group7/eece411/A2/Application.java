package com.group7.eece411.A2;

public class Application {
	
	public static void main(String[] args) throws Exception {
		System.setProperty("java.net.preferIPv4Stack", "true");
		UDPClient client = new UDPClient(7777);
		client.setTimeout(0); 	
		String msg;
		String monitorServer = "54.68.197.12";
		String monitorPort = "5700";
		Service monitorService = new Service();
		monitorService.run(monitorServer, monitorPort);

		do {
    		msg = (new String(client.receive(), "UTF-8")).trim();
    		System.out.println("Received : "+msg);

    		if(msg.equalsIgnoreCase("stop monitor")) {
    			monitorService.stop();
    		} else if(msg.equalsIgnoreCase("start monitor")) {
    			monitorService.run(monitorServer, monitorPort);
    		}
    	} while(!msg.equalsIgnoreCase("exit service"));
		
		monitorService.terminate();
	}

}
