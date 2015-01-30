package com.group7.eece411.A2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @author Danny Chih Yang Hsieh
 *
 */
public class UDPClient {	
	private InetAddress source;
	private int source_port;
	private int timeout;
	private DatagramSocket socket;
	
	public UDPClient(int port) throws UnknownHostException {
		this.source = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress());
		this.source_port = port;
		this.setTimeout(100);
	}	

	public void setTimeout(int millseconds) {
		this.timeout = millseconds;
		if(millseconds < 0) 
			this.timeout = 100;
	}
	
	public int getTimeout() {
		if(this.timeout <= 0) {
			this.timeout = 100;
		}
		return this.timeout;
	}
	
	public InetAddress getSource() {
		return this.source;
	}
	
	public void createSocket() throws SocketException {
		if(this.socket == null) {
			this.socket = new DatagramSocket(this.source_port);
			this.socket.setReuseAddress(true);
		}
	}
	
	public void closeSocket() {
		if(this.socket != null && !this.socket.isClosed()) {
			this.socket.close();
		}
	}
	
	public byte[] send(String host, String port, String msg) throws IllegalArgumentException, IOException {
		byte[] uniqueId = generateUniqueID();
		this.send(host, port, msg, uniqueId);
		return uniqueId;
	}
	
	public void send(String host, String port, String msg, byte[] uniqueId) throws IllegalArgumentException, IOException {
		if(host == null || host.isEmpty() || 
				port == null || port.isEmpty() || 
				msg == null || msg.isEmpty()) {
			throw new IllegalArgumentException();
		}
        this.createSocket();
        byte[] request = constructByteRequest(uniqueId, msg);
		DatagramPacket packet = new DatagramPacket(request, request.length, InetAddress.getByName(host), Integer.valueOf(port));
		socket.send(packet);	
	}
	 
	public byte[] receive() throws SocketException, IOException { 
        this.createSocket();
        byte buffer[] = new byte[16384];        
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.setSoTimeout(this.timeout);
        socket.receive(packet); 
		
		return Arrays.copyOfRange(buffer, 0, packet.getLength());                         
	}
	/*
	public byte[] send_reply(InetAddress host, int port, String msg) throws IllegalArgumentException, IOException {
		if(host == null || port <=0 || msg == null || msg.isEmpty()) {
			throw new IllegalArgumentException();
		}
		generateUniqueID();
		byte request[] = constructByteRequest(msg);
		int count = 3;
        while(count > 0) {			
			try {
				this.send(host, port, request);
				byte[] receivePacket = this.receive();
				if(this.hasUniqueId(receivePacket))
					return Arrays.copyOfRange(receivePacket, 16, receivePacket.length - 16);;
			} catch(SocketTimeoutException se) {
				System.out.println("retrying...");
				count--;
				this.setTimeout(this.getTimeout()*2);
			}
		} 
        this.closeSocket();
        throw new IOException("Retried " + count + " times : Connection failed.");
	}*/
	
	private byte[] constructByteRequest(byte[] uniqueId, String data) {
		byte[] dataBytes;
		byte[] result;
		try { 
	        int number = Integer.parseInt(data); 
	        dataBytes = ByteBuffer.allocate(8).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(number).array();
	    } catch(NumberFormatException e) { 
	    	dataBytes = data.getBytes(Charset.forName("UTF-8"));
	    }
		result = new byte[uniqueId.length + dataBytes.length];
		System.arraycopy(uniqueId, 0, result, 0, uniqueId.length);
		System.arraycopy(dataBytes, 0, result, uniqueId.length, dataBytes.length);
		return result;
	}
	
	private byte[] generateUniqueID() {
		ByteBuffer resultBuffer = ByteBuffer.allocate(16).order(java.nio.ByteOrder.LITTLE_ENDIAN)
					.put(source.getAddress())
					.putShort((short)this.source_port)
					.put(new byte[2])
					.putLong(System.currentTimeMillis());
		return resultBuffer.array();
	}
	
	/*private boolean hasUniqueId(byte[] packet) {
		return Arrays.equals(Arrays.copyOfRange(packet, 0, 16), this.uniqueId); 
	}	*/
}	
