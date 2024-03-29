package com.archer.net.p2p;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import com.archer.net.Bytes;
import com.archer.net.Channel;
import com.archer.net.ChannelContext;
import com.archer.net.Debugger;
import com.archer.net.HandlerList;
import com.archer.net.ServerChannel;
import com.archer.net.ssl.SslContext;


public class P2PChannel {
	
	private Set<EndPoint> peers;
	
	private ServerChannel server;
	private Set<Channel> channels;
	private HandlerList handlerList;
	private SslContext sslCtx;
	
	private int port;
	private String host;

	
	public P2PChannel(String host, int port) {
		this(host, port, null);
	}
	
	public P2PChannel(String host, int port, SslContext sslCtx) {
		channels = new LinkedHashSet<>();
		peers = new LinkedHashSet<>();
		this.sslCtx = sslCtx;
		this.port = port;
		this.host = host;
	}
	
	public P2PChannel peers(EndPoint... peers) throws IOException {
		for(EndPoint peer: peers) {
			this.peers.add(peer);
		}
		return this;
	}
	
	public P2PChannel handlerList(HandlerList handlerList) {
		this.handlerList = handlerList;
		return this;
	}
	
	
	public void start() {
		if(handlerList == null) {
			handlerList = new HandlerList();
		}
		handlerList.addFirst(new P2PBaseHandler());
		server = new ServerChannel(sslCtx);
		server.handlerList(handlerList);
		server.listen(host, port);
		for(EndPoint peer: peers) {
			Channel ch = new Channel(sslCtx);
			ch.handlerList(handlerList);
			channels.add(ch);
			ch.connect(peer.host(), peer.port());
		}
	}
	
	public void stop() {
		server.close();
		for(Channel channel: channels) {
			channel.close();
		}
	}
	
	public void send(EndPoint peer, ChannelContext ctx, Bytes msg) throws Exception {
		for(Channel channel: channels) {
			if(EndPoint.toEndpointString(channel.remoteHost(), channel.remotePort()).equals(peer.enpoint())) {
				ctx.write(msg);
				return ;
			}
		}
		if(Debugger.enableDebug()) {
			System.err.println("can not found peer channel " + peer.enpoint() + " while send message");
		}
	}
	
	public void disconnect(EndPoint peer) throws Exception {
		Channel theChannel = null;
		for(Channel channel: channels) {
			if(EndPoint.toEndpointString(channel.remoteHost(), channel.remotePort()).equals(peer.enpoint())) {
				theChannel = channel;
				break;
			}
		}
		if(theChannel != null) {
			theChannel.close();
			channels.remove(theChannel);
		}
	}
}
