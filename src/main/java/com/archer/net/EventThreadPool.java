package com.archer.net;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class EventThreadPool {

	private static final String T_NAME = "archer-thread-";
    private static final int KEEP_ALIVE_TIME = 10_000;
	
	private ThreadPoolExecutor pool = null;
    private AtomicInteger refCount = new AtomicInteger(0);
    
    public EventThreadPool(int threadNum) {
    	this(threadNum, KEEP_ALIVE_TIME);
    }
    
    public EventThreadPool(int threadNum, long keepAlive) {
		this.pool = new ThreadPoolExecutor(
				threadNum,
				threadNum,
				keepAlive,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new NamedThreadFactory(T_NAME));
    }
    
    protected boolean isAlive() {
    	return !pool.isTerminated();
    }
    
    protected void execute(ChannelContext ctx, ChannelContextAction action, Bytes data) {
    	pool.execute(() -> {
    		try {
    			switch(action) {
    			case ON_ACCEPT: {
    				ctx.onAccept();
    				break ;
    			}
    			case ON_CONNECT: {
    				ctx.onConnect();
    				break ;
    			}
    			case ON_READ: {
    				ctx.onRead(data);
    				break ;
    			}
    			case ON_DISCONNECT: {
    				ctx.onDisconnect();
    				break ;
    			}
				default:
					break;
    			}
    		} catch(Exception e) {
    			ctx.onError(e);
    		}
    	});
    }
    
    protected void executeOnError(ChannelContext ctx, Throwable t) {
    	pool.execute(() -> {
    		try {
        		ctx.onError(t);
    		} catch(Exception ignore) {}
    	});
    }
    
    protected void referenceIncrease() {
    	refCount.incrementAndGet();
    }
    
    protected void referenceDecrease() {
    	int refs = refCount.decrementAndGet();
    	if(refs <= 0 && pool != null) {
    		pool.shutdown();
    	}
    }
}
