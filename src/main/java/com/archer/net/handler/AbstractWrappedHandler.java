package com.archer.net.handler;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;
import com.archer.net.message.Message;

public abstract class AbstractWrappedHandler<I extends Message> implements Handler {
	
	@Override
	public void onRead(ChannelContext ctx, Bytes in) {
		onMessage(ctx, decode(in));
	}

	@Override
	public void onWrite(ChannelContext ctx, Bytes out) {
		ctx.toLastOnWrite(out);
	}

	@Override
	public void onSslCertificate(ChannelContext ctx, byte[] cert) {
		// we do nothing here
	}
	
	public abstract void onMessage(ChannelContext ctx, I input);
	
	public abstract I decode(Bytes in);
	
}
