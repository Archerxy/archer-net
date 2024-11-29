package com.archer.net.http;

import com.archer.net.HandlerList;
import com.archer.net.ServerChannel;
import com.archer.net.ssl.SslContext;

public class HttpServer {
	
	private SslContext sslCtx;
	private ServerChannel server;
	private int threadNum;
	
	public HttpServer() {
		this(null);
	}
	
	public HttpServer(SslContext sslCtx) {
		this.sslCtx = sslCtx;
	}
	
	public void setThreadNum(int threadNum) {
		this.threadNum = threadNum;
	}
	
	public void listen(String host, int port, HttpWrappedHandler handler) throws HttpServerException {
		HandlerList handlerList = new HandlerList();
		handlerList.add(handler);
		if(sslCtx != null) {
			if(sslCtx.crt() == null || sslCtx.key() == null) {
				throw new HttpServerException("certificate and privateKey is required");
			}
			server = new ServerChannel(sslCtx);
		} else {
			server = new ServerChannel();
		}
		server.handlerList(handlerList);
		if(threadNum > 0) {
			server.setThreads(threadNum);
		}
		server.listen(host, port);
	}
	
	public void destroy() {
		server.close();
	}
}


