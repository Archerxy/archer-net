package com.archer.net.handler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;

public class FrameReadHandler implements Handler {

	private static ConcurrentHashMap<ChannelContext, FrameMessage> frameCache = new ConcurrentHashMap<>();
	
	private int off, len, headLen;
	
	public FrameReadHandler(int off, int len, int headLen) {
		this.off = off;
		this.len = len;
		this.headLen = headLen;
	}

	@Override
	public void onAccept(ChannelContext ctx) {
		ctx.toNextOnAccept();
	}
	
	@Override
	public void onConnect(ChannelContext ctx) {
		toFrameMessage(ctx);
		ctx.toNextOnConnect();
	}

	@Override
	public void onRead(ChannelContext ctx, Bytes in) {
		FrameMessage frame = toFrameMessage(ctx);
		Bytes read = frame.read(in, off, len, headLen);
		if(read != null) {
			ctx.toNextOnRead(read);
		}
	}

	@Override
	public void onWrite(ChannelContext ctx, Bytes out) {
		ctx.toLastOnWrite(out);
	}

	@Override
	
	public void onDisconnect(ChannelContext ctx) {
		frameCache.remove(ctx);
		ctx.toNextOnDisconnect();
	}

	@Override
	public void onError(ChannelContext ctx, Throwable t) {
		ctx.toNextOnError(t);
	}
	
	@Override
	public void onSslCertificate(ChannelContext ctx, byte[] cert) {
		ctx.toNextOnCertificate(cert);
	}

	
	private FrameMessage toFrameMessage(ChannelContext ctx) {
		FrameMessage msg = frameCache.getOrDefault(ctx, null);
		if(msg == null) {
			msg = new FrameMessage();
			frameCache.put(ctx, msg);
		}
		return msg;
	}
	
	private class FrameMessage {
		
        ReentrantLock frameLock = new ReentrantLock(true);
        
		byte[] data;
		int pos;
		
		public FrameMessage() {}
		
		public Bytes read(Bytes in, int off, int len, int headLen) {
			try {
				frameLock.lock();
                
				int readCount;
				if(data == null) {
					int dataLen = getFrameLength(in, off, len) + headLen;
					data = new byte[dataLen];
					pos = 0;
					readCount = dataLen > in.available() ? in.available() : dataLen;
				} else {
					int remain = data.length - pos;
					readCount = remain > in.available() ? in.available() : remain;
				}
				in.read(data, pos, readCount);
				pos += readCount;
				if(readCount >= data.length) {
					Bytes read =  new Bytes(data);
					data = null;
					pos = 0;
					return read;
				}
				return null;
			} finally {
				frameLock.unlock();
			}
		}
		
		private int getFrameLength(Bytes in, int off, int len) {
			if(len >= 4) {
				throw new IllegalArgumentException("length out of int's range");
			}
			int base = 8 * (len - 1), readLen = 0;
			for(int i = 0; i < len; i++) {
				int b = in.at(off + i);
				b = b < 0 ? b + 256 : b;
				readLen |= (b << base);
				base -= 8;
			}
			return readLen;
		}
	}
}
