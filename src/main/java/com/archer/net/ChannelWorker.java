package com.archer.net;

abstract class ChannelWorker extends Thread {

	public ChannelWorker(String name) {
		super(name);
	}
	
	@Override
	public void run() {
		apply();
	}
	
	public abstract void apply();

}
