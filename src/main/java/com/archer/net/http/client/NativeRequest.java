package com.archer.net.http.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import com.archer.net.Bytes;
import com.archer.net.Channel;
import com.archer.net.ChannelContext;
import com.archer.net.EventLoop;
import com.archer.net.handler.Handler;
import com.archer.net.http.HttpException;
import com.archer.net.http.HttpStatus;
import com.archer.net.SslContext;
import com.archer.net.ssl.ProtocolVersion;

public class NativeRequest {


    private static final int TIMEOUT = 3000;
    private static final int BASE_HEADER_LEN = 128;
    private static final int DEFAULT_HEADER_SIZE = 12;
    private static final char COLON = ':';
	private static final char SPACE = ' ';
	private static final String ENTER = "\r\n";
	
	private static final String HTTP_PROTOCOL = "HTTP/1.1";
	private static final String HEADER_CONTENT_LENGTH = "content-length";
	private static final String HEADER_CONTENT_ENCODE = "content-encoding";
	private static final String HEADER_HOST = "host";
	private static final String DEFAULT_CONTENT_ENCODE = "utf-8";
	private static final String[] HEADER_KEY = {"user-agent", "connection", "content-type", "accept"};
	private static final String[] HEADER_VAL = 
		{"Java/"+System.getProperty("java.version"), "close", "application/x-www-form-urlencoded",
		 "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"};

    public static NativeResponse get(String httpUrl) {
        return get(httpUrl, null);
    }

    public static NativeResponse post(String httpUrl, byte[] body) {
        return post(httpUrl, body, null);
    }

    public static NativeResponse put(String httpUrl, byte[] body) {
        return put(httpUrl, body, null);
    }

    public static NativeResponse delete(String httpUrl, byte[] body) {
        return delete(httpUrl, body, null);
    }

    public static NativeResponse get(String httpUrl, Options ctxions) {
        return request("GET", httpUrl, null, ctxions);
    }

    public static NativeResponse post(String httpUrl, byte[] body, Options ctxions) {
        return request("POST", httpUrl, body, ctxions);
    }

    public static NativeResponse put(String httpUrl, byte[] body, Options ctxions) {
        return request("PUT", httpUrl, body, ctxions);
    }

    public static NativeResponse delete(String httpUrl, byte[] body, Options ctxions) {
        return request("DELETE", httpUrl, body, ctxions);
    }
	
