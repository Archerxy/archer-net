package test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;

import com.archer.net.http.client.NioRequest;
import com.archer.net.http.client.NioResponse;

public class HttpsTest {
    private static final TrustManager[] NULL_TRUSTED_MGR = new TrustManager[] { 
    new X509ExtendedTrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            System.out.println("check cert");
			for(X509Certificate cert :chain) {
	            System.out.println("Subject: " + cert.getSubjectDN());
			}
        }
        @Override
        public X509Certificate[] getAcceptedIssuers() {return null;}@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {}
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
            System.out.println("socket check cert");
			for(X509Certificate cert :chain) {
	            System.out.println("Subject: " + cert.getSubjectDN());
			}
		}
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {}
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            System.out.println("engine check cert");
			for(X509Certificate cert :chain) {
	            System.out.println("Subject: " + cert.getSubjectDN());
			}
		}
    }};
	
	public static void javaTest() {
		try {
		    // 创建URL对象
		    URL url = new URL("https://www.zhihu.com"); 
		    String host = url.getHost();

            InetAddress ipAddress = InetAddress.getByName(host);
            System.out.println(ipAddress.getHostAddress());

		    // 打开HTTPS连接
		    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		    
		    SSLContext ctx = SSLContext.getInstance("TLS");
		    ctx.init(null, NULL_TRUSTED_MGR, null);
		    connection.setSSLSocketFactory(ctx.getSocketFactory());
		    connection.connect();

            System.out.println("*********************");
		    for(Entry<String, List<String>> entry: connection.getHeaderFields().entrySet()) {
		    	System.out.println(entry.getKey() + ": " + Arrays.toString(entry.getValue().toArray(new String[0])));
		    }
            System.out.println("*********************");
		    
		    // 获取服务器证书链
		    Certificate[] serverCertificates = connection.getServerCertificates();

		    // 打印每个证书的信息
		    for (Certificate cert : serverCertificates) {
		        if (cert instanceof X509Certificate) {
		            X509Certificate x509Cert = (X509Certificate) cert;
		            System.out.println("get final Subject: " + x509Cert.getSubjectDN());
		        }
		    }

		    //Subject: CN=*.zhihu.com, O=智者四海（北京）技术有限公司, ST=北京市, C=CN
		    //Issuer: CN=GeoTrust CN RSA CA G1, OU=www.digicert.com, O=DigiCert Inc, C=US
		    // 关闭连接
		    connection.disconnect();
		} catch (SSLPeerUnverifiedException e) {
		    e.printStackTrace();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}

	public static void main(String[] args) {
//		try {
//			NioResponse res = NioRequest.get("https://www.zhihu.com", new NioRequest.Options().verifyCert(false));
//			System.out.println(new String(res.getBody()));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		javaTest();
	}
}
