package com.group7.eece411.A2;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
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
		try {
			update();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void update() throws IOException, ParseException
	{
		put("ip", InetAddress.getLocalHost().getHostAddress());
		put("ping", true);
		put("ssh", true);
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
	
	private String getLocalHost() throws UnknownHostException
	{
		return InetAddress.getLocalHost().getHostAddress();
	}
}
