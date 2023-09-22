package com.archer.net;

import com.archer.net.handler.Handler;
import com.archer.net.handler.HandlerException;

final class HandlerList {
    private static final int DEFAULT_SIZE = 16;
	
	private Handler[] handlers = new Handler[DEFAULT_SIZE];
	private int pos = 0;
	
	protected HandlerList() {}
	
	protected void addFirst(Handler handler) {
		if(handler == null) {
			return ;
		}
		insert(0, handler);
	}
	
	protected void add(Handler ...handlers) {
		if(handlers == null || handlers.length <= 0) {
			return ;
		}
		checkCap(handlers.length);
		System.arraycopy(handlers, 0, this.handlers, pos, handlers.length);
		pos += handlers.length;
	}
	
	protected void insert(int index, Handler handler) {
		if(handler == null) {
			return ;
		}
		checkCap(1);
		System.arraycopy(handlers, index, handlers, index + 1, pos - index);
		handlers[index] = handler;
		pos++;
	}
	
	protected void remove(int index) {
		if(0 <= index && index < pos) {
			System.arraycopy(handlers, index + 1, handlers, index, pos - index - 1);
			pos--;
		}
	}
	
	protected Handler at(int index) {
		if(index < 0 || index >= pos) {
			throw new HandlerException("index " +index+ " out of range 0 - " + pos);
		}
		return handlers[index];
	}
	
	protected int handlerCount() {
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
}