	public static NativeResponse request(String method, String httpUrl, byte[] body, Options opt) {
		if(opt == null) {
			opt = new Options();
		}
		HttpUrl url = HttpUrl.parse(httpUrl);
		
		SslContext ctx = null;
		if(url.isHttps()) {
			ctx = new SslContext(opt.getSslProtocol(), opt.getSslProtocol());
			ctx.verifyPeer(opt.verifyCert);
			if(opt.verifyCert) {
				if(opt.caPath() != null) {
					ctx.trustCertificateAuth(opt.readSslCrt(opt.caPath()));
				}
				if(opt.crtPath() != null && opt.keyPath() != null) {
					ctx.useCertificate(opt.readSslCrt(opt.crtPath()), opt.readSslCrt(opt.keyPath()));
				}
			}
		}
		Channel ch = new Channel(ctx);
		try {
			EventLoop loop = new EventLoop();
			HttpRequestHandler handler = 
					new HttpRequestHandler(new Bytes(getRequestAsBytes(method, url, opt, body))); 
			loop.addHandlers(handler);
			ch.eventLoop(loop);
			ch.connect(url.getHost(), url.getPort());
			
			handler.await();
			if(!handler.res.finished() && handler.err != null) {
				throw handler.err;
			}
			return handler.res;
		} finally {
			ch.close();
		}
	}
	
	   
	private static byte[] getRequestAsBytes(String method, HttpUrl url, Options ctxion, byte[] body) {
		Map<String, String> headers = ctxion.getHeaders();
		Map<String, String> newHeaders = new HashMap<>(DEFAULT_HEADER_SIZE);
		if(headers != null && headers.size() > 0) {
			for(Map.Entry<String, String> header: headers.entrySet()) {
				newHeaders.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue());
			}
		}
		StringBuilder sb = new StringBuilder(BASE_HEADER_LEN * (newHeaders.size() + 3));
		sb.append(method).append(SPACE).append(url.getUri()).append(SPACE).append(HTTP_PROTOCOL).append(ENTER);
		sb.append(HEADER_HOST).append(COLON).append(SPACE)
				.append(url.getHost()).append(COLON).append(url.getPort()).append(ENTER);
		for(int i = 0 ; i < HEADER_KEY.length; i++) {
			sb.append(HEADER_KEY[i]).append(COLON).append(SPACE);
			if(newHeaders.containsKey(HEADER_KEY[i])) {
				sb.append(newHeaders.get(HEADER_KEY[i])).append(ENTER);
				newHeaders.remove(HEADER_KEY[i]);
			} else {
				sb.append(HEADER_VAL[i]).append(ENTER);
			}
		}
		for(Map.Entry<String, String> header: newHeaders.entrySet()) {
			sb.append(header.getKey()).append(COLON).append(SPACE).append(header.getValue()).append(ENTER);
		}
		if(body != null) {
			sb.append(HEADER_CONTENT_LENGTH).append(COLON).append(SPACE).append(body.length).append(ENTER);
			sb.append(HEADER_CONTENT_ENCODE).append(COLON).append(SPACE).append(ctxion.getEncoding()).append(ENTER);
		}
		sb.append(ENTER);
		byte[] headerBytes = sb.toString().getBytes();
		byte[] requestBytes;
		if(body != null) {
			requestBytes = new byte[headerBytes.length + body.length];
			System.arraycopy(headerBytes, 0, requestBytes, 0, headerBytes.length);
			System.arraycopy(body, 0, requestBytes, headerBytes.length, body.length);
		} else {
			requestBytes = headerBytes;
		}
		return requestBytes;
	}

	
	public static class Options {
    	
    	private boolean verifyCert = true;
    	
    	private Map<String, String> headers = null;
    	
    	private int timeout = TIMEOUT;
    	
    	private ProtocolVersion sslProtocol = ProtocolVersion.TLS1_2_VERSION;

		private String encoding = DEFAULT_CONTENT_ENCODE;
		
		private String hostname = null;
		
		private String caPath;

		private String crtPath;
		
		private String keyPath;
    	
    	public Options() {}
    	
		public boolean isVerifyCert() {
			return verifyCert;
		}

		public Options verifyCert(boolean verifyCert) {
			this.verifyCert = verifyCert;
			return this;
		}

		public Map<String, String> getHeaders() {
			return headers;
		}

		public Options headers(Map<String, String> headers) {
			this.headers = headers;
			return this;
		}

		public int getTimeout() {
			return timeout;
		}

		public Options timeout(int timeout) {
			this.timeout = timeout;
			return this;
		}

		public String getEncoding() {
			return encoding;
		}

		public Options encoding(String encoding) {
			this.encoding = encoding;
			return this;
		}

		public ProtocolVersion getSslProtocol() {
			return sslProtocol;
		}
		
		public Options sslProtocol(ProtocolVersion sslProtocol) {
			this.sslProtocol = sslProtocol;
			return this;
		}

		public String getHostname() {
			return hostname;
		}

		public Options hostname(String hostname) {
			this.hostname = hostname;
			return this;
		}

		public String caPath() {
			return caPath;
		}
		
		public Options caPath(String caPath) {
			this.caPath = caPath;
			return this;
		}

		public String crtPath() {
			return crtPath;
		}
		
		public Options crtPath(String crtPath) {
			this.crtPath = crtPath;
			return this;
		}
		
		public String keyPath() {
			return keyPath;
		}

		public Options keyPath(String keyPath) {
			this.keyPath = keyPath;
			return this;
		}
		
		public byte[] readSslCrt(String path) {
			Path p = Paths.get(path);
			if(!Files.exists(p)) {
				throw new HttpException(HttpStatus.EXPECTATION_FAILED.getCode(),
						"file not found " + p.toString());
			}
			try {
				return Files.readAllBytes(p);
			} catch (IOException e) {
				throw new HttpException(HttpStatus.EXPECTATION_FAILED.getCode(),
						"can not read file " + p.toString());
			}
		}
	}
	
	final static class HttpUrl {

		private String url;

		private String protocol;

		private String host;

		private int port;

		private String uri;

		private HttpUrl(String url, String protocol, String host, int port, String uri) {
			this.url = url;
			this.protocol = protocol;
			this.host = host;
			this.port = port;
			this.uri = uri;
		}

		public String getUrl() {
			return url;
		}
		
		public String getProtocol() {
			return protocol;
		}
		
		public String getHost() {
			return host;
		}

		public int getPort() {
			return port;
		}

		public String getUri() {
			return uri;
		}
		
		public boolean isHttps() {
			return PROTOCOL_HTTPS.equals(protocol);
		}
		
		private static final char COLON = ':';
		private static final char SLASH = '/';

	    private static final char[] HTTP = { 'h', 't', 't', 'p' };
	    private static final char[] HTTPS = { 'h', 't', 't', 'p', 's' };
	    
	    private static final char[] PROTOCOL_SEP = { ':', '/', '/' };
	    
	    private static final String PROTOCOL_HTTP = "http";
	    private static final String PROTOCOL_HTTPS = "https";
	    
		
		public static HttpUrl parse(String httpUrl) {
			if(httpUrl == null || httpUrl.length() < HTTP.length + PROTOCOL_SEP.length + 2) {
				throw new IllegalArgumentException("invalid http url " + httpUrl);
			}
			String protocol = null, host = null, uri = null;
			int port = 0;
			char[] urlChars = httpUrl.toCharArray();
			int i = 0, t = 0;
			for(; i < HTTP.length; i++) {
				if(urlChars[i] != HTTP[i]) {
					throw new IllegalArgumentException("invalid http url " + httpUrl);
				}
			}
			if(urlChars[i] == HTTPS[i]) {
				protocol = PROTOCOL_HTTPS;
				i++;
			} else {
				protocol = PROTOCOL_HTTP;
			}
			t = i;
			for(; i < t + PROTOCOL_SEP.length; i++ ) {
				if(urlChars[i] != PROTOCOL_SEP[i - t]) {
					throw new IllegalArgumentException("invalid http url " + httpUrl);
				}
			}
			try {
				t = i;
				for(; i < urlChars.length; i++) {
					if(urlChars[i] == COLON) {
						char[] hostChars = Arrays.copyOfRange(urlChars, t, i);
						host = new String(hostChars);
						i++;
						break;
					}
					if(urlChars[i] == SLASH) {
						char[] hostChars = Arrays.copyOfRange(urlChars, t, i);
						host = new String(hostChars);
						port = PROTOCOL_HTTPS.equals(protocol) ? 443 :80;
						i++;
						break;
					}
					if(i >= urlChars.length - 1) {
						char[] hostChars = Arrays.copyOfRange(urlChars, t, urlChars.length);
						host = new String(hostChars);
						port = PROTOCOL_HTTPS.equals(protocol) ? 443 :80;
					}
				}
			} catch(Exception e) {
				throw new IllegalArgumentException("invalid http url " + httpUrl);
			}
			if(i < urlChars.length && port == 0) {
				try {
					t = i;
					for(; i < urlChars.length; i++) {
						if(urlChars[i] == SLASH) {
							char[] portChars = Arrays.copyOfRange(urlChars, t, i);
							port = Integer.parseInt(new String(portChars));
							i++;
							break ;
						}
						if(i >= urlChars.length - 1) {
							char[] portChars = Arrays.copyOfRange(urlChars, t, urlChars.length);
							port = Integer.parseInt(new String(portChars));
						}
					}
				} catch(Exception e) {
					throw new IllegalArgumentException("invalid http url " + httpUrl);
				}
			}
			uri = new String(Arrays.copyOfRange(urlChars, i, urlChars.length));
            if (uri.length() == 0) {
            	uri = "/";
            } else if (uri.charAt(0) == '?') {
            	uri = "/" + uri;
            }
			if(host == null || port == 0) {
				throw new IllegalArgumentException("invalid http url " + httpUrl);
			}
			return new HttpUrl(httpUrl, protocol, host, port, uri);
		}
	}
	

	private static class HttpRequestHandler implements Handler {

		NativeResponse res = new NativeResponse();
		Object lock = new Object();
		Bytes requestData;
		HttpException err;
		
		HttpRequestHandler(Bytes requestData) {
			this.requestData = requestData;
		}
		
		public void await() {
			long start = System.currentTimeMillis();
			synchronized(lock) {
				try {
					lock.wait(TIMEOUT);
				} catch (InterruptedException e) {}
			}
			long end = System.currentTimeMillis();
			if(end - start >= TIMEOUT) {
				throw new HttpException(HttpStatus.BAD_REQUEST.getCode(), "connect time out");
			}
		}

		@Override
		public void onRead(ChannelContext ctx, Bytes input) {
			if(res.headerParsed()) {
				res.parseContent(input.readAll());
			} else {
				res.parseHead(input.readAll());
			}
			if(res.finished()) {
				synchronized(lock) {
					lock.notifyAll();
				}
			}
		}
		@Override
		public void onWrite(ChannelContext ctx, Bytes output) {
			ctx.toLastOnWrite(output);
		}
		@Override
		public void onError(ChannelContext ctx, Throwable t) {
			err = new HttpException(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), t.getMessage());
			synchronized(lock) {
				lock.notifyAll();
			}
		}
		@Override
		public void onConnect(ChannelContext ctx) {
			ctx.write(requestData);
		}
		@Override
		public void onAccept(ChannelContext ctx) {}
		@Override
		public void onDisconnect(ChannelContext ctx) {
			synchronized(lock) {
				lock.notifyAll();
			}
		}
		@Override
		public void onSslCertificate(ChannelContext ctx, byte[] cert) {}
		
	}
	
}
