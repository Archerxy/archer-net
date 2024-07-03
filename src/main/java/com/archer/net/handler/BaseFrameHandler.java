package com.archer.net.handler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;

public class BaseFrameHandler implements Handler {

	private static ConcurrentHashMap<ChannelContext, BlockedMessage> frameCache = new ConcurrentHashMap<>();
	
	public BaseFrameHandler() {}

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
		BlockedMessage frame = toFrameMessage(ctx);
		Bytes read = frame.read(in);
		if(read != null) {
			ctx.toNextOnRead(read);
		}
	}

	@Override
	public void onWrite(ChannelContext ctx, Bytes out) {
		Bytes wrap = new Bytes(out.available() + 16);
		wrap.writeInt32(out.available());
		wrap.readFromBytes(out);
		ctx.toLastOnWrite(wrap);
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

	
	private BlockedMessage toFrameMessage(ChannelContext ctx) {
		BlockedMessage msg = frameCache.getOrDefault(ctx, null);
		if(msg == null) {
			msg = new BlockedMessage();
			frameCache.put(ctx, msg);
		}
		return msg;
	}
	
	private class BlockedMessage {
		
        ReentrantLock frameLock = new ReentrantLock(true);
        
		byte[] data;
		int pos;
		
		public BlockedMessage() {}
		
		public Bytes read(Bytes in) {
			try {
				frameLock.lock();
                
				int readCount;
				if(data == null) {
					int dataLen = in.readInt32();
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
	}
}
