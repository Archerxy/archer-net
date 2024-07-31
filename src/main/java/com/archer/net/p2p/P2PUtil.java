package com.archer.net.p2p;

import com.archer.net.Channel;
import com.archer.net.ChannelContext;

public class P2PUtil {
	
	public static String getEndpoint(ChannelContext ctx) {
		return ctx.channel().remoteHost() + ":" + ctx.channel().remotePort();
	}

	public static String getEndpoint(Channel ch) {
		return ch.remoteHost() + ":" + ch.remotePort();
	}

	public static String getEndpoint(PeerChannel ch) {
		return ch.getRemoteHost() + ":" + ch.getRemotePort();
	}
}

