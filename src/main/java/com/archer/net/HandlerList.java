package com.archer.net;

import com.archer.net.handler.Handler;
import com.archer.net.handler.HandlerException;

public final class HandlerList {
    private static final int DEFAULT_SIZE = 16;

    private ChannelContextContainer container;
	private Handler[] handlers = new Handler[DEFAULT_SIZE];
	private int pos = 0;
	
	private ThreadPool pool = null;
	
	public HandlerList() {
    	this.container = new ChannelContextContainer();
	}
	public HandlerList(ThreadPool pool) {
		this.pool = pool;
    	this.container = new ChannelContextContainer();
	}
	
	public HandlerList threadPool(ThreadPool pool) {
		this.pool = pool;
		return this;
	}
	
	public void addFirst(Handler handler) {
		if(handler == null) {
			return ;
		}
		insert(0, handler);
	}
	
	public void add(Handler ...handlers) {
		if(handlers == null || handlers.length <= 0) {
			return ;
		}
		checkCap(handlers.length);
		System.arraycopy(handlers, 0, this.handlers, pos, handlers.length);
		pos += handlers.length;
	}
	
	public void insert(int index, Handler handler) {
		if(handler == null) {
			return ;
		}
		checkCap(1);
		System.arraycopy(handlers, index, handlers, index + 1, pos - index);
		handlers[index] = handler;
		pos++;
	}
	
	public void remove(int index) {
		if(0 <= index && index < pos) {
			System.arraycopy(handlers, index + 1, handlers, index, pos - index - 1);
			pos--;
		}
	}
	
	public Handler at(int index) {
		if(index < 0 || index >= pos) {
			throw new HandlerException("index " +index+ " out of range 0 - " + pos);
		}
		return handlers[index];
	}
	
	public int handlerCount() {
		return pos;
	}
	
	private void checkCap(int inputSize) {
		if(inputSize + pos > handlers.length) {
			int size = handlers.length << 1;
			while(size < handlers.length + pos) {
				size <<= 1;
			}
			Handler[] tmp = new Handler[size];
			System.arraycopy(this.handlers, 0, tmp, 0, pos);
			handlers = tmp;
		}
	}
	
	
	protected void onAccept(Channel channel) {
		ChannelContext ctx = findChannelContext(channel);
		try {
			ctx.onAccept();
		} catch(Exception e) {
			ctx.onError(e);
		}
	}
	
	protected void onConnect(Channel channel) {
		channel.setActive(true);
		ChannelContext ctx = findChannelContext(channel);
		try {
			ctx.onConnect();
		} catch(Exception e) {
			ctx.onError(e);
		}
	}
	
	protected void onRead(Channel channel, byte[] data) {
		ChannelContext ctx = findChannelContext(channel);
		if(pool != null) {
			pool.submit(new Bytes(data), ctx);
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
		try {
			ctx.onDisconnect();
		} catch(Exception e) {
			ctx.onError(e);
		}	
	}

	protected void onError(Channel channel, byte[] msg) {
		channel.setActive(false);
		ChannelContext ctx = findChannelContext(channel);
		String errMsg = new String(msg).trim();
		try {
			ctx.onError(new ChannelException(errMsg));
		} catch(Exception e) {
			System.err.println("the last handler did not handle the exception.");
			e.printStackTrace();
		}
	}
	
	protected void onCertCallback(Channel channel, byte[] cert) {
		ChannelContext ctx = findChannelContext(channel);
		try {
			ctx.onSslCertificate(cert);
		} catch(Exception ignore) {}
	}

    protected ChannelContext findChannelContext(Channel ch) {
    	ChannelContext ctx = container.findChannelContext(ch);
    	if(ctx  == null) {
    		return container.init(ch, this);
    	}
    	return ctx;
    }

}
