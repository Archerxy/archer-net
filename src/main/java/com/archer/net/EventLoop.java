package com.archer.net;

import java.util.concurrent.atomic.AtomicBoolean;

import com.archer.log.Logger;
import com.archer.net.handler.Handler;

public class EventLoop {
	
	private static final Logger log = Debugger.getLogger();
	
	private static final String T_NAME = "event_loop";
	
	static {
		Library.loadDetectLibrary();
	}
	
	protected static native long init(EventLoop loop);
	
	protected static native void run(long loopfd);
	
	protected static native void write(long channelfd, byte[] data);
	

    private ChannelContextContainer container;
    
    private HandlerList handlerList;
	
	private EventThreadPool pool;
	
	private EventLoopFuture future;
	
	private long loopfd;
	
	private int threadNum;
	
	private AtomicBoolean run = new AtomicBoolean(false);
	
	public EventLoop() {
		this(-1);
	}
	
	public EventLoop(int threadNum) {
		this.threadNum = threadNum;
    	this.container = new ChannelContextContainer();
    	this.handlerList = new HandlerList();
	}
	
	
	public boolean running() {
		return run.get();
	}
	
	protected void init() {
		this.loopfd = init(this);
	}
	
	protected void startLoop() {
		if(run.getAndSet(true)) {
			return ;
		}
		if(threadNum > 0 && !this.pool.isAlive()) {
			this.pool = new EventThreadPool(threadNum);
		}
		if(Debugger.enableDebug()) {
			log.info("event loop starting = " + run.get());
		}
		if(this.future == null || !this.future.isAlive()) {
			newEventLoopFuture();
		}
		if(Debugger.enableDebug()) {
			log.info("event loop start");
		}
		this.future.start();
	}
	
	protected long getLoopfd() {
		return loopfd;
	}
	
	protected void eventAdd() {
		if(pool != null) {
			pool.referenceIncrease();
		}
	}
	
	protected void eventRemove() {
		if(pool != null) {
			pool.referenceDecrease();
		}
	}
	
	protected void newEventLoopFuture() {
		this.future = new EventLoopFuture(T_NAME) {
			@Override
			public void apply() {
				EventLoop.run(loopfd);
				loopfd = 0;
				run.set(false);
				EventLoop.this.future = null;
				if(Debugger.enableDebug()) {
					log.info("event loop exits");
				}
			}};
	}
	
	/******** below methods will be called by native codes *********/
	
	
	protected void onAccept(long channelfd, byte[] host, int port, boolean ssl) {
		Channel channel = new Channel(channelfd, host, port, ssl);
		ChannelContext ctx = findChannelContext(channel);
		if(this.pool != null) {
			pool.execute(ctx, ChannelContextAction.ON_ACCEPT, null);
		} else {
			try {
				ctx.onAccept();
			} catch(Exception e) {
				ctx.onError(e);
			}	
		}
	}
	
	protected void onConnect(Channel channel) {
		channel.setActive(true);
		ChannelContext ctx = findChannelContext(channel);
		if(this.pool != null) {
			pool.execute(ctx, ChannelContextAction.ON_CONNECT, null);
		} else {
			try {
				ctx.onConnect();
			} catch(Exception e) {
				ctx.onError(e);
			}	
		}
	}
	
	protected void onRead(Channel channel, byte[] data) {
		ChannelContext ctx = findChannelContext(channel);
		if(this.pool != null) {
			pool.execute(ctx, ChannelContextAction.ON_READ, new Bytes(data));
		} else {
			try {
				ctx.onRead(new Bytes(data));
			} catch(Exception e) {
				ctx.onError(e);
			}			
		}
	}
	
	protected void onDisconnect(Channel channel) {
		channel.setActive(false);
		ChannelContext ctx = findChannelContext(channel);
		container.remove(channel);
		if(this.pool != null) {
			pool.execute(ctx, ChannelContextAction.ON_DISCONNECT, null);
		} else {
			try {
				ctx.onDisconnect();
			} catch(Exception e) {
				ctx.onError(e);
			}	
		}
	}

	protected void onError(Channel channel, byte[] msg) {
		ChannelContext ctx = findChannelContext(channel);
		if(this.pool != null) {
			pool.executeOnError(ctx, new ChannelException(new String(msg)));
		} else {
    		try {
    			ctx.onError(new ChannelException(new String(msg)));
    		} catch(Exception e) {
    			System.err.println("the last handler did not handle the exception.");
    			e.printStackTrace();
    		}
		}
	}
	
	protected void onCertCallback(Channel channel, byte[] cert) {
		ChannelContext ctx = findChannelContext(channel);
		try {
			ctx.onSslCertificate(cert);
		} catch(Exception ignore) {}
	}
	

	/******** above methods will be called by native codes *********/

    protected ChannelContext findChannelContext(Channel ch) {
    	ChannelContext ctx = container.findChannelContext(ch);
    	if(ctx  == null) {
    		return container.init(ch, handlerList);
    	}
    	return ctx;
    }

    public void addHandlers(Handler ...handlers) {
    	if(handlers == null || handlers.length <= 0) {
    		return ;
    	}
    	handlerList.add(handlers);
    }
    
    public void addFirst(Handler handler) {
    	if(handler == null) {
    		return ;
    	}
    	handlerList.addFirst(handler);
    }
    
    public void removeHandler(int index) {
    	if(index < 0) {
    		return ;
    	}
    	handlerList.remove(index);
    }
    
    public void insert(int index, Handler handler) {
    	handlerList.insert(index, handler);
    }
	
}