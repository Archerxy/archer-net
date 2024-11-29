package com.archer.net.ssl;

public class SslException extends RuntimeException {

    static final long serialVersionUID = -3321467993124229948L;
    
    static final int STACK_DEEPTH = 8;
    static final int STACK_LEN = 128;
    
    public SslException(Throwable e) {
    	super(e.getMessage());
    }
    
    public SslException(String msg) {
    	super(msg);
    }
    
    public SslException(String msg, Throwable e) {
    	super(msg, e);
    }
    
    public static SslException formatException(Exception e) {
    	StackTraceElement[] stacks = e.getStackTrace();
    	String msg = e.getLocalizedMessage();
    	StringBuilder sb = new StringBuilder(msg.length() + STACK_DEEPTH * STACK_LEN);
    	sb.append(msg);
    	for(int i = 0; i < STACK_DEEPTH; i++) {
    		StackTraceElement el = stacks[i];
    		sb.append(';').append(el.getClassName()).append('.')
    		.append(el.getMethodName()).append('.').append(el.getLineNumber());
    	}
    	return new SslException(sb.toString());
    }
}
