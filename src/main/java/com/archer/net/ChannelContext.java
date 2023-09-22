package com.archer.net;

import com.archer.log.Logger;
import com.archer.net.handler.Handler;
import com.archer.net.message.OutputMessage;

public class ChannelContext {
	
	Logger log = Debugger.getLogger();
	
	private Handler handler;
	
	private Channel channel;
	
	private ChannelContext last;

	private ChannelContext next;
	
	public ChannelContext(Handler handler, Channel channel) {
		this.handler = handler;
		this.channel = channel;
	}
	
	public void write(OutputMessage msg) {
		onWrite(msg.encode());
	}
	
	public void write(Bytes output) {
		onWrite(output);
	}
	
	public Channel channel() {
		return channel;
	}
	
	public void toNextOnAccept() {
		if(next != null) {
			next.onAccept();
		}
	}
	
	public void toNextOnConnect() {
		if(next != null) {
			next.onConnect();
		}
	}
	
	public void toNextOnDisconnect() {
		if(next != null) {
			next.onDisconnect();
		}
	}
	
	public void toNextOnRead(Bytes input) {
		if(next != null) {
			next.onRead(input);
		}
	}
	
	public void toLastOnWrite(Bytes output) {
		if(last != null) {
			last.onWrite(output);
		} else {
			if(channel.isActive()) {
				EventLoop.write(channel.getChannelfd(), output.readAll());
			} else {
				if(Debugger.enableDebug()) {
					log.warn("channel is not active, writting is not supported");
				}
			}
		}
	}
	
	public void toNextOnError(Throwable t) {
		if(next != null) {
			next.onError(t);
		}
	}
	
	public void toNextOnCertificate(byte[] cert) {
		if(next != null) {
			next.onSslCertificate(cert);
		}
	}
	
	protected void onAccept() {
		handler.onAccept(this);
	}
	
	protected void onConnect() {
		handler.onConnect(this);
	}
	
	protected void onDisconnect() {
		handler.onDisconnect(this);
	}
	
	protected void onRead(Bytes input) {
		handler.onRead(this, input);
	}

	protected void onWrite(Bytes output) {
		handler.onWrite(this, output);
	}
	
	protected void onError(Throwable t) {
		handler.onError(this, t);
	}
	
	protected void onSslCertificate(byte[] cert) {
		handler.onSslCertificate(this, cert);
	}
	
	protected ChannelContext next() {
		return next;
	}
	
	protected ChannelContext last() {
		return last;
	}
	
	
	protected void last(ChannelContext last) {
		this.last = last;
		if(last != null) {
			last.next = this;
		}
	}
	
}
