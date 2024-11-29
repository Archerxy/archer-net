package com.archer.net.p2p;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;

public class PeerChannel {
	
	private String host;
	private int port;
	
	private boolean isConnected = false;
	
	private ChannelContext serverCtx;
	private ChannelContext clientCtx;
	
	protected PeerChannel(ChannelContext clientCtx) {
		this.clientCtx = clientCtx;
		this.host = clientCtx.channel().remoteHost();
		this.port = clientCtx.channel().remotePort();
	}
	
	protected PeerChannel(ChannelContext serverCtx, int port) {
		this.serverCtx = serverCtx;
		this.host = serverCtx.channel().remoteHost();
		this.port = port;
	}
	
	protected void removeChannelContext() {
		this.clientCtx = null;
		this.serverCtx = null;
	}

	public void close() {
		if(this.clientCtx != null) {
			this.clientCtx.close();
		}
		if(this.serverCtx != null) {
			this.serverCtx.close();
		}
		removeChannelContext();
	}
	
	
	public void send(Bytes data) {
		if(clientCtx == null || !clientCtx.channel().isActive()) {
			if(serverCtx != null && serverCtx.channel().isActive()) {
				serverCtx.write(data);
			} else {
				throw new P2PException("remote " + host+ ":" + port + " disconnected before write.");
			}
		} else {
			clientCtx.write(data);
		}
	}
	
	public String getRemoteHost() {
		return host;
	}
	
	public int getRemotePort() {
		return port;
	}
	
	protected synchronized boolean getAndSetConnected() {
		boolean ret = true;
		if(!isConnected) {
			ret = false;
		}
		isConnected = true;
		return ret;
	}
}
