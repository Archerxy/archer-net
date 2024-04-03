package com.archer.net.http.multipart;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import com.archer.net.http.HttpException;
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpStatus;

public class MultipartParser {
	
	private static final String MULTIPART = "multipart/form-data; boundary=";
	private static final String PART_BODY = "\r\n\r\n";
	private static final String LINE_LR = "\r\n";
	private static final String HEADER_SEP = "; ";
	private static final String CONTENT_DISPOS = "Content-Disposition: form-data; ";
	private static final String CONTENT_TYPE = "Content-Type: ";
	private static final String EQ = "=\"";
	private static final String NAME = "name";
	private static final String FILENAME = "filename";
	
    public static List<Multipart> parse(HttpRequest req) throws UnsupportedEncodingException {
    	if(!req.getContentType().startsWith(MULTIPART)) {
    		throw new HttpException(HttpStatus.BAD_REQUEST.getCode(), 
    				"invalid mulipart formdata content-type:" + req.getContentType());
    	}
        String sep = "--"+req.getContentType().substring(MULTIPART.length()).trim();
        String bodyStr = new String(req.getContent(), req.getContentEncoding());
        String start = sep + "\r\n", end = sep + "--\r\n";
        if(!bodyStr.startsWith(start) || !bodyStr.endsWith(end)) {
    		throw new HttpException(HttpStatus.BAD_REQUEST.getCode(), 
    				"invalid mulipart formdata content");
        }
        bodyStr = bodyStr.substring(start.length(), bodyStr.length() - end.length());
        return parse(bodyStr, sep + "\r\n", req.getContentEncoding());
    }
    
    private static List<Multipart> parse(String body, String sep, String encoding) throws UnsupportedEncodingException {
    	List<Multipart> parts = new LinkedList<>();
    	String[] partStrs = body.split(sep);
    	for(String s: partStrs) {
    		Multipart part = new Multipart();
    		String[] headAndBody = s.split(PART_BODY);
    		if(headAndBody.length != 2) {
    			throw new HttpException(HttpStatus.BAD_REQUEST);
    		}
    		parseHeader(headAndBody[0], part);
    		String content = headAndBody[1];
    		if(content.endsWith(LINE_LR)) {
    			content = content.substring(0, content.length() - LINE_LR.length());
    		}
    		part.setContent(content.getBytes(encoding));
    		parts.add(part);
    	}
    	return parts;
    }
    
    
    private static void parseHeader(String headerStr, Multipart part) {
    	part.setType(MultipartType.TEXT);
    	String[] headers = headerStr.split(LINE_LR);
    	for(String header: headers) {
    		if(header.startsWith(CONTENT_DISPOS)) {
    			header = header.substring(CONTENT_DISPOS.length());
    			for(String item: header.split(HEADER_SEP)) {
    				int eq = item.indexOf(EQ);
    				if(eq < 0) {
    					continue;
    				}
    				String k = item.substring(0, eq);
    				String v = item.substring(eq + 2, item.length() - 1);
    				if(NAME.equals(k)) {
    					part.setName(v);
    				} else if(FILENAME.equals(k)) {
    					part.setFileName(v);
    					part.setType(MultipartType.FILE);
    				}
    			}
    		} else if(header.startsWith(CONTENT_TYPE)) {
    			part.setContentType(header.substring(CONTENT_TYPE.length()).trim());
    		}
    	}
    }
}
