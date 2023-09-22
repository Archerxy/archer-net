package com.archer.net.p2p;

public class EndPoint {
	
	private String host;
	
	private int port;

	public EndPoint() {}

	public EndPoint(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public EndPoint host(String host) {
		this.host = host;
		return this;
	}
	
	public EndPoint port(int port) {
		this.port = port;
		return this;
	}
	
	public String host() {
		return host;
	}
	
	public int port() {
		return port;
	}
	
	public String enpoint() {
		return toEndpointString(host, port);
	}
	
	public static String toEndpointString(String host, int port) {
		return host +":" + port;
	}
}
