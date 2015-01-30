package com.sn40243107.eece411.A1;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.group7.eece411.A2.UDPClient;

public class UDPClientTest {

	private UDPClient client;
	
	@Before
	public void setUp() throws Exception {
		client = new UDPClient(9999);
	}

	@After
	public void tearDown() throws Exception {
		client.closeSocket();
	}
	
	@Test
	public void testSetGetTimeout() {
		assertEquals(100, client.getTimeout());
		client.setTimeout(999);
		assertEquals(999, client.getTimeout());
		client.setTimeout(-1234);
		assertEquals(100, client.getTimeout());
		client.setTimeout(555);
		assertEquals(555, client.getTimeout());
	}

	@Test(expected = SocketException.class)
	public void testCreateSocket() throws SocketException {
		try {
			client.createSocket();
			client.createSocket();
		} catch (SocketException e) {
			fail(e.getMessage());
		}
		
		UDPClient testClient;
		try {
			testClient = new UDPClient(9999);
			testClient.createSocket();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testCloseSocket() {
		client.closeSocket();
		try {
			client.createSocket();
		} catch (SocketException e) {
			fail(e.getMessage());
		}
		client.closeSocket();		
	}
/*
	@Test(expected = Exception.class)
	public void testSend_reply() throws Exception {
		try { //happy path
			byte[] result = client.send_reply(InetAddress.getByName("168.235.153.23"), 5627, "909090");
			assertEquals("D728540991957D00FCF08D832626AD02", StringUtils.byteArrayToHexString(Arrays.copyOfRange(result, 4, 20)));
			
		} catch (Exception e) {
			fail(e.getMessage());
		}		
		//fail to reach the host
		client.send_reply(InetAddress.getByName("255.255.255.255"), 5627, "909090");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSendArugements() throws IllegalArgumentException, UnknownHostException, IOException{
		client.send(InetAddress.getByName("168.235.153.23"), 5627, null);
	}
	
	@Test
	public void testSend(){
		try {
			client.send(InetAddress.getByName("168.235.153.23"), 5627, new byte[]{0, 0});
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test(expected = SocketTimeoutException.class)
	public void testReceive() throws SocketException, IOException {
		client.receive();
	}
*/
}
