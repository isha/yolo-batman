package com.group7.eece411.A2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.Timer;
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

	public enum GossipState {
		STOPPED, WAIT_FOR_INIT, ACTIVE_GOSSIP, PASSIVE_GOSSIP
	}

	private HostPort[] hostPorts = { new HostPort("127.0.0.1", "7777") };
	private ConcurrentHashMap<String, JSONObject> statsData;
	private Timer timer;
	private GossipState currentState;
	private GossipTimerTask gossipTimerTask;
	private Vector<byte[]> uniqueIds;

	public Service() throws MalformedURLException, UnknownHostException,
			IOException, ParseException {
		this.setState(GossipState.WAIT_FOR_INIT);
		this.timer = new Timer(true);

		statsData = new ConcurrentHashMap<String, JSONObject>();
		uniqueIds = new Vector<byte[]>();
		
		this.gossipTimerTask = new GossipTimerTask(hostPorts, statsData, uniqueIds);

		// Start gossiping for now, but eventually need to wait for message before starting 
		this.setState(GossipState.ACTIVE_GOSSIP);
	}

	public void terminate() {
		this.setState(GossipState.STOPPED);
	}

	public void start() {
		this.setState(GossipState.WAIT_FOR_INIT);
	}

	public void stop() {
		this.setState(GossipState.STOPPED);
	}

	@SuppressWarnings("unchecked")
	public void processMessage(byte[] msg) {
		// Ignore the message
		if (this.currentState == GossipState.STOPPED) {
			return;
		}

		// Transition into active state, still respond to this message
		if (this.currentState == GossipState.WAIT_FOR_INIT) {
			this.setState(GossipState.ACTIVE_GOSSIP);
		}
		
		// Check for a reply to a message we initiated
		ByteBuffer byteBuffer = ByteBuffer.wrap(msg);
		byte[] uniqueID = new byte[16];
		byteBuffer.get(uniqueID);
		boolean isReply = false;
		int uniqueIdIndex = 0;
		for (uniqueIdIndex = 0; uniqueIdIndex < uniqueIds.size(); uniqueIdIndex++) {
			if (Arrays.equals(uniqueID, uniqueIds.get(uniqueIdIndex))) {
				uniqueIds.remove(uniqueIdIndex);
				isReply = true;
				break;
			}
		}
		
		if (!isReply) {
		// TODO: Get ip and port from uniqueId
		
		
		// Reply with our current data
		}
		
		
		// Merge received data with own data
		byte[] actualMsgBytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(actualMsgBytes);
		String actualMsg = "";
		try {
			actualMsg = new String(actualMsgBytes, "UTF-8").trim();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JSONObject receivedJSONObject = (JSONObject) JSONValue.parse(actualMsg);
		
		Set<String> keys = receivedJSONObject.keySet();
		for(String key : keys)
		{
			JSONObject obj = (JSONObject) receivedJSONObject.get(key);
			statsData.putIfAbsent(key, obj);
		}
		
		// Check if data complete
		if (statsData.size() == hostPorts.length && this.currentState == GossipState.ACTIVE_GOSSIP) {
			this.setState(GossipState.PASSIVE_GOSSIP);
		}
	}

	private void setState(GossipState state) {
		if (this.currentState == state) {
			return;
		}

		this.currentState = state;
		if (this.currentState == GossipState.ACTIVE_GOSSIP) {
			startGossipTask();
		} else if (this.currentState == GossipState.PASSIVE_GOSSIP) {
			stopGossipTask();
		}
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
}
