package com.group7.eece411.A2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemCmd {

	public static String uptime() throws IOException, ParseException {
		String os = System.getProperty("os.name").toLowerCase();
		BufferedReader in;
		String line;
		if(os.contains("win")) { //Windows
			in = getBufferedReaderFromCmd("net stats srv");
			while((line = in.readLine()) != null) {
				if(line.startsWith("Statistics since")) {
					Date startTime = (new SimpleDateFormat("'Statistics since' yyyy-MM-dd hh:mm:ss a")).parse(line);
					return String.valueOf(System.currentTimeMillis() - startTime.getTime());
				}
			}
		} else {
			in = getBufferedReaderFromCmd("uptime");
			if((line = in.readLine()) != null) {
	            Matcher matcher = Pattern.compile("((\\d+) days,)? (\\d+):(\\d+)").matcher(line.split("up", 2)[1]);
	            if (matcher.find()) {
	                String _days = matcher.group(2);
	                String _hours = matcher.group(3);
	                String _minutes = matcher.group(4);
	                int days = _days != null ? Integer.parseInt(_days) : 0;
	                int hours = _hours != null ? Integer.parseInt(_hours) : 0;
	                int minutes = _minutes != null ? Integer.parseInt(_minutes) : 0;
	                return days + " days " + hours + " hours " + minutes + " minutes";
	            }
			}			
		}
		return "0 days 0 hours 0 minutes";
	}
	
	public static int getFileSize(String filename) {
		return -1;
	}
	
	public static long getDiskAvailableSize() throws IOException {
		for (Path root : FileSystems.getDefault().getRootDirectories())	{
		    try {
		    	return Files.getFileStore(root).getUsableSpace()/1024/1024;
		    } catch (FileSystemException e) {
		    	System.out.println(e.getMessage());
		    	return -1;
		    }
		}
		return -1;
	}
	
	public static String getLoad() throws IOException {
		String os = System.getProperty("os.name").toLowerCase();
		BufferedReader in;
		String line;
		if(!os.contains("win")) {
			in = getBufferedReaderFromCmd("uptime");
			if((line = in.readLine()) != null) {
				return line.substring(line.indexOf("load average: ")+13);			
			}
		} 			
		return "";
	}
	
	public static String getHostName() throws IOException {
		BufferedReader in = getBufferedReaderFromCmd("hostname");
		return in.readLine();
	}
	
	private static BufferedReader getBufferedReaderFromCmd(String cmd) throws IOException {
		InputStreamReader isr = new InputStreamReader(Runtime.getRuntime().exec(cmd).getInputStream());
		return new BufferedReader(isr);		
	}
}
