package com.archer.net.ssl;

public enum ProtocolVersion {
	/**
	 * tls version
	 * */
	
	TLS1_VERSION(0x0301),
	TLS1_1_VERSION(0x0302),
	TLS1_2_VERSION(0x0303),
	TLS1_3_VERSION(0x0304);
	
	private int version;
	
	ProtocolVersion(int version) {
		this.version = version;
	}
	
	public int version() {
		return version;
	}
}
