package com.archer.net.http;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;
import com.archer.net.handler.Handler;
import com.archer.net.util.Sha1Util;

public abstract class WebSocketHandler implements Handler {
	
	private static final String MAGIC_NUMBER = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	private static final String Sec_WebSocket_Key = "Sec-WebSocket-Key";
	private static final String Sec_WebSocket_Accept = "Sec-WebSocket-Accept";
	private static final String Sec_WebSocket_Version = "Sec-WebSocket-Version";
	private static final String Upgrade = "Upgrade";
	private static final String Upgrade_Val = "websocket";
	private static final String Connection = "Connection";
	private static final String Connection_Val = "Upgrade";
	
	private static ConcurrentHashMap<ChannelContext, WebSocketChannel> wsChannelCache = new ConcurrentHashMap<>();
    
	private String uri;
	
    public WebSocketHandler() {}
    
    protected void setUri(String uri) {
    	this.uri = uri;
    }
    
	@Override
	public void onAccept(ChannelContext ctx) {
		//nothing
	}
    
	@Override
	public void onConnect(ChannelContext ctx) {
		WebSocketChannel wsChannel = getWebSocketChannel(ctx, true);
		wsChannelCache.put(ctx, wsChannel);
	}

	@Override
	public void onRead(ChannelContext ctx, Bytes in) {
		if(in.available() <= 0) {
			return ;
		}
		
		WebSocketChannel wsChannel = getWebSocketChannel(ctx, true);
		HttpRequest req = wsChannel.request;
		HttpResponse res = wsChannel.response;
		
		wsChannel.lock.lock();
		try {
			if(wsChannel.handshakeDone) {
				if(wsChannel.parseWebSocketMessage(in, wsChannel.input)) {
					onMessage(wsChannel, wsChannel.resetAndGet());
				} else {
					ctx.close();
					onClose(wsChannel);
				}
			} else {
				byte[] msg = in.readAll();
				if(req.isEmpty()) {
					try {
						req.parse(msg);
					} catch(HttpException e) {
						res.setStatus(HttpStatus.valueOf(e.getCode()));
						onWrite(ctx, new Bytes(res.toBytes()));
					}
				} else {
					req.putContent(msg);
				}
				if(req.isFinished()) {
					boolean ok = setWsResponse(req, res);
					ctx.toLastOnWrite(new Bytes(res.toBytes()));
					if(!ok) {
						ctx.channel().close();
						return ;
					}
					wsChannel.handshakeDone = true;
				}
			}
		} catch(Exception e) {
			onError(ctx, e);
		} finally {
			wsChannel.lock.unlock();
		}
	}
	
	@Override
	public void onWrite(ChannelContext ctx, Bytes out) {}
	
	@Override
	public void onDisconnect(ChannelContext ctx) {
		WebSocketChannel wsChannel = getWebSocketChannel(ctx, false);
		if(null != wsChannel) {
			try {
				onClose(wsChannel);
			} catch(Exception ignore) {}
		}
		wsChannelCache.remove(ctx);
	}

	@Override
	public void onError(ChannelContext ctx, Throwable t) {
		WebSocketChannel wsChannel = getWebSocketChannel(ctx, true);
		try {
			onError(wsChannel, t);
		} catch(Exception ignore) {}
	}
	
	@Override
	public void onSslCertificate(ChannelContext ctx, byte[] cert) {
		//we do nothing here
	}
	
	private WebSocketChannel getWebSocketChannel(ChannelContext ctx, boolean create) {
		WebSocketChannel wsChannel = wsChannelCache.getOrDefault(ctx, null);
		if(create && wsChannel == null) {
			wsChannel = new WebSocketChannel(ctx);
		}
		return wsChannel;
	}
	

