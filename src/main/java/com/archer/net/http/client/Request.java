package com.archer.net.http.client;

import javax.net.ssl.*;

import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author xuyi
 */
public class Request {

    static final int CONNECT_TIMEOUT = 5000;
    static final int READ_TIMEOUT = 10000;
    static final int DEFAULT_HEADER_SIZE = 4;

    static final String HTTP = "http://";
    static final String HTTPS = "https://";
    static final String DEFAULT_CHARSET = "utf-8";
    static final String DEFAULT_CONTENT_TYPE = "application/json; charset=utf-8";
    static final String HEADER_CONTENT_TYPE = "content-type";
    static final String HEADER_CONTENT_ENCODE = "content-encoding";
    static final String HEADER_CONTENT_LENGTH = "content-length";

    static Map<String, String> setHeaders(HttpURLConnection conn, Map<String, String> headers) {
        Map<String, String> newHeaders = new HashMap<>(DEFAULT_HEADER_SIZE);
        if(headers == null) {
            newHeaders.put(HEADER_CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
        } else {
            for(Map.Entry<String, String> headerEntry: headers.entrySet()) {
                newHeaders.put(headerEntry.getKey().toLowerCase(Locale.ROOT), headerEntry.getValue());
            }
        }
        if(!newHeaders.containsKey(HEADER_CONTENT_TYPE)) {
            newHeaders.put(HEADER_CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
        }
        for(Map.Entry<String, String> en: newHeaders.entrySet()) {
            conn.setRequestProperty(en.getKey(), en.getValue());
        }
        return newHeaders;
    }

    public static Response get(String httpUrl) throws Exception {
        return get(httpUrl, null);
    }

    public static Response post(String httpUrl, byte[] body) throws Exception {
        return post(httpUrl, body, null);
    }

    public static Response put(String httpUrl, byte[] body) throws Exception {
        return put(httpUrl, body, null);
    }

    public static Response delete(String httpUrl, byte[] body) throws Exception {
        return delete(httpUrl, body, null);
    }

    public static Response get(String httpUrl, Options options) throws Exception {
        return request("GET", httpUrl, null, options);
    }

    public static Response post(String httpUrl, byte[] body, Options options) throws Exception {
        return request("POST", httpUrl, body, options);
    }

    public static Response put(String httpUrl, byte[] body, Options options) throws Exception {
        return request("PUT", httpUrl, body, options);
    }

    public static Response delete(String httpUrl, byte[] body, Options options) throws Exception {
        return request("DELETE", httpUrl, body, options);
    }

    public static Response request(String httpMethod, String httpUrl, byte[] body, Options options) throws Exception {
        URL url = new URL(httpUrl);
        
        HttpURLConnection conn;
    	if(options == null) {
    		options = new Options();
    	}
        if(isHttp(httpUrl)) {
        	conn = (HttpURLConnection) url.openConnection();
        } else if(isHttps(httpUrl)) {
        	HttpsURLConnection httpsConn = (HttpsURLConnection) url.openConnection();
        	SSLContext ctx;
        	if(options.getSslProtocol() == null) {
                ctx = SSLContext.getInstance("TLS");
        	} else {
                ctx = SSLContext.getInstance(options.getSslProtocol());
        	}
        	if(!options.isVerifyCert()) {
                ctx.init(options.getKeyManager(), TRUSTED_MGR, null);
            } else {
            	ctx.init(options.getKeyManager(), options.getTrustManager(), null);
            }
            httpsConn.setSSLSocketFactory(ctx.getSocketFactory());
        	conn = httpsConn;
        } else {
        	throw new IllegalArgumentException("invalid http url " + httpUrl);
        }
        conn.setRequestMethod(httpMethod);
        conn.setConnectTimeout(options.getConnectTimeout());
        conn.setReadTimeout(options.getReadTimeout());
        conn.setDoOutput(true);
        conn.setDoInput(true);

        Map<String, String> headers = setHeaders(conn, options.getHeaders());

        if(body != null) {
            String charset = headers.get(HEADER_CONTENT_ENCODE);
            if(charset == null) {
                charset = DEFAULT_CHARSET;
                conn.setRequestProperty(HEADER_CONTENT_ENCODE, DEFAULT_CHARSET);
            }
            conn.setRequestProperty(HEADER_CONTENT_LENGTH, String.valueOf(body.length));
            conn.getOutputStream().write(body);
        }
        conn.connect();
        
        return Response.parseHttpConnection(conn);
    }
    
    private static boolean isHttp(String url) {
    	return url != null && url.startsWith(HTTP);
    }
    
    private static boolean isHttps(String url) {
    	return url != null && url.startsWith(HTTPS);
    }

    public static final TrustManager[] TRUSTED_MGR = new TrustManager[] { new X509ExtendedTrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
        @Override
        public X509Certificate[] getAcceptedIssuers() {
        	
        	return null;}
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {}
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {}
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {}
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {}
    } };
    

	public static class Options {
    	
    	private boolean verifyCert = true;
    	
    	private Map<String, String> headers = null;
    	
    	private int connectTimeout = CONNECT_TIMEOUT;
    	
    	private int readTimeout = READ_TIMEOUT;
    	
    	private String sslProtocol;
    	
    	private KeyManager[] keyManager;
    	
    	private TrustManager[] trustManager;

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

		public int getConnectTimeout() {
			return connectTimeout;
		}

		public Options connectTimeout(int connectTimeout) {
			this.connectTimeout = connectTimeout;
			return this;
		}

		public int getReadTimeout() {
			return readTimeout;
		}

		public Options readTimeout(int readTimeout) {
			this.readTimeout = readTimeout;
			return this;
		}

		public String getSslProtocol() {
			return sslProtocol;
		}
		
		public Options sslProtocol(String sslProtocol) {
			this.sslProtocol = sslProtocol;
			return this;
		}

		public KeyManager[] getKeyManager() {
			return keyManager;
		}

		public Options keyManager(KeyManager[] keyManager) {
			this.keyManager = keyManager;
			return this;
		}

		public TrustManager[] getTrustManager() {
			return trustManager;
		}

		public Options trustManager(TrustManager[] trustManager) {
			this.trustManager = trustManager;
			return this;
		}
	}

}
