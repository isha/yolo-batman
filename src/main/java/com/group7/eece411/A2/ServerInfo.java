package com.group7.eece411.A2;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;

public class ServerInfo extends JSONObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5065919895932350647L;

	
	public ServerInfo()
	{
		super();
		update();
	}
	
	@SuppressWarnings("unchecked")
	public void update()
	{		
		put("hostname", SystemCmd.getHostName());
    	put("systemUptime", SystemCmd.uptime());
    	put("deploySize", String.valueOf(SystemCmd.getFileSize("filename")));
    	put("spaceAvailable", String.valueOf(SystemCmd.getDiskAvailableSize()));
    	put("averageLoads", SystemCmd.getLoad());
    	long millis = ManagementFactory.getRuntimeMXBean().getUptime();
    	long days = TimeUnit.MILLISECONDS.toDays(millis);
    	long hours = TimeUnit.MILLISECONDS.toHours(millis) - TimeUnit.DAYS.toHours(days);
    	long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.DAYS.toMinutes(days) - TimeUnit.HOURS.toMinutes(hours);
    	put("serviceUptime", days + " days " + hours + " hours " + minutes + " minutes");
    	
		JSONObject location = Request.request("http://ip-api.com/json/" + getLocalHost(), "GET");

    	put("latitude", String.valueOf(location.get("lat")));
    	put("longitude", String.valueOf(location.get("lon")));
    	put("city", String.valueOf(location.get("city")));
    	put("country", String.valueOf(location.get("country")));
    	put("isp", String.valueOf(location.get("isp"))); 
	}
	
	private String getLocalHost()
	{
		try {
		return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
}
