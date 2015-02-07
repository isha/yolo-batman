package com.group7.eece411.A2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * 
 *
 */
public class Service 
{	
	public enum GossipState {
		ACTIVE_GOSSIP, PASSIVE_GOSSIP, WAIT_FOR_INIT
	}
	private JSONObject loc;
	private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> statsData;
	private UDPClient client;
	private Timer timer;
	private boolean isStop;
	private GossipState currentState;
	
	public Service() throws MalformedURLException, UnknownHostException, IOException, ParseException {
		this.loc = request("http://ip-api.com/json/"+InetAddress.getLocalHost().getHostAddress(), "GET");
		this.client = new UDPClient(5628);  
		this.isStop = true;
		this.currentState = GossipState.WAIT_FOR_INIT;
	}
	
    public void run(final String host, final String port) throws Exception
    {	
    	if(!this.isStop()) {
    		return;
    	}
		this.timer = new Timer(true); //daemon thread
		this.timer.scheduleAtFixedRate(new TimerTask() {
    		public void run() {
    			try {
    				String dataString = new JSONObject(getData()).toJSONString();
					client.send(host, port, dataString);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}, 0, 30000);   
		this.isStop = false;
    }
    
    public void stop() {
    	this.isStop = true;
    	this.timer.cancel();    	
    }
    
    public void terminate() {
    	this.stop();   
    	client.closeSocket();
    }
    
    public boolean isStop() {
    	return this.isStop;
    }
    
    public JSONObject request(String url, String method) throws MalformedURLException, IOException, ParseException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod(method);
		if(connection.getResponseCode() == 200) {
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			if((line = in.readLine()) != null) {
    			return (JSONObject) (new JSONParser()).parse(line);
			}
		}
		return null;
	}
    
    @SuppressWarnings("unchecked")
	public JSONObject getData() throws IOException, java.text.ParseException {
    	statsData = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();
    	ConcurrentHashMap<String, String> currentData = new ConcurrentHashMap<String, String>();
		
    	
    	currentData.put("hostname", SystemCmd.getHostName());
    	currentData.put("systemUptime", SystemCmd.uptime());
    	currentData.put("deploySize", String.valueOf(SystemCmd.getFileSize("filename")));
    	currentData.put("spaceAvailable", String.valueOf(SystemCmd.getDiskAvailableSize()));
    	currentData.put("averageLoads", SystemCmd.getLoad());
    	long millis = ManagementFactory.getRuntimeMXBean().getUptime();
    	long days = TimeUnit.MILLISECONDS.toDays(millis);
    	long hours = TimeUnit.MILLISECONDS.toHours(millis) - TimeUnit.DAYS.toHours(days);
    	long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.DAYS.toMinutes(days) - TimeUnit.HOURS.toMinutes(hours);
    	currentData.put("serviceUptime", days + " days " + hours + " hours " + minutes + " minutes");
    	currentData.put("latitude", String.valueOf(loc.get("lat")));
    	currentData.put("longitude", String.valueOf(loc.get("lon")));
    	currentData.put("city", String.valueOf(loc.get("city")));
    	currentData.put("country", String.valueOf(loc.get("country")));
    	currentData.put("isp", String.valueOf(loc.get("isp"))); 
    	
		statsData.put(SystemCmd.getHostName(), currentData);
		
		// Temporary workaround - dont need this once node server can understand hostname: statsdata KV store
		JSONObject tmp = new JSONObject(currentData);
		return tmp;
    }
}
