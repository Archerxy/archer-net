package com.archer.framework.net.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class HttpRequest {

    private static final int DEFAULT_HEADER_SIZE = 32;
    private static final char SEM = ';';
    private static final char COLON = ':';
    private static final char SPACE = ' ';
    private static final char ENTER = '\n';
    private static final int KEY_START = 1;
    private static final int VAL_START = 2;
    
	protected static final String GET = "GET";
	protected static final String POST = "POST";
	protected static final String PUT = "PUT";
	protected static final String DELETE = "DELETE";
	protected static final String OPTION = "OPTION";

	protected static final char[] VERSION = { 'H', 'T', 'T', 'P', '/', '1', '.'};
	protected static final char URI_SEP = '/';
	
	protected static final int METHOD_LEN = OPTION.length() + 2;

	private static final String HEADER_CONTENT_TYPE = "content-type";
	private static final String HEADER_CONTENT_LENGTH = "content-length";
	private static final String HEADER_CONTENT_ENCODE = "content-encoding";

    private static final String DEFAULT_ENCODING_VAL = "utf-8";
    private static final String DEFAULT_ENCODING_KEY = "charset";
    
    private ReentrantLock contentLock = new ReentrantLock(true);
	

    private String remoteHost;
    private int remotePort;
    
	private String method;
	private String uri;
	private String httpVersion;
	
	
	private Map<String, String> headers;
	private String contentType;
	private String contentEncoding;
	private int contentLength;

	private byte[] content;
	private int pos = 0;

	protected HttpRequest(String host, int port) {
		clear();
		remoteHost = host;
		remotePort = port; 
	}

	public String remoteHost() {
		return remoteHost;
	}

	public int remotePort() {
		return remotePort;
	}

	
	public String getMethod() {
		return method;
	}

	public String getUri() {
		return uri;
	}

	public String getHttpVersion() {
		return httpVersion;
	}

	public String getPropoerty(String key) {
		return headers.getOrDefault(key, null);
	}
	
	public String getContentType() {
		return contentType;
	}

	public String getContentEncoding() {
		return contentEncoding;
	}

	public int getContentLength() {
		return contentLength;
	}

	public byte[] getContent() {
		return content;
	}
	
	public void clear() {
		method = null;
		uri = null;
		httpVersion = null;
		
		headers = new HashMap<>(DEFAULT_HEADER_SIZE);
		contentType = null;
		contentEncoding = null;
		contentLength = -1;
		
		content = null;
		pos = 0;
	}

	protected void setMethod(String method) throws IOException {
		if(!GET.equals(method) &&
			!POST.equals(method) &&
			!PUT.equals(method) &&
			!DELETE.equals(method) &&
			!OPTION.equals(method)) {
			throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED);
		}
		this.method = method;
	}

	protected void setUri(String uri) {
		uri = uri.trim();
		if(uri.length() == 0 || uri.charAt(0) != URI_SEP) {
			uri = URI_SEP + uri;
		}
		this.uri = uri;
	}
	
	protected void setHttpVersion(String version) throws IOException {
		version = version.trim();
		for(int i = 0; i < VERSION.length; i++) {
			if(version.charAt(i) != VERSION[i]) {
				throw new HttpException(HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
			}
		}
		this.httpVersion = version;
	}
	
	protected void parse(byte[] msg) throws IOException {
		if(msg.length <= HttpRequest.OPTION.length()) {
			throw new HttpException(HttpStatus.BAD_REQUEST);
		}
		contentLock.lock();
		int i = 0, p = 0;
		while(msg[i] != SPACE && i < HttpRequest.METHOD_LEN) {
			i++;
		}
		if(i >= HttpRequest.METHOD_LEN) {
			throw new HttpException(HttpStatus.BAD_REQUEST);
		}
		setMethod(new String(Arrays.copyOfRange(msg, p, i)));
		i++;
		p = i;
		while(msg[i] != SPACE) {
			if(msg[i] == ENTER || i == msg.length - 1) {
				throw new HttpException(HttpStatus.BAD_REQUEST);
			}
			i++;
		}
		setUri(new String(Arrays.copyOfRange(msg, p, i)));
		i++;
		p = i;
		while(msg[i] != ENTER) {
			if(i == msg.length - 1) {
				throw new HttpException(HttpStatus.BAD_REQUEST);
			}
			i++;
		}
		setHttpVersion(new String(Arrays.copyOfRange(msg, p, i)));
		i++;
		p = i;
		int state = KEY_START;
		String key = null, val = null;
    	for(; i < msg.length; i++) {
    		if(state == KEY_START && msg[i] == ENTER) {
    			break;
    		}
    		if(state == KEY_START && msg[i] == COLON) {
    			key = new String(Arrays.copyOfRange(msg, p, i)).trim();
    			p = i + 1;
    			state = VAL_START;
    			continue;
    		}
    		if(state == VAL_START && msg[i] == ENTER) {
    			if(key == null) {
    				throw new HttpException(HttpStatus.BAD_REQUEST);
    			}
    			val = new String(Arrays.copyOfRange(msg, p, i)).trim();
    			headers.put(key.toLowerCase(), val);
    			p = i + 1;
    			state = KEY_START;
    		}
    	}
		this.contentType = headers.getOrDefault(HEADER_CONTENT_TYPE, null);
		this.contentEncoding = headers.getOrDefault(HEADER_CONTENT_ENCODE, null);
		if(contentType != null && contentEncoding == null) {
			int sem;
			if((sem = contentType.indexOf(SEM)) > 0) {
				contentEncoding = contentType.substring(sem + 1).trim();
				if(!contentEncoding.startsWith(DEFAULT_ENCODING_KEY)) {
					contentEncoding = DEFAULT_ENCODING_VAL;
				} else {
					contentEncoding = contentEncoding
							.substring(DEFAULT_ENCODING_KEY.length() + 1).trim();
				}
				contentType = contentType.substring(0, sem).trim();
			}
		}
		while(i < msg.length && msg[i] != ENTER) {
			i++;
		}
		if(i < msg.length) {
			i++;
		}
		String len = headers.getOrDefault(HEADER_CONTENT_LENGTH, "0");
		try {
			contentLength = Integer.parseInt(len);
		} catch(Exception e) {
			throw new HttpException(HttpStatus.BAD_REQUEST);
		}
		if(contentLength > 0) {
			if(msg.length - i > contentLength) {
				System.err.println("content remaining " + (msg.length - i) + 
						"while content length is " + len);
				throw new HttpException(HttpStatus.BAD_REQUEST);
			}
			if(i > msg.length) {
				throw new HttpException(HttpStatus.BAD_REQUEST);
			}
			content = new byte[contentLength];
			if(i < msg.length) {
				System.arraycopy(msg, i, content, 0, msg.length - i);
				pos += msg.length - i;
			}
		} else if(contentLength == 0) {
			content = new byte[0];
		} else {
			throw new HttpException(HttpStatus.BAD_REQUEST);
		}
		contentLock.unlock();
	}
	
	protected synchronized void putContent(byte[] content) throws IOException {
		contentLock.lock();
		if(this.content == null) {
			throw new IOException("content is not expected here.");
		}
		if(content.length + pos > this.content.length) {
			throw new IOException("content bytes over flow.");
		}
		System.arraycopy(content, 0, this.content, pos, content.length);
		pos += content.length;
		contentLock.unlock();
	}
	
	protected boolean isFinished() {
		if(content == null) {
			return contentLength == 0;
		}
		return  pos == contentLength;
	}
	
	protected boolean isEmpty() {
		return method == null || uri == null || httpVersion == null;
	}
}
