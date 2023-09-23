package com.archer.net;

import java.nio.charset.StandardCharsets;

import com.archer.log.Logger;

public class Channel {
	
	private static final Logger log = Debugger.getLogger();
	
	static {
		Library.loadDetectLibrary();
	}

	protected static native long init(Channel channel, boolean ssl);

	protected static native long setChannel(long channelfd, Channel channel);

	protected static native void connect(long loopfd, long channelfd, byte[] host, int port);

	protected static native void validateHostname(long channelfd, byte[] hostname);

	protected static native void close(long channelfd);
	
	/*******above are native methods********/
	
	private long channelfd;
	private volatile boolean active;
	
	private boolean clientSide;
	
	private String host;
	private int port;
	
	private boolean useSsl;
	private SslContext sslCtx;
	
	private EventLoopFuture future;
	private EventLoop loop;
		public Channel() {
		this(null);
	}
		public Channel(SslContext sslCtx) {		
		if(sslCtx == null) {
			this.useSsl = false;
		} else {
			this.useSsl = true;
		}
		this.sslCtx = sslCtx;
		this.clientSide = true;
	}
	
	/**
	 * for server connected channel
	 * */
	protected Channel(long channelfd, byte[] host, int port, boolean useSsl) {
		this.channelfd = channelfd;
		this.useSsl = useSsl;
		this.port = port;
		this.active = true;
		this.clientSide = false;
		this.host = new String(host);
		setChannel(channelfd, this);
	}
	
	private synchronized void initChannelfd() {
		if(Debugger.enableDebug()) {
			log.info("initializing channel");
		}
		this.channelfd = init(this, useSsl);
	}

	public void eventLoop(EventLoop loop) {
		if(this.loop != null) {
			throw new ChannelException("can not set EventLoop twice.");
		}
		this.loop = loop;
	}
	
	public EventLoop handlerLoop() {
		return loop;
	}
	
	public Channel validateHostname(String hostname) {
		if(useSsl) {
			validateHostname(channelfd, hostname.getBytes(StandardCharsets.UTF_8));
		}
		return this;
	}
	
	public synchronized void connect(String host, int port) {
		if(active) {
			return ;
		}
		this.host = host;
		this.port = port;
		if(Debugger.enableDebug()) {
			log.info("starting connect to {}:{}", host, port);
		}
		if(loop == null) {
			throw new ChannelException("set EventLoop before start Channel");
		}
		initChannelfd();
		if(useSsl) {
			sslCtx.setSsl(channelfd, true);
		}
		if(!loop.running()) {
			loop.init();
		}
		loop.eventAdd();
		connect(loop.getLoopfd(), channelfd, host.getBytes(), port);
		loop.startLoop();
		active = true;
	}
	
	public synchronized void close() {
		if(!active) {
			return ;
		}
		active = false;
		close(channelfd);
		if(clientSide) {
			loop.eventRemove();
		}
	}
	
	public String remoteHost() {
		return host;
	}
	
	public int remotePort() {
		return port;
	}
	
	public boolean isActive() {
		return active;
	}
	
	public boolean isClientSide() {
		return clientSide;
	}
	
	protected void setActive(boolean active) {
		this.active = active;
	}
	
	protected long getChannelfd() {
		return channelfd;
	}
	
	protected void throwError(byte[] msg) {
		//native channel will be closed when throwing exception 
		active = false;
		throw new ChannelException(new String(msg));
	}
}
