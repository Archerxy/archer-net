package com.archer.net;

import com.archer.net.ssl.SslContext;

public class Channel {
	
	static {
		Library.loadDetectLibrary();
	}

	protected static native long init(Channel channel);

	protected static native void setChannel(long channelfd, Channel channel);

	protected static native void write(long channelfd, byte[] data);

	protected static native boolean connect(long channelfd, byte[] host, int port);

	protected static native void close(long channelfd);
	
	protected void onConnect() {
		if(handlerList != null) {
			handlerList.onConnect(this);
		}
	}
	protected void onRead(byte[] data) {
		if(handlerList != null) {
			handlerList.onRead(this, data);
		}
	}
	protected void onDisconnect() {
		if(handlerList != null) {
			handlerList.onDisconnect(this);
		}
	}
	protected void onError(byte[] msg) {
		if(handlerList != null) {
			handlerList.onError(this, msg);
		}
	}
	protected void onCertCallback(byte[] crt) {
		if(handlerList != null) {
			handlerList.onCertCallback(this, crt);
		}
	}
	
	protected void throwError(byte[] msg) {
		active = false;
		throw new ChannelException(new String(msg));
	}
	
	
	/*******above are native methods********/
	
	private static final long TIMEOUT = 3500;
	
	private long channelfd;
	private volatile boolean active;
	
	private boolean clientSide;
	
	private String host;
	private int port;
	private SslContext sslCtx;
	private HandlerList handlerList;
	private ChannelFuture future;
		public Channel() {
		this(null);
	}
		public Channel(SslContext sslCtx) {
		this.sslCtx = sslCtx;
		this.clientSide = true;
	}
	
	/**
	 * for server connected channel
	 * */
	protected Channel(long channelfd, byte[] host, int port) {
		this.channelfd = channelfd;
		this.port = port;
		this.active = true;
		this.clientSide = false;
		this.host = new String(host);
		setChannel(channelfd, this);
	}
	
	private synchronized void initChannelfd() {
		if(Debugger.enableDebug()) {
			System.out.println("initializing channel");
		}
		this.channelfd = init(this);
	}

	public void handlerList(HandlerList handlerList) {
		this.handlerList = handlerList;
	}
	
	public HandlerList handlerList() {
		return handlerList;
	}
	
	public synchronized void connect(String host, int port) {
		if(active) {
			return ;
		}
		this.host = host;
		this.port = port;
		
		if(Debugger.enableDebug()) {
			System.out.println("starting connect to " + host + ":" + port);
		}
		System.out.println("java connect to " + host+":"+port + " init fd");
		initChannelfd();
		if(sslCtx != null) {
			if(!sslCtx.isClientMode()) {
				throw new ChannelException("can not use a server-side sslcontext within client channel");
			}
			System.out.println("java connect to " + host+":"+port + " set ssl");
			sslCtx.setSsl(channelfd);
		}
		this.future = new ChannelFuture(host+port) {
			public void apply() {
				System.out.println("java connect to " + host+":"+port + " start");
				active = true;
				connect(channelfd, host.getBytes(), port);
				active = false;
			}
		};
		this.future.start();
	}
	
	public void write(byte[] data) {
		write(channelfd, data);
	}
	
	public synchronized void close() {
		if(!active) {
			return ;
		}
		active = false;
		close(channelfd);
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
}
