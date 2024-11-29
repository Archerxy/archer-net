package com.archer.net.ssl;

import java.util.Arrays;

public enum NamedCurve {
	/**
	 * some of openssl 1.1.1 supported curves.
	 * */
	NID_secp112r1("secp112r1"),
	NID_secp112r2("secp112r2"),
	NID_secp128r1("secp128r1"),
	NID_secp128r2("secp128r2"),
	NID_secp160k1("secp160k1"),
	NID_secp160r1("secp160r1"),
	NID_secp160r2("secp160r2"),
	NID_secp192k1("secp192k1"),
	NID_secp224k1("secp224k1"),
	NID_secp224r1("secp224r1"),
	NID_secp256k1("secp256k1"),
	NID_secp384r1("secp384r1"),
	NID_secp521r1("secp521r1"),
	NID_sect113r1("sect113r1"),
	NID_sect113r2("sect113r2"),
	NID_sect131r1("sect131r1"),
	NID_sect131r2("sect131r2"),
	NID_sect163k1("sect163k1"),
	NID_sect163r1("sect163r1"),
	NID_sect163r2("sect163r2"),
	NID_sect193r1("sect193r1"),
	NID_sect193r2("sect193r2"),
	NID_sect233k1("sect233k1"),
	NID_sect233r1("sect233r1"),
	NID_sect239k1("sect239k1"),
	NID_sect283k1("sect283k1"),
	NID_sect283r1("sect283r1"),
	NID_sect409k1("sect409k1"),
	NID_sect409r1("sect409r1"),
	NID_sect571k1("sect571k1"),
	NID_sect571r1("sect571r1");
	
	private String name;
	
	NamedCurve(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	

	private static final String[] DEFAULT_NAMED_GROUPS = { "X25519", "P-224", "P-256", "P-384", "P-521", "SM2" };
	
	public static String toOpensslNamedGroups(NamedCurve[] userCurves) {
		if(userCurves == null || userCurves.length == 0) {
			return String.join(":", DEFAULT_NAMED_GROUPS);
		}
		String[] nGroups = new String[DEFAULT_NAMED_GROUPS.length + userCurves.length];
		System.arraycopy(DEFAULT_NAMED_GROUPS, 0, nGroups, 0, DEFAULT_NAMED_GROUPS.length);
		int off = DEFAULT_NAMED_GROUPS.length;
		for(int i = 0; i < userCurves.length; i++) {
			String nGroup = toOpensslGroup(userCurves[i].getName());
			if(userCurves[i].getName().equals(nGroup)) {
				nGroups[off++] = nGroup;
			}
		}
		return String.join(":", Arrays.copyOfRange(nGroups, 0, off));
	}

	
	private static String toOpensslGroup(String curve) {
		switch(curve) {
	    case "secp224r1": return "P-224";
        case "prime256v1": return "P-256";
        case "secp256r1": return "P-256";
        case "secp384r1": return "P-384";
        case "secp521r1": return "P-521";
        case "x25519": return "X25519";
        case "sm2p256v1": return "SM2";
        default: return curve;
		}
	}
}
