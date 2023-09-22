package com.archer.net;

abstract class EventLoopFuture extends Thread {
	
	public EventLoopFuture(String name) {
		super(name);
	}
	
	@Override
	public void run() {
		apply();
	}
	
	public abstract void apply();

}
