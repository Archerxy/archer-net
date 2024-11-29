package com.archer.net.ssl;

public class SslContext {
	
	protected static native long init(SslContext sslCtx, boolean isClient, boolean verifyPeer, int maxVersion, int minVersion);

	protected static native void setSsl(long sslfd, long channelfd, boolean isClient);

	protected static native boolean trustCertificate(long sslfd, byte[] ca);
	
	protected static native boolean setCertificate(long sslfd, byte[] cert, byte[] key);
	
	protected static native boolean setEnCertificate(long sslfd, byte[] encryptCert, byte[] encryptKey);

	protected static native void setNamedCurves(long sslfd, byte[] namedCurves);
	
	protected static native void validateHostname(long sslfd, byte[] hostname);

	protected static native void close(long sslfd);
	
	
	/***below defines class***/
	
	private long sslfd;
	
	private ProtocolVersion maxVersion, minVersion;
	
	private boolean clientMode;
	
	private boolean verifyPeer;
	
	private byte[] ca;

	private byte[] crt, key;

	private byte[] encryptCrt, encryptKey;
	
	private NamedCurve[] curves;
	
	private String hostname;
	
	
	public SslContext(boolean isClientMode) {
		this(isClientMode, true, ProtocolVersion.TLS1_3_VERSION, ProtocolVersion.TLS1_1_VERSION);
	}
	
	public SslContext(boolean isClientMode, boolean verifyPeer) {
		this(isClientMode, verifyPeer, ProtocolVersion.TLS1_3_VERSION, ProtocolVersion.TLS1_1_VERSION);
	}
	
	public SslContext(boolean isClientMode, ProtocolVersion max, ProtocolVersion min) {
		this(isClientMode, true, max, min);
	}
	
	public SslContext(boolean isClientMode, boolean verifyPeer, ProtocolVersion max, ProtocolVersion min) {
		this.clientMode = isClientMode;
		this.verifyPeer = verifyPeer;
		this.maxVersion = max;
		this.minVersion = min;	
	}
	
	public void throwError(byte[] message) {
		close(sslfd);
		throw new SslException(new String(message));
	}
	
	public SslContext trustCertificateAuth(byte[] ca) {
		this.ca = ca;
		return this;
	}
	
	public SslContext useCertificate(byte[] crt, byte[] key) {
		this.crt = crt;
		this.key = key;
		return this;
	}	
	
	public SslContext useEncryptCertificate(byte[] encryptCrt, byte[] encryptKey) {
		this.encryptCrt = encryptCrt;
		this.encryptKey = encryptKey;
		return this;
	}
	
	public SslContext namedCurves(NamedCurve[] curves) {
		this.curves = curves;
		return this;
	}
	
	public SslContext validateHostname(String hostname) {
		this.hostname = hostname;
		return this;
	}
	
	public void setSsl(long channelfd) {
		this.sslfd = init(this, clientMode, verifyPeer, maxVersion.version(), minVersion.version());
		trustCertificate(sslfd, ca);
		if(crt != null && key != null) {
			setCertificate(sslfd, crt, key);	
		}
		if(encryptCrt != null && encryptKey != null) {
			setEnCertificate(sslfd, encryptCrt, encryptKey);
		}
		if(curves != null) {
			setNamedCurves(sslfd, NamedCurve.toOpensslNamedGroups(curves).getBytes());
		}
		if(hostname != null) {
			validateHostname(sslfd, hostname.getBytes());
		}
		setSsl(sslfd, channelfd, clientMode);
	}
	
	public ProtocolVersion maxVersion() {
		return maxVersion;
	}

	public ProtocolVersion minVersion() {
		return minVersion;
	}

	public boolean isClientMode() {
		return clientMode;
	}
	
	public boolean verifyPeer() {
		return verifyPeer;
	}

	public byte[] ca() {
		return ca;
	}

	public byte[] crt() {
		return crt;
	}

	public byte[] key() {
		return key;
	}

	public byte[] encryptCrt() {
		return encryptCrt;
	}

	public byte[] encryptKey() {
		return encryptKey;
	}

	public NamedCurve[] namedCurves() {
		return curves;
	}
}
