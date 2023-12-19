package com.archer.net;


public class ServerChannel {
	
	static {
		Library.loadDetectLibrary();
	}

	protected static native long init(ServerChannel server, boolean ssl);

	protected static native boolean listen(long serverfd, long loopfd, int jport);

	protected static native void close(long serverfd);
	
	private long serverfd;
	private int port;
	
	private boolean useSsl;
	private SslContext sslCtx;
	
	private EventLoopFuture future;
	
	private EventLoop loop;
	
	private volatile boolean running = false;
	
	public ServerChannel() {
		this(null);
	}
	
	public ServerChannel(SslContext sslCtx) {
		if(sslCtx == null) {
			this.useSsl = false;
		} else {
			this.useSsl = true;
		}
		this.sslCtx = sslCtx;
	}
	
	private void initServerfd() {
		this.serverfd = init(this, useSsl);
	}
	
	public void eventLoop(EventLoop loop) {
		if(this.loop != null) {
			throw new ChannelException("can not set EventLoop twice.");
		}
		this.loop = loop;
	}
	
	public synchronized void listen(int port) {
		if(running) {
			return ;
		}
		this.port = port;
		if(loop == null) {
			throw new ChannelException("set EventLoop before start ServerChannel");
		}
		if(Debugger.enableDebug()) {
			System.out.println("server listenning on " + port);
		}
		initServerfd();
		if(useSsl) {
			sslCtx.setSsl(serverfd, false);
		}
		if(!loop.running()) {
			loop.init();
		}
		loop.eventAdd();
		if(!listen(serverfd, loop.getLoopfd(), port)) {
			return ;
		}
		loop.startLoop();
		running = true;
	}
	
	public synchronized void close() {
		if(!running) {
			return ;
		}
		running = false;
		close(serverfd);
		loop.eventRemove();
	}
	
	public boolean useSsl() {
		return useSsl;
	}
	
	protected long getServerfd() {
		return serverfd;
	}
	
	protected void throwError(byte[] msg) {
		//native server will be closed when throwing exception 
		running = false;
		throw new ChannelException(new String(msg));
	}
}
