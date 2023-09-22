package com.archer.net.message;

import com.archer.net.Bytes;

public interface InputMessage {
	
	void decode(Bytes bytes);
}
