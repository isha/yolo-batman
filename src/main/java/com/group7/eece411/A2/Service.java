package com.group7.eece411.A2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 * 
 *
 */
public class Service {
	private static int GOSSIP_DELAY_MS = 100;
	private static int EXIT_PASSIVE_GOSSIP_STATE_DELAY_MS = 5000;
	private static int DEFAULT_SEND_PORT = 41171;

	public enum GossipState {
		STOPPED, WAIT_FOR_INIT, ACTIVE_GOSSIP, PASSIVE_GOSSIP
	}

	private ArrayList<HostPort> hostPorts;
	private ConcurrentHashMap<String, JSONObject> statsData;
	private Timer timer;
	private Timer endPassiveStateTimer;
	private GossipState currentState;
	private GossipTimerTask gossipTimerTask;
	private ConcurrentHashMap<Date, ConcurrentHashMap<String, byte[]>> uniqueIds;

	public Service() throws MalformedURLException, IOException, ParseException {
		this.currentState = GossipState.WAIT_FOR_INIT;
		this.timer = new Timer(true);

		this.statsData = new ConcurrentHashMap<String, JSONObject>();
		this.uniqueIds = new ConcurrentHashMap<Date, ConcurrentHashMap<String, byte[]>>();
		this.hostPorts = getHostPorts();
		this.endPassiveStateTimer = new Timer();
	}

	public void start() throws IllegalArgumentException, IOException {
		this.setState(GossipState.WAIT_FOR_INIT);
	}

	public void startGossiping() throws IllegalArgumentException, IOException {
		this.setState(GossipState.ACTIVE_GOSSIP);
	}

	public void stop() throws IllegalArgumentException, IOException {
		this.setState(GossipState.STOPPED);
	}

	public void terminate() throws IllegalArgumentException, IOException {
		this.setState(GossipState.STOPPED);
		//TODO: This does not "terminate" the service.
	}

	@SuppressWarnings("unchecked")
	public void processMessage(byte[] msg) throws IllegalArgumentException,
			IOException {
		// Ignore the message
		if (this.currentState == GossipState.STOPPED) {
			return;
		}			

		// Transition into active state, still respond to this message
		if (this.currentState == GossipState.WAIT_FOR_INIT) {
			startGossiping();
		}

		// Check for a reply to a message we initiated
		ByteBuffer byteBuffer = ByteBuffer.wrap(msg);
		byte[] uniqueID = new byte[16];
		byteBuffer.get(uniqueID);
		boolean isReply = false;

		synchronized (uniqueIds) {
			for (Date key : uniqueIds.keySet()) {
				for (String hostname : uniqueIds.get(key).keySet()) {
					if (Arrays.equals(uniqueID, uniqueIds.get(key).get(hostname))) {
						uniqueIds.get(key).remove(hostname);
						uniqueIds.remove(key);
						isReply = true;
						break;
					}
				}
			}
		}

		if (!isReply) {
			byte[] ipAddr = new byte[] { uniqueID[0], uniqueID[1], uniqueID[2],
					uniqueID[3] };
			InetAddress addr = null;
			addr = InetAddress.getByAddress(ipAddr);
			System.out.println("Parsed ip address from uniqueHeader" + addr);

			// Reply with our current data
			gossipTimerTask.sendDataTo(addr.getHostName(),
					String.valueOf(Application.DEFAULT_RECEIVE_PORT), false);
		}

		if(this.currentState == GossipState.PASSIVE_GOSSIP) {
			this.endPassiveStateTimer.cancel();
			scheduleEndPassiveStateTimer();
			return; //stop here since this node is already in PASSIVE state
		}
		
		// Merge received data with own data
		byte[] actualMsgBytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(actualMsgBytes);
		String actualMsg = "";
		actualMsg = new String(actualMsgBytes, "UTF-8").trim();
		JSONObject receivedJSONObject = (JSONObject) JSONValue.parse(actualMsg);
		synchronized(statsData) {
			Set<String> keys = receivedJSONObject.keySet();
			for (String key : keys) {
				JSONObject obj = (JSONObject) receivedJSONObject.get(key);
				statsData.putIfAbsent(key, obj);
			}
			System.out.println(statsData.toString());
			// Check if data complete
			if (statsData.size() == hostPorts.size()
					&& this.currentState == GossipState.ACTIVE_GOSSIP) {
				this.setState(GossipState.PASSIVE_GOSSIP);
			}
		}
	}

	public void setState(GossipState state) throws IllegalArgumentException,
			IOException {
		if(this.currentState != GossipState.STOPPED && state == GossipState.STOPPED) {
			System.out.println(state);
			stopGossipTask();
			this.endPassiveStateTimer.cancel();
			this.currentState = state;
		} else if ((this.currentState == GossipState.STOPPED || this.currentState == GossipState.PASSIVE_GOSSIP) 
				&& state == GossipState.WAIT_FOR_INIT) {
			System.out.println(state);
			this.currentState = state;
		} else if (this.currentState == GossipState.WAIT_FOR_INIT && state == GossipState.ACTIVE_GOSSIP) {
			System.out.println(state);
			startGossipTask();
			this.currentState = state;
		} else if (this.currentState == GossipState.ACTIVE_GOSSIP && state == GossipState.PASSIVE_GOSSIP) {
			System.out.println(state);
			stopGossipTask();
			scheduleEndPassiveStateTimer();
			this.currentState = state;
		}
		
	}

	private void scheduleEndPassiveStateTimer() {
		this.endPassiveStateTimer.schedule(new TimerTask() {
			public void run() {
				try {
					setState(GossipState.WAIT_FOR_INIT);
					gossipTimerTask.sendDataTo("127.0.0.1", String.valueOf(DEFAULT_SEND_PORT), false);
					endPassiveStateTimer.cancel();
				} catch (Exception e) {
					// TODO Send exception to monitor server
				}
			}
		}, EXIT_PASSIVE_GOSSIP_STATE_DELAY_MS);
	}

	private void startGossipTask() throws UnknownHostException {
		this.gossipTimerTask = new GossipTimerTask(hostPorts, statsData, uniqueIds, this);
		statsData.clear();
		ServerInfo myInfo = new ServerInfo();
		statsData.put(String.valueOf(myInfo.get("hostname")), myInfo);
		this.timer.scheduleAtFixedRate(gossipTimerTask, 0, GOSSIP_DELAY_MS);
	}

	private void stopGossipTask() {
		this.timer.cancel();
	}

	private ArrayList<HostPort> getHostPorts() throws IOException {
		hostPorts = new ArrayList<HostPort>();
		InputStream in = getClass().getClassLoader().getResourceAsStream("file/hosts.txt"); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = null;
		while((line = reader.readLine()) != null) {
			if(line.equals("127.0.0.1") || line.equals(InetAddress.getLocalHost().getHostAddress())
					|| line.equals(InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()))) {
			} else {
 				HostPort hp = new HostPort(line,
						String.valueOf(Application.DEFAULT_RECEIVE_PORT));
				hostPorts.add(hp);
			}
		}
		return hostPorts;
	}
}
