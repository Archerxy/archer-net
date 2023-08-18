package com.archer.net;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.archer.net.utils.Library;

public class ServerChannel {

	private static final String DEFAULT_ANME = "ServerChannel";
	private static final String WIN_LIB = "lib/libarnet.dll";
	private static final String LINUX_LIB = "lib/libarnet.so";
	
	static {
		if(!Files.exists(Paths.get("E:/projects/javaProject/maven-package/archer-net/src/main/resources/lib/libarnet.dll"))) {
			System.out.println("file not exists.");
		}
		Library.loadLib("E:/projects/javaProject/maven-package/archer-net/src/main/resources/lib/libarnet.dll");
	}
	
	protected static void onConnect(byte[] ip, int port, long channelfd) {
		String ipStr = new String(ip);
		System.out.println(ipStr+":"+port+" connected.");
	}
	
	protected static void onRead(byte[] data, long channelfd) {
		System.out.println("receive: " + new String(data));
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return ;
		}
		write("send data".getBytes(), channelfd);
	}
	
	protected static void onDisconnect(long channelfd) {
		System.out.println("dis connected.");
	}
	
	protected static void onError(byte[] msg) {
		System.err.println("ERROR: " + new String(msg));
	}
	
	protected static native long init();
	
	protected static native void listen(int port, long serverfd);
	
	protected static native void close(long serverfd);
	
	protected static native void write(byte[] data, long channelfd);
	
	protected static native void closeChannel(long channelfd);
	
	
	private long serverfd;
	private int port;
	private ChannelWorker worker;
	
	public ServerChannel() {
		this.serverfd = init();
		this.worker = new ChannelWorker(DEFAULT_ANME) {
			@Override
			public void apply() {
				listen(port, serverfd);
			}
		};
	}
	
	public void listen(int port) {
		this.port = port;
		this.worker.start();
	}
}
