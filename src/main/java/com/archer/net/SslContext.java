package com.archer.net;

import com.archer.net.ssl.NamedCurve;
import com.archer.net.ssl.ProtocolVersion;

public class SslContext {
	
	private ProtocolVersion maxVersion, minVersion;
	
	private boolean verifyPeer = true;
	
	private byte[] ca;

	private byte[] crt, key;

	private byte[] encrt, enkey;
	
	private NamedCurve[] curves;
	
	public SslContext() {
		this(ProtocolVersion.TLS1_3_VERSION, ProtocolVersion.TLS1_1_VERSION);
	}
	
	public SslContext(ProtocolVersion max, ProtocolVersion min) {
		this.maxVersion = max;
		this.minVersion = min;
	}
	
	public SslContext verifyPeer(boolean verifyPeer) {
		this.verifyPeer = verifyPeer;
		return this;
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
	
	public SslContext useEncryptCertificate(byte[] enCrt, byte[] enKey) {
		this.encrt = enCrt;
		this.enkey = enKey;
		return this;
	}
	
	public SslContext namedCurves(NamedCurve[] curves) {
		this.curves = curves;
		return this;
	}

	public ProtocolVersion maxVersion() {
		return maxVersion;
	}

	public ProtocolVersion minVersion() {
		return minVersion;
	}

	public boolean verifyPeer() {
		return verifyPeer;
	}

	protected byte[] ca() {
		return ca;
	}

	protected byte[] crt() {
		return crt;
	}

	protected byte[] key() {
		return key;
	}

	protected byte[] encrt() {
		return encrt;
	}

	protected byte[] enkey() {
		return enkey;
	}

	protected NamedCurve[] namedCurves() {
		return curves;
	}
	
	protected void setSsl(long fd, boolean client) {
		SslContext.setSsl(fd, client, verifyPeer(), 
				maxVersion().version(), minVersion().version());
		SslContext.trustCertificate(fd, client, ca());
		if(crt() != null && key() != null) {
			SslContext.setCertificate(fd, client, crt(), key());
		}
		if(encrt() != null && enkey() != null) {
			SslContext.setEnCertificate(fd, client, encrt(), enkey());
		}
		if(namedCurves() != null) {
			SslContext.setNamedCurves(fd, client, NamedCurve.toOpensslNamedGroups(namedCurves()));
		}
	}
	
	protected static native void setSsl(long fd, boolean client, boolean verifyPeer, int maxVersion, int minVersion);

	protected static native void trustCertificate(long fd, boolean client, byte[] ca);
	
	protected static native void setCertificate(long fd, boolean client, byte[] crt, byte[] jkey);
	
	protected static native void setEnCertificate(long fd, boolean client, byte[] enCrt, byte[] enKey);
	
	protected static native void setNamedCurves(long fd, boolean client, String curves);
	
}
