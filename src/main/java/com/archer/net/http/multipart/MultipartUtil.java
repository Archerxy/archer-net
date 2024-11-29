package com.archer.net.http.multipart;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.archer.net.http.HttpException;
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpStatus;

@Deprecated
public class MultipartUtil {

	private static final String MULTIPART = "multipart/form-data; boundary=";

    private static final char[] KEY_NAME = {'n', 'a', 'm', 'e', '=', '"'};
    private static final char[] FILE_NAME = {'f','i','l','e','n','a','m','e', '=', '"'};
    private static final char[] CONTENT_TYPE = {'C','o','n','t','e','n','t','-', 'T', 'y', 'p', 'e', ':'};
    private static final char QT = '"';
    private static final char LF = '\n';
    
    private static final int SEP_START = 1;
    private static final int HEAD_START = 2;
    private static final int CONTENT_START = 3;

    public static List<Multipart> parse(HttpRequest req) {
    	if(!req.getContentType().startsWith(MULTIPART)) {
    		throw new HttpException(HttpStatus.BAD_REQUEST.getCode(), 
    				"invalid mulipart formdata content-type:" + req.getContentType());
    	}
        String sep = "--"+req.getContentType().substring(MULTIPART.length()).trim();
        return parse(sep, req.getContent());
    }
    private static List<Multipart> parse(String sepStr, byte[] content) {
    	byte[] sep = sepStr.getBytes();
    	int off = 0, state = SEP_START, vs = 0, cs = -1, lastLF = 0;
		Multipart part = null;
		List<Multipart> partList = new LinkedList<>();
    	for(; off < content.length; off++) {
    		if(state == SEP_START && off + sep.length < content.length) {
        		boolean ok = true;
        		for(int i = 0; i < sep.length; i++) {
        			if(sep[i] != content[off + i]) {
        				ok = false;
        				break ;
        			}
        		}
        		if(ok) {
        			if(cs > 0 && part != null) {
        				part.setContent(Arrays.copyOfRange(content, cs, off - 2));
        				partList.add(part);
        			}
        			part = new Multipart();
        			state = HEAD_START;
        			off += sep.length;
        		}
    		}
    		if(state == HEAD_START) {
    			part.setType(MultipartType.TEXT);
    			while(off < content.length) {
    				if(content[off] == LF) { 
    					if(off - lastLF <= 2) {
        					state = SEP_START;
        					off++;
            				cs = off;
            				break;
    					} else {
        					lastLF = off;	
    					}
    				}
       				if(content[off] == KEY_NAME[0] && content[off+1] == KEY_NAME[1] && 
     				   content[off+2] == KEY_NAME[2] && content[off+3] == KEY_NAME[3] && 
     				   content[off+4] == KEY_NAME[4] && content[off+5] == KEY_NAME[5]) {
     					off += KEY_NAME.length;
     					vs = off;

         				while(content[off] != QT) {
         					off++;
         					if(off >= content.length) {
         			    		throw new HttpException(HttpStatus.BAD_REQUEST.getCode(), 
         			    				"invalid mulipart formdata head:" + 
         			    				new String(Arrays.copyOfRange(content, vs, vs + KEY_NAME.length + 16)));
         					}
         				}
     					part.setName(new String(Arrays.copyOfRange(content, vs, off)));
     				}
       				
       				if(content[off] == FILE_NAME[0] && content[off+1] == FILE_NAME[1] && 
     				   content[off+2] == FILE_NAME[2] && content[off+3] == FILE_NAME[3] && 
     				   content[off+4] == FILE_NAME[4] && content[off+5] == FILE_NAME[5] && 
     				   content[off+6] == FILE_NAME[6] && content[off+7] == FILE_NAME[7] && 
     				   content[off+8] == FILE_NAME[8] && content[off+9] == FILE_NAME[9]) {
       					
     					off += FILE_NAME.length;
     					vs = off;
     					while(content[off] != QT) {
         					off++;
         					if(off >= content.length) {
         			    		throw new HttpException(HttpStatus.BAD_REQUEST.getCode(), 
         			    				"invalid mulipart formdata head:" + 
         			    				new String(Arrays.copyOfRange(content, vs, vs + FILE_NAME.length + 16)));
         					}
        				}
    					part.setType(MultipartType.FILE);
    					part.setFileName(new String(Arrays.copyOfRange(content, vs, off)));
    					state = CONTENT_START;
     				}
       				if(off + CONTENT_TYPE.length < content.length) {
       					boolean ok = true;
       					for(int t = 0; t < CONTENT_TYPE.length; t++) {
       						if(CONTENT_TYPE[t] != content[off + t]) {
       							ok = false;
       							break;
       						}
       					}
       					if(ok) {
       						off += CONTENT_TYPE.length;
       						vs = off;
         					while(content[off] != LF) {
             					off++;
             					if(off >= content.length) {
             			    		throw new HttpException(HttpStatus.BAD_REQUEST.getCode(), 
             			    				"invalid mulipart formdata head:" + 
             			    				new String(Arrays.copyOfRange(content, vs, vs + FILE_NAME.length + 16)));
             					}
            				}
         					part.setContentType(new String(Arrays.copyOfRange(content, vs, off)).trim());
           					continue ;
       					}
       				}
        			off++;
    			}
    		}
    	}
    	if(state == SEP_START && cs > 0 && part != null) {
    		part.setContent(Arrays.copyOfRange(content, cs, content.length));
    		partList.add(part);
    	}
        return partList;
    }
}
