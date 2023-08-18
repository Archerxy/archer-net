package com.archer.framework.net.p2p;

import java.io.InputStream;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.archer.framework.net.util.CertAndKeyUtil;

final class SSLContextBuilder {

	private static final String PROTOCOL = "TLS";
	
	public static SSLContext build(InputStream caStream, InputStream keyStream, InputStream crtStream) 
			throws Exception {
		KeyManager[] km = CertAndKeyUtil.buildKeyManagers(keyStream, crtStream, null);
		CertificateChecker checker = new CertificateChecker(CertAndKeyUtil.getCertificates(caStream)[0]);

		SSLContext context = SSLContext.getInstance(PROTOCOL);
		context.init(km, new TrustManager[] {checker}, null);
		return context;
	}
}
