package com.group7.eece411.A2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Request {
	public static JSONObject request(String url, String method) {
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url)
					.openConnection();
			connection.setRequestMethod(method);
			if (connection.getResponseCode() == 200) {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						connection.getInputStream()));
				String line;
				if ((line = in.readLine()) != null) {
					return (JSONObject) (new JSONParser()).parse(line);
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
