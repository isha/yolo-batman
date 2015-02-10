package com.group7.eece411.A2;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLEngineResult.Status;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.omg.PortableServer.REQUEST_PROCESSING_POLICY_ID;

import com.group7.eece411.A2.Service.GossipState;

public class GossipTimerTask extends TimerTask {

	private static int NUM_TO_GOSSIP_WITH = 10;

	private static int PORT = 5628;
	private ArrayList<HostPort> hostPorts;
	private UDPClient client;
	private ConcurrentHashMap<Date, ConcurrentHashMap<String, byte[]>> uniqueIds;
	private ConcurrentHashMap<String, JSONObject> statsData;
	private int count;
	private Service serv;

	public GossipTimerTask(
			ArrayList<HostPort> hostPorts,
			ConcurrentHashMap<String, JSONObject> statsData,
			ConcurrentHashMap<Date, ConcurrentHashMap<String, byte[]>> uniqueIds,
			Service ref) throws UnknownHostException {
		this.hostPorts = hostPorts;
		this.client = new UDPClient(PORT);
		this.statsData = statsData;
		this.uniqueIds = uniqueIds;
		this.count = 0;
		this.serv = ref;
	}

	public byte[] sendDataTo(String hostName, String port, boolean storeUniqueId)
			throws IllegalArgumentException, IOException {
		byte[] uniqueId;
		String obj = JSONObject.toJSONString(statsData);
		uniqueId = client.send(hostName, port, obj);
		if (storeUniqueId) {
			ConcurrentHashMap<String, byte[]> temp = new ConcurrentHashMap<String, byte[]>();
			temp.put(hostName, uniqueId);
			synchronized (uniqueIds) {
				uniqueIds.put(new Date(System.currentTimeMillis()), temp);
			}
		}
		return uniqueId;
	}

	public byte[] sendDataTo(String hostName, String port)
			throws IllegalArgumentException, IOException {
		return sendDataTo(hostName, port, true);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		Random random = new Random();
		System.out.println("Run Gossip Task");
		boolean isRepeated = false;
		Vector<HostPort> activeHostPorts = getActiveHostPorts();
		int randomIndex = random.nextInt(activeHostPorts.size());
		final HostPort hostPort = activeHostPorts.get(randomIndex);
		synchronized (uniqueIds) {
			for (Date key : uniqueIds.keySet()) {
				if (uniqueIds.get(key).containsKey(hostPort.hostName)) {
					isRepeated = true;
				}
				if (System.currentTimeMillis() >= key.getTime() + 10000) {
					for (String hostname : uniqueIds.get(key).keySet()) {
						JSONObject offlineData = new JSONObject();
						offlineData.put("ping", false);
						statsData.putIfAbsent(hostname, offlineData);
						System.out.println("no respond, size " + statsData.size());
						System.out.println(statsData.toString());
						uniqueIds.get(key).remove(hostname);
					}
					uniqueIds.remove(key);					
					if (statsData.size() == hostPorts.size()) {
						try {
							this.serv.setState(GossipState.PASSIVE_GOSSIP);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		System.out.println("unique size : "+uniqueIds.size());
		System.out.println("statData size : "+statsData.size());
			System.out.println("Sending to : " + hostPort.hostName);
			try {
				if (SystemCmd.isReachable(hostPort.hostName) == false) {
					synchronized(statsData) {
						JSONObject offlineData = new JSONObject();
						offlineData.put("ping", false);
						statsData.putIfAbsent(hostPort.hostName, offlineData);
					}
				} else if(!isRepeated) {
					sendDataTo(hostPort.hostName, hostPort.port);
				}
				System.out.println("size : "+ statsData.size());
				System.out.println(statsData.toString());
			} catch (Exception e) {
				// TODO Send to monitor server
			}
	}

	private Vector<HostPort> getActiveHostPorts() {
		Vector<HostPort> activeHostPorts = new Vector<HostPort>();
		for (HostPort hostPort : hostPorts) {
			JSONObject serverData = statsData.get(hostPort.hostName);
			if (serverData == null)
			{
				activeHostPorts.add(hostPort);
			}
		}
		return activeHostPorts;
	}

}
