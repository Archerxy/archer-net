package com.archer.net.p2p;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DataFormatException;

import com.archer.net.Bytes;
import com.archer.net.ChannelContext;
import com.archer.net.ChannelException;
import com.archer.net.handler.Handler;


public class P2PBaseHandler implements Handler {

	protected static final byte COMPRESS = 21;
	protected static final byte UN_COMPRESS = 22;
	
	private static ConcurrentHashMap<String, P2PBlockedMessage> frameCache = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, PeerChannel> channelCache = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, String> serverEndpointMap = new ConcurrentHashMap<>();
	
	private P2PChannel p2pChannel;
	private P2PMessageEvent event;
	
	public P2PBaseHandler(P2PChannel p2pChannel, P2PMessageEvent event) {
		this.event = event;
		this.p2pChannel = p2pChannel;
		p2pChannel.setP2PBaseHandler(this);
	}
	
	@Override
	public void onAccept(ChannelContext ctx) {}
	
	@Override
	public void onConnect(ChannelContext ctx) {
		if(ctx.channel().isClientSide()) {
			
			P2PBlockedMessage frame = toFrameMessage(ctx);
			Bytes host = new Bytes(128);
			host.writeInt32(p2pChannel.getPort());
			
			ctx.toLastOnWrite(frame.appDataWrap(P2PMsgType.CLIENT_PORT, host));
			

			PeerChannel peerChannel = clientNewChannel(ctx);
			if(!peerChannel.getAndSetConnected()) {
				event.onConnect(peerChannel);
			}
		}
	}

	@Override
	public void onRead(ChannelContext ctx, Bytes in) {
		P2PBlockedMessage frame = toFrameMessage(ctx);
		while(in.available() > 0) {
            P2PInnerMessage msg = frame.appDataUnwrap(in);
            
            if(msg != null) {
            	PeerChannel channel = null;
            	//server side message 
    			if(msg.getOp() == P2PMsgType.CLIENT_PORT) {
    				int port = msg.getData().readInt32();
    				channel = serverNewChannel(ctx, port);
    				if(!channel.getAndSetConnected()) {
    					event.onConnect(channel);
    				}
    			} else {
    				channel = getChannel(ctx);
    				if(channel == null) {
    					ctx.close();
    				} else {
        				event.onMessage(channel, msg.getData());
    				}
    			}
            }
        }
	}

	@Override
	public void onWrite(ChannelContext ctx, Bytes out) {
		P2PBlockedMessage frame = toFrameMessage(ctx);
		ctx.toLastOnWrite(frame.appDataWrap(P2PMsgType.USER_DATA, out));
	}

	@Override
	public void onDisconnect(ChannelContext ctx) {
		PeerChannel channel = getChannel(ctx);
		if(channel == null) {
			ctx.close();
		} else {
			event.onDisconnect(channel);
			channel.close();
		}
		String clientEndpoint = channel.getRemoteHost() + ":" + channel.getRemotePort();
		String serverEndpoint = toEndpoint(ctx);
		frameCache.remove(clientEndpoint);
		serverEndpointMap.remove(serverEndpoint);
		frameCache.remove(clientEndpoint);
		frameCache.remove(serverEndpoint);
	}

	@Override
	public void onError(ChannelContext ctx, Throwable t) {
		PeerChannel channel = getChannel(ctx);
		if(channel == null) {
			ctx.close();
		} else {
			event.onError(channel, t);
		}
	}
	
	@Override
	public void onSslCertificate(ChannelContext ctx, byte[] cert) {}

	protected PeerChannel getPeerChannel(EndPoint endpoint) {
		String endpointStr = endpoint.enpoint();
		PeerChannel channel = channelCache.getOrDefault(endpointStr, null);
		if(channel == null) {
			String cliEndpoint = serverEndpointMap.getOrDefault(endpointStr, null);
			if(cliEndpoint == null) {
				return null;
			}
			channel = channelCache.getOrDefault(cliEndpoint, null);
		}
		return channel;
	}
	
	protected void closePeerChannel(EndPoint peer) {
		String endpoint = peer.enpoint();
		PeerChannel channel = channelCache.getOrDefault(endpoint, null);
		if(channel != null) {
			channel.close();
		}
	}
	
