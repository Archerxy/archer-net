package com.archer.net.handler;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;

public interface Handler {
	
	void onAccept(ChannelContext ctx);
	
	void onConnect(ChannelContext ctx);
	
	void onDisconnect(ChannelContext ctx);
	
	void onRead(ChannelContext ctx, Bytes input);

	void onWrite(ChannelContext ctx, Bytes output);
	
	void onError(ChannelContext ctx, Throwable t);
	
	void onSslCertificate(ChannelContext ctx, byte[] cert);
}
