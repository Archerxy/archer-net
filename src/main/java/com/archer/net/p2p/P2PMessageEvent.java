package com.archer.net.p2p;

import com.archer.net.Bytes;

public interface P2PMessageEvent {
	
	void onConnect(PeerChannel channel);
	
	void onMessage(PeerChannel channel, Bytes data);
	
	void onDisconnect(PeerChannel channel);
	
	void onError(PeerChannel channel, Throwable t);
	
}

