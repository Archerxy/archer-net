package com.archer.net;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {
	
	private String namePrefix;
	
	public NamedThreadFactory(String namePrefix) {
		this.namePrefix = namePrefix;
	} 
	
	@Override
	public Thread newThread(Runnable r) {
		return new Thread(namePrefix);
	}

}
