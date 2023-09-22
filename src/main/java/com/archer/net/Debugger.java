package com.archer.net;

import com.archer.log.LogProperties;
import com.archer.log.Logger;

public class Debugger {
	
	private static final Logger log;
	private static volatile boolean EN_DEBUG;
	private static String LOGGER_NAME = "ACHER_NET_LOG";
	
	static {
		LogProperties properties = new LogProperties().level("DEBUG");
		log = Logger.getLoggerAndSetPropertiesIfNotExits(LOGGER_NAME, properties);
	}
	
	public static Logger getLogger() {
		return log;
	}
	
	public static boolean enableDebug() {
		return EN_DEBUG;
	}
	
	public static void setMode(boolean enableDebug) {
		EN_DEBUG = enableDebug;
	}
}
