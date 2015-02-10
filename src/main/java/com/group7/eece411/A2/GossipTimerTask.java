package com.group7.eece411.A2;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLEngineResult.Status;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.omg.PortableServer.REQUEST_PROCESSING_POLICY_ID;

public class GossipTimerTask extends TimerTask {

	private static int NUM_TO_GOSSIP_WITH = 1;

	private static int PORT = 5628;
	private ArrayList<HostPort> hostPorts;
	private UDPClient client;
	private Vector<byte[]> uniqueIds;
	private ConcurrentHashMap<String, JSONObject> statsData;

	public GossipTimerTask(ArrayList<HostPort> hostPorts,
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
			uniqueId = client.send(hostName, port, obj);
			if (storeUniqueId) {
				synchronized (uniqueIds) {
					uniqueIds.add(uniqueId);
				}
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

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		Random random = new Random();

		for (int i = 0; i < NUM_TO_GOSSIP_WITH; i++) {
			int randomIndex = random.nextInt(hostPorts.size());
			HostPort hostPort = hostPorts.get(randomIndex);

			try {
				if (SystemCmd.isReachable(hostPort.hostName) == false) {
					JSONObject offlineData = new JSONObject();
					offlineData.put("online", false);
					statsData.putIfAbsent(hostPort.hostName, offlineData);
				} else {
					sendDataTo(hostPort.hostName, hostPort.port);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
