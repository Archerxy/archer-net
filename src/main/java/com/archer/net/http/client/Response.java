package com.archer.net.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Response {
	public static final int HTTP_OK = 200;
	
	private static final int BUF_SIZE = 10 * 1024;
	private static final String JOIN_SEP = "; ";
	private static final int DEFAULT_HEADER_SIZE = 64;
	
	int httpStatus;
	
	Map<String, String> headers;
	
	String body;

	protected Response() {}
	
	public int getHttpStatus() {
		return httpStatus;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public String getBody() {
		return body;
	}

	public static Response parseHttpConnection(HttpURLConnection conn) throws IOException {
		Response res = new Response();
		res.httpStatus = conn.getResponseCode();
		res.headers = new HashMap<>();
		for(Map.Entry<String, List<String>> headerEntry: conn.getHeaderFields().entrySet()) {
			res.headers.put(headerEntry.getKey(), joinList(headerEntry.getValue()));
		}
		String charset = conn.getContentEncoding();
		if(charset == null) {
		  	charset = Request.DEFAULT_CHARSET;
		}
		InputStream in;
		if(res.httpStatus == HTTP_OK) {
			in = conn.getInputStream();
		} else {
			in = conn.getErrorStream();
		}
		if(in == null) {
			res.body = null;
		} else {
			byte[] out = new byte[BUF_SIZE];
			int bytes = 0;
			StringBuilder sb = new StringBuilder(in.available());
			while((bytes = in.read(out)) >= 0) {
				sb.append(new String(Arrays.copyOfRange(out, 0, bytes), charset));
			}
	        res.body = sb.toString();
		}
		conn.disconnect();
        return res;
	}
	
	private static String joinList(List<String> list) {
		int i = list.size();
		StringBuilder sb = new StringBuilder(list.size() * DEFAULT_HEADER_SIZE);
		for(String l: list) {
			sb.append(l);
			if(i > 0) {
				sb.append(JOIN_SEP);
			}
		}
		return sb.toString();
	}
}
