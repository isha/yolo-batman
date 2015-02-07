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
					client.send(host, port, getData().toJSONString());
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
    	JSONObject obj=new JSONObject();
		obj.put("hostname", SystemCmd.getHostName());
		obj.put("systemUptime", SystemCmd.uptime());
		obj.put("deploySize", SystemCmd.getFileSize("filename"));
		obj.put("spaceAvailable", SystemCmd.getDiskAvailableSize());
		obj.put("averageLoads", SystemCmd.getLoad());
		long millis = ManagementFactory.getRuntimeMXBean().getUptime();
		long days = TimeUnit.MILLISECONDS.toDays(millis);
		long hours = TimeUnit.MILLISECONDS.toHours(millis) - TimeUnit.DAYS.toHours(days);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.DAYS.toMinutes(days) - TimeUnit.HOURS.toMinutes(hours);
		obj.put("serviceUptime", days + " days " + hours + " hours " + minutes + " minutes");
		obj.put("latitude", loc.get("lat"));
		obj.put("longitude", loc.get("lon"));
		obj.put("city", loc.get("city"));
		obj.put("country", loc.get("country"));
		obj.put("isp", loc.get("isp"));	
		//System.out.println(obj.toJSONString());
		return obj;
    }
}
