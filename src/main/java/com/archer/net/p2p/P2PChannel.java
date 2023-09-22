package com.archer.net.p2p;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import com.archer.log.Logger;
import com.archer.net.Bytes;
import com.archer.net.Channel;
import com.archer.net.ChannelContext;
import com.archer.net.Debugger;
import com.archer.net.EventLoop;
import com.archer.net.ServerChannel;
import com.archer.net.SslContext;


public class P2PChannel {
	
	private static final Logger log = Debugger.getLogger();
	
	private Set<EndPoint> peers;
	
	private ServerChannel server;
	private Set<Channel> channels;
	
	private SslContext sslCtx;
	private int port;
	
	private EventLoop eventLoop;
	
	public P2PChannel(int port) {
		this(port, null);
	}
	
	public P2PChannel(int port, SslContext sslCtx) {
		channels = new LinkedHashSet<>();
		peers = new LinkedHashSet<>();
		this.sslCtx = sslCtx;
		this.port = port;
	}
	
	public P2PChannel peers(EndPoint... peers) throws IOException {
		for(EndPoint peer: peers) {
			this.peers.add(peer);
		}
		return this;
	}
	
	public P2PChannel eventLoop(EventLoop loop) {
		this.eventLoop = loop;
		return this;
	}
	
	
	public void start() {
		if(eventLoop == null) {
			eventLoop = new EventLoop();
		}
		eventLoop.addFirst(new P2PBaseHandler());
		server = new ServerChannel(sslCtx);
		server.eventLoop(eventLoop);
		server.listen(port);
		for(EndPoint peer: peers) {
			Channel ch = new Channel(sslCtx);
			ch.eventLoop(eventLoop);
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
			log.warn("can not found peer channel {} while send message", peer.enpoint());
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
