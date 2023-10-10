package com.archer.net.p2p;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;
import com.archer.net.ChannelException;
import com.archer.net.handler.Handler;


public class P2PBaseHandler implements Handler {

	private static final byte COMPRESS = 21;
	private static final byte UN_COMPRESS = 22;
	
	private static ConcurrentHashMap<ChannelContext, FrameMessage> frameCache = new ConcurrentHashMap<>();
	
	@Override
	public void onAccept(ChannelContext ctx) {
		//todo
	}
	
	@Override
	public void onConnect(ChannelContext ctx) {
		toFrameMessage(ctx);
	}

	@Override
	public void onRead(ChannelContext ctx, Bytes in) {
		FrameMessage frame = toFrameMessage(ctx);
		Bytes read = frame.appDataUnwrap(in);
		if(read != null) {
			ctx.toNextOnRead(read);
		}
	}

	@Override
	public void onWrite(ChannelContext ctx, Bytes out) {
		FrameMessage frame = toFrameMessage(ctx);
		ctx.toLastOnWrite(frame.appDataWrap(out));
	}

	@Override
	public void onDisconnect(ChannelContext ctx) {
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

		private ReentrantLock frameLock = new ReentrantLock(true);

		private byte[] data = null;
		private int pos = 0;
		private boolean compressed = false;
		
		public FrameMessage() {}
		
		public Bytes appDataWrap(Bytes in) {
			byte[] text = in.readAll();
			int head = 4 + 1;
			Bytes out = new Bytes(head + text.length);
			out.writeInt32(in.available());
			if(text.length > Bytes.BUFFER_SIZE) {
				out.writeInt8(COMPRESS);
				text = Compresser.compress(text);
			} else {
				out.writeInt8(UN_COMPRESS);
			}
			in.writeToBytes(out);
			return out;
		}
		
		public Bytes appDataUnwrap(Bytes in) {
			try {
				frameLock.lock();
				int readCount;
				if(data == null) {
					int dataLen = in.readInt32();
					byte compress = (byte) in.readInt8();
					compressed = compress == COMPRESS;
					data = new byte[dataLen];
					pos = 0;
					readCount = dataLen > in.available() ? in.available() : dataLen;
				} else {
					int remain = data.length - pos;
					readCount = remain > in.available() ? in.available() : remain;
				}
				in.read(data, pos, readCount);
				pos += readCount;
				if(pos >= data.length) {
					if(compressed) {
						try {
							data = Compresser.decompress(data);
						} catch (DataFormatException e) {
							throw new ChannelException("decompress failed.");
						}
					}
					Bytes read =  new Bytes(data);
					data = null;
					pos = 0;
					compressed = false;
					return read;
				}
				return null;
			} finally {
				frameLock.unlock();
			}
		}
	}
}