	private String toEndpoint(ChannelContext ctx) {
		return ctx.channel().remoteHost() + ":" + ctx.channel().remotePort();
	}
	
	private P2PBlockedMessage toFrameMessage(ChannelContext ctx) {
		P2PBlockedMessage msg = frameCache.getOrDefault(toEndpoint(ctx), null);
		if(msg == null) {
			msg = new P2PBlockedMessage();
			frameCache.put(toEndpoint(ctx), msg);
		}
		return msg;
	}
	
	private PeerChannel getChannel(ChannelContext ctx) {
		String endpoint = P2PUtil.getEndpoint(ctx);
		PeerChannel channel = channelCache.getOrDefault(endpoint, null);
		if(channel == null && !ctx.channel().isClientSide()) {
			String cliEndpoint = serverEndpointMap.getOrDefault(endpoint, null);
			if(cliEndpoint == null) {
				return null;
			}
			channel = channelCache.getOrDefault(cliEndpoint, null);
		}
		return channel;
	}
	
	private PeerChannel clientNewChannel(ChannelContext ctx) {
		String endpoint = P2PUtil.getEndpoint(ctx);
		PeerChannel channel = channelCache.getOrDefault(endpoint, null);
		if(channel == null) {
			if(ctx.channel().isClientSide()) {
				channel = new PeerChannel(ctx);
				channelCache.put(endpoint, channel);
			}
		}
		return channel;
	}
	
	private PeerChannel serverNewChannel(ChannelContext ctx, int port) {
		String endpoint = ctx.channel().remoteHost() + ":" + port;
		PeerChannel channel = channelCache.getOrDefault(endpoint, null);
		if(channel == null) {
			channel = new PeerChannel(ctx, port);
			channelCache.put(endpoint, channel);
		}
		String serverEndpoint = P2PUtil.getEndpoint(ctx);
		serverEndpointMap.put(serverEndpoint, endpoint);
		
		return channel;
	}
	
	private class P2PInnerMessage {

		private P2PMsgType op;
		
		private Bytes data;
		
		public P2PInnerMessage(P2PMsgType op, Bytes data) {
			super();
			this.op = op;
			this.data = data;
		}
		
		public P2PMsgType getOp() {
			return op;
		}
		
		public Bytes getData() {
			return data;
		}
	}
	
	private class P2PBlockedMessage {
		
		private ReentrantLock frameLock = new ReentrantLock(true);

		private byte[] data = null;
		private int pos = 0;
		private boolean compressed = false;
		private P2PMsgType op = null;
		
		public P2PBlockedMessage() {}
		
		public Bytes appDataWrap(P2PMsgType type, Bytes in) {
			byte[] text = in.readAll();
			byte compress;
			if(text.length > Bytes.BUFFER_SIZE) {
				compress = COMPRESS;
				text = Compresser.compress(text);
			} else {
				compress = UN_COMPRESS;
			}
			Bytes out = new Bytes(4 + 2 + text.length);
			out.writeInt32(text.length);
			out.writeInt8(compress);
			out.writeInt8(type.getOp());
			out.write(text);
			return out;
		}
		
		public P2PInnerMessage appDataUnwrap(Bytes in) {
			frameLock.lock();
			try {
				int readCount;
				if(data == null) {
					int dataLen = in.readInt32();
					byte compress = (byte) in.readInt8();
					int opt = in.readInt8();
					op = P2PMsgType.from(opt);
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
					P2PInnerMessage ret = new P2PInnerMessage(op, read);
					data = null;
					pos = 0;
					compressed = false;
					op = null;
					return ret;
				}
				return null;
			} finally {
				frameLock.unlock();
			}
		}
	}
	
	protected static enum P2PMsgType {
		
		CLIENT_PORT(1),
		
		USER_DATA(10);
		
		private int op;

		private P2PMsgType(int op) {
			this.op = op;
		}

		public int getOp() {
			return op;
		}
		
		
		public static P2PMsgType from(int op) {
			for(P2PMsgType val: values()) {
				if(val.getOp() == op) {
					return val;
				}
			}
			
			return USER_DATA;
		}
	}
}