	private boolean websocketUriMatch(String hostname, String pattern) {
	    int hostname_len = hostname.length(), pattern_len = pattern.length(); 
	    if(pattern.indexOf('*') < 0) {
	        if(hostname_len != pattern_len) {
	            return false;
	        }
	        for(int i = 0; i < pattern_len; i++) {
	        	if(hostname.charAt(i) != pattern.charAt(i)) {
	        		return false;
	        	}
	        }
	        return true;
	    }
	    if(hostname_len < pattern_len) {
	        return false;
	    }
	    int h = 0, star = 0;
	    for(int i = 0; i < pattern_len; ++i, ++h) {
	        if(hostname_len - h < pattern_len - i) {
	            return false;
	        }
	        if(pattern.charAt(i) == '*') {
	            star = 1;
	            if(i >= pattern_len - 1) {
	                return true;
	            }
	            ++i;
	            while(hostname.charAt(h) != pattern.charAt(i)) {
	                ++h;
	                if(h >= hostname_len) {
	                    return false;
	                }
	            }
	        } else if(hostname.charAt(h) != pattern.charAt(i)) {
	            if(star == 1 && h < hostname_len - 1) {
	                ++h;
	                while(hostname.charAt(h) != pattern.charAt(i)) {
	                    ++h;
	                    if(h >= hostname_len) {
	                        return false;
	                    }
	                }
	            } else {
	                return false;
	            }
	        } 
	    }
	    return true;
	}

	
	public abstract void onConnected(WebSocketChannel wsChannel);
	public abstract void onMessage(WebSocketChannel wsChannel, Bytes input);
	public abstract void onError(WebSocketChannel wsChannel, Throwable t);
	public abstract void onClose(WebSocketChannel wsChannel);
	
	
	private boolean setWsResponse(HttpRequest request, HttpResponse response) {
		if(!websocketUriMatch(request.getUri(), uri)) {
			response.setStatus(HttpStatus.NOT_ACCEPTABLE);
			return false;
		}
		if(!"GET".equals(request.getMethod())) {
			response.setStatus(HttpStatus.METHOD_NOT_ALLOWED);
			return false;
		}
		if(request.getContentLength() > 0) {
			response.setStatus(HttpStatus.BAD_REQUEST);
			return false;
		}
		if(!Upgrade_Val.equals(request.getHeader(Upgrade))) {
			response.setStatus(HttpStatus.BAD_REQUEST);
			return false;
		}
		if(!Connection_Val.equals(request.getHeader(Connection))) {
			response.setStatus(HttpStatus.BAD_REQUEST);
			return false;
		}
		String version = request.getHeader(Sec_WebSocket_Version);
		if(null == version) {
			response.setStatus(HttpStatus.BAD_REQUEST);
			return false;
		}
		
		String key = request.getHeader(Sec_WebSocket_Key);
		String accept = key + MAGIC_NUMBER;
		accept = new String(Base64.getEncoder().encode(Sha1Util.hash(accept.getBytes())));
		
		response.setStatus(HttpStatus.SWITCHING_PROTOCOLS);
		response.setHeader(Upgrade, Upgrade_Val);
		response.setHeader(Connection, Connection_Val);
		response.setHeader(Sec_WebSocket_Accept, accept);
		response.setHeader(Sec_WebSocket_Version, version);
		
		return true;
	}

	public class WebSocketChannel {
		
		HttpRequest request;
		HttpResponse response;
		ChannelContext ctx;
		boolean handshakeDone;
		ReentrantLock lock = new ReentrantLock(true);
		Bytes input;
		
		public WebSocketChannel(ChannelContext ctx) {
			this.request = new HttpRequest(ctx.channel().remoteHost(), ctx.channel().remotePort());
			this.response = new HttpResponse(ctx.channel().remoteHost(), ctx.channel().remotePort());
			this.ctx = ctx;
			this.handshakeDone = false;
			this.input = new Bytes();
		}
		
		public void send(Bytes data) {
			ctx.toLastOnWrite(wrapWebSocketMessage(data));
		}
		
		public void close() {
			ctx.close();
		}
		
		public String remoteHost() {
			return ctx.channel().remoteHost();
		}
		
		public int remotePort() {
			return ctx.channel().remotePort();
		}
		
		protected Bytes resetAndGet() {
			Bytes input = this.input;
			this.input = new Bytes();
			return input;
		}
		
		protected Bytes wrapWebSocketMessage(Bytes out) {
			Bytes output = new Bytes();
			output.writeInt8(129);
			int length = out.available();
			if(length >= 65536) {
				output.writeInt8(127);
				output.writeInt32(length);
			} else if(length >= 126) {
				output.writeInt8(126);
				output.writeInt16(length);
			} else {
				output.writeInt8(length);
			}
			output.readFromBytes(out);
			return output;
		}

		protected boolean parseWebSocketMessage(Bytes in, Bytes buf) throws HttpServerException {
			int b = in.readInt8();
			int fin = b >> 7, opcode = b & 0xf;
		    b = in.readInt8();
		    int mask = b >> 7;
			int payloadLen = b & 0x7F;
			
			if(opcode == 0x8) {
				return false;
			}
			
			if(payloadLen == 126) {
				payloadLen = in.readInt16();
			} else if(payloadLen == 127) {
				payloadLen = in.readInt32();
			}
			byte[] content;
			byte[] maskingKey;
			if(mask == 1) {
				maskingKey = in.read(4);
				content = in.readAll();
				if(content.length != payloadLen) {
					throw new HttpServerException("can not parse websocket input data");
				}
				for(int i = 0; i < payloadLen; i++) {
					content[i] = (byte) (content[i] ^ maskingKey[i%4]);
				}
			} else {
				content = in.readAll();
				if(content.length != payloadLen) {
					throw new HttpServerException("can not parse websocket input data");
				}
			}
			buf.write(content);
			return fin == 1;
		}
		
	}
}
