package com.group7.eece411.A2;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
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
	//Ehsan: list of ipaddresses/domain names on planetlab
	private HostPort[] hostPorts;
	private UDPClient client;
	private Vector<byte[]> uniqueIds; 
	// Ehsan: i'm assuming string represents nodes ipaddress and JSONobject hold what each node contains. 
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

	/*
	 * Added by Ehsan -> might contain wrong implementation 
	 * Recives the statsData from a the client that was contacted on sendDataTo().
	 * The client would return a byte[], converts that byte array to string and then converts the string to JSON object 
	 * */
	public JSONObject recDataFrom(String hostName, String port) {
		byte [] recievedData ;
		String recievedData_str;
		JSONObject return_val = new JSONObject();
		try {
			recievedData = client.receive();
			recievedData_str = recievedData.toString(); 
			return_val = (JSONObject)new JSONParser().parse(recievedData_str); // will be returned by the method
			
			//Here compare and save the JSON object to statsData of this node
			
			
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	 catch (ParseException e) {
			// Happens if the json parser could not parse the string
			e.printStackTrace();
		}

		return return_val;
	}
	
	/*
	 * Check to see if the node has the information regarding hostname
	 * if it does update the key value pair 
	 * if not insert it to statsData  
	 * */
	public void insertOrReplace(String hostname, JSONObject json) {
	    if (statsData.containsKey(hostname) == true)
	    {
	    	//Replaces the old JSON corresponding to host name with a new JSON received from UDPclient 
	    	statsData.replace(hostname, statsData.get(hostname), json);
	    }
	    else
	    {
	    	statsData.put(hostname, json);
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
