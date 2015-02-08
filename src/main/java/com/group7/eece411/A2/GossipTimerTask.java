package com.group7.eece411.A2;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;

public class GossipTimerTask extends TimerTask {

	private static int NUM_TO_GOSSIP_WITH = 1;

	private static int PORT = 5628;
	private HostPort[] hostPorts;
	private UDPClient client;
	private Vector<byte[]> uniqueIds;
	private ConcurrentHashMap<String, JSONObject> statsData;

	public GossipTimerTask(HostPort[] hostPorts,
			ConcurrentHashMap<String, JSONObject> statsData,
			Vector<byte[]> uniqueIds) throws UnknownHostException {
		this.hostPorts = hostPorts;
		this.client = new UDPClient(PORT);
		this.statsData = statsData;
		this.uniqueIds = uniqueIds;
	}

	public void sendDataTo(String hostName, String port, boolean storeUniqueId) {
		byte[] uniqueId;
		try {
			String obj = JSONObject.toJSONString(statsData);
			System.out.println(obj);
			uniqueId = client.send(hostName, port, obj);
			if (storeUniqueId) {
				uniqueIds.add(uniqueId);
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendDataTo(String hostName, String port) {
		sendDataTo(hostName, port, true);
	}

	@Override
	public void run() {
		Random random = new Random();

		for (int i = 0; i < NUM_TO_GOSSIP_WITH; i++) {
			int randomIndex = random.nextInt(hostPorts.length);
			HostPort hostPort = hostPorts[randomIndex];
			sendDataTo(hostPort.hostName, hostPort.port);
		}
	}
	
}
