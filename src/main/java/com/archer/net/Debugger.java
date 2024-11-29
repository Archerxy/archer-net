package com.archer.net;

public class Debugger {
	
	private static volatile boolean EN_DEBUG;
	
	public static boolean enableDebug() {
		return EN_DEBUG;
	}
	
	public static void setMode(boolean enableDebug) {
		EN_DEBUG = enableDebug;
	}
}
