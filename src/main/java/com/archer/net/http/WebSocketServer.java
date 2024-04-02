package com.archer.net.http;

import com.archer.net.HandlerList;
import com.archer.net.ServerChannel;
import com.archer.net.ssl.SslContext;

public class WebSocketServer {
	
	private SslContext sslCtx;
	private ServerChannel server;
	
	public WebSocketServer() {
		this(null);
	}
	
	public WebSocketServer(SslContext sslCtx) {
		this.sslCtx = sslCtx;
	}
	
	public void listen(String host, int port, String uri, WebSocketHandler handler) throws HttpServerException {
		handler.setUri(uri);
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
		server.listen(host, port);
	}
	
	public void destroy() {
		server.close();
	}
}
