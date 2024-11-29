package com.archer.net;

import com.archer.net.ssl.SslContext;

public class ServerChannel {
	
	static {
		Library.loadDetectLibrary();
	}

	protected static native long init(ServerChannel server);

	protected static native void useThreadPool(long serverfd, int threadNum);
	
	protected static native boolean listen(long serverfd, byte[] host, int port);

	protected static native void close(long serverfd);
	
	protected void onAccept(long channelfd, byte[] host, int port) {
		Channel channel = new Channel(channelfd, host, port);
		if(handlerList != null) {
			channel.handlerList(handlerList);
			handlerList.onAccept(channel);
		}
	}
	
	protected void throwError(byte[] msg) {
		running = false;
		throw new ChannelException(new String(msg));
	}
	
	private long serverfd;
	private String host;
	private int port;
	
	private int threadNum;
	private ThreadPool pool;
	
	private SslContext sslCtx;
	private HandlerList handlerList;
	private ChannelFuture future;
	
	private volatile boolean running = false;
	
	public ServerChannel() {
		this(null);
	}
	
	public ServerChannel(SslContext sslCtx) {
		this.sslCtx = sslCtx;
	}
	
	public void setThreads(int threadNum) {
		//(main loop thread) + (child loop threads) = threadNum;
		this.threadNum = threadNum > 0 ? threadNum - 1 : 0;
		if(threadNum > 0) {
			this.pool = new ThreadPool(threadNum);
		}
	}
	
	public void handlerList(HandlerList handlerList) {
		this.handlerList = handlerList;
	}
	
	public HandlerList handlerList() {
		return handlerList;
	}
	
	public synchronized void listen(String host, int port) {
		if(running) {
			return ;
		}
		this.host = host;
		this.port = port;
		this.serverfd = init(this);
		if(threadNum > 0) {
			useThreadPool(serverfd, threadNum);
		}
		if(pool != null) {
			pool.start();
			handlerList.threadPool(pool);
		}
		if(Debugger.enableDebug()) {
			System.out.println("server listenning on " + port);
		}
		if(sslCtx != null) {
			if(sslCtx.isClientMode()) {
				throw new ChannelException("can not use a client-side sslcontext within server channel");
			}
			sslCtx.setSsl(serverfd);
		}

		this.future = new ChannelFuture(host+port) {
			public void apply() {
				running = true;
				listen(serverfd, host.getBytes(), port);
				running = false;
			}
		};
		this.future.start();
	}
	
	public synchronized void close() {
		if(!running) {
			return ;
		}
		running = false;
		close(serverfd);
		pool.stop();
	}
	
	protected long getServerfd() {
		return serverfd;
	}
}

