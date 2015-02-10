package com.group7.eece411.A2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
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
	private static int GOSSIP_DELAY_MS = 2000;
	private static int EXIT_PASSIVE_GOSSIP_STATE_DELAY_MS = 5000;

	public enum GossipState {
		STOPPED, WAIT_FOR_INIT, ACTIVE_GOSSIP, PASSIVE_GOSSIP
	}

	private ArrayList<HostPort> hostPorts;
	private ConcurrentHashMap<String, JSONObject> statsData;
	private Timer timer;
	private Timer endPassiveStateTimer;
	private GossipState currentState;
	private GossipTimerTask gossipTimerTask;
	private Vector<byte[]> uniqueIds;

	public Service() throws MalformedURLException, UnknownHostException,
			IOException, ParseException {
		this.setState(GossipState.WAIT_FOR_INIT);
		this.timer = new Timer(true);

		statsData = new ConcurrentHashMap<String, JSONObject>();
		uniqueIds = new Vector<byte[]>();
		hostPorts = getHostPorts();
		this.endPassiveStateTimer = new Timer();

		this.gossipTimerTask = new GossipTimerTask(hostPorts, statsData,
				uniqueIds);
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
		int uniqueIdIndex = 0;

		synchronized (uniqueIds) {
			for (uniqueIdIndex = 0; uniqueIdIndex < uniqueIds.size(); uniqueIdIndex++) {
				if (Arrays.equals(uniqueID, uniqueIds.get(uniqueIdIndex))) {
					uniqueIds.remove(uniqueIdIndex);
					isReply = true;
					break;
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
					String.valueOf(Application.DEFAULT_PORT), false);
		}

		// Merge received data with own data
		byte[] actualMsgBytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(actualMsgBytes);
		String actualMsg = "";
		actualMsg = new String(actualMsgBytes, "UTF-8").trim();

		JSONObject receivedJSONObject = (JSONObject) JSONValue.parse(actualMsg);

		Set<String> keys = receivedJSONObject.keySet();
		for (String key : keys) {
			JSONObject obj = (JSONObject) receivedJSONObject.get(key);
			statsData.putIfAbsent(key, obj);
		}

		// Check if data complete
		if (statsData.size() == hostPorts.size()
				&& this.currentState == GossipState.ACTIVE_GOSSIP) {
			this.setState(GossipState.PASSIVE_GOSSIP);
		}

		this.endPassiveStateTimer.cancel();
		scheduleEndPassiveStateTimer();
	}

	private void setState(GossipState state) throws IllegalArgumentException,
			IOException {
		if (this.currentState == state) {
			return;
		}

		this.currentState = state;
		if (this.currentState == GossipState.ACTIVE_GOSSIP) {
			startGossipTask();
		} else if (this.currentState == GossipState.PASSIVE_GOSSIP) {
			stopGossipTask();
			scheduleEndPassiveStateTimer();
			gossipTimerTask.sendDataTo("127.0.0.1", "41171", false);
		}
	}

	private void scheduleEndPassiveStateTimer() {
		this.endPassiveStateTimer.schedule(new TimerTask() {
			public void run() {
				try {
					setState(GossipState.WAIT_FOR_INIT);
				} catch (Exception e) {
					// TODO Send exception to monitor server
				}
			}
		}, EXIT_PASSIVE_GOSSIP_STATE_DELAY_MS);
	}

	private void startGossipTask() {
		statsData.clear();
		ServerInfo myInfo = new ServerInfo();
		statsData.put(String.valueOf(myInfo.get("hostname")), myInfo);
		this.timer.scheduleAtFixedRate(gossipTimerTask, 0, GOSSIP_DELAY_MS);
	}

	private void stopGossipTask() {
		this.timer.cancel();
	}

	private ArrayList<HostPort> getHostPorts() throws FileNotFoundException {
		hostPorts = new ArrayList<HostPort>();

		// Get file from resources folder
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("file/hosts.txt")
				.getFile());

		Scanner scanner = new Scanner(file);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			HostPort hp = new HostPort(line,
					String.valueOf(Application.DEFAULT_PORT));
			hostPorts.add(hp);
		}
		scanner.close();

		return hostPorts;
	}
}
