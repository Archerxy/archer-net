package com.archer.net.p2p;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.archer.net.Bytes;
import com.archer.net.Channel;
import com.archer.net.HandlerList;
import com.archer.net.ServerChannel;
import com.archer.net.ssl.SslContext;


public class P2PChannel {
	
    private static final long RECONNECT_DELAY = 10 * 1000L;
    private static final String THREAD_NAME = "p2p-reconnect-";
    private ScheduledExecutorService reconnectSchedule;
	
	private Set<EndPoint> peers;
	
	private ServerChannel server;
	private Set<Channel> channels;
	private HandlerList handlerList;
	private SslContext sslCtx;
	
	
	private P2PBaseHandler p2pHandler;
	
	private int port;
	private String host;
	
	private Object channelsLock = new Object();
	private volatile boolean active = false;
	
	public P2PChannel(String host, int port, EndPoint... endpoints) {
		this(host, port, null, endpoints);
	}
	
	public P2PChannel(String host, int port, SslContext sslCtx, EndPoint... endpoints) {
		channels = new LinkedHashSet<>();
		peers = new LinkedHashSet<>();
		this.sslCtx = sslCtx;
		this.port = port;
		this.host = host;
		this.handlerList = new HandlerList();
		this.reconnectSchedule = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(THREAD_NAME + host + ":" + port + "-"));
		
		for(EndPoint peer: endpoints) {
			this.peers.add(peer);
		}
	}
	
	
	public void start(P2PMessageEvent event) {
		handlerList.add(new P2PBaseHandler(this, event));
		server = new ServerChannel(sslCtx);
		server.handlerList(handlerList);
		server.listen(host, port);
		synchronized(channelsLock) {
			for(EndPoint peer: peers) {
				Channel ch = new Channel(sslCtx);
				ch.handlerList(handlerList);
				channels.add(ch);
				ch.connect(peer.host(), peer.port());
			}
		}
		startReconnectScheduler();
		active = true;
	}
	
	public void addPeers(EndPoint... peers) {
		if(!active) {
			throw new P2PException("can not add peers before start P2PChannel.");
		}
		boolean ok = false;
		for(EndPoint peer: peers) {
			ok = true;
			for(EndPoint exist: this.peers) {
				if(exist.enpoint().equals(peer.enpoint())) {
					ok = false;
					break ;
				}
			}
			if(ok) {
				synchronized(channelsLock) {
					this.peers.add(peer);
					Channel ch = new Channel(sslCtx);
					ch.handlerList(handlerList);
					channels.add(ch);
					ch.connect(peer.host(), peer.port());
				}
			}
		}
	}
	
	public void stop() {
		active = false;
		synchronized(channelsLock) {
			server.close();
			for(Channel channel: channels) {
				channel.close();
			}
		}
		reconnectSchedule.shutdown();
	}
	
	private void startReconnectScheduler() {
		reconnectSchedule.scheduleAtFixedRate(
                () -> {
                	for(Channel channel: channels) {
            			System.out.println("判断 " + channel.remoteHost() +":"+ channel.remotePort());
                		if(!channel.isActive()) {
                			System.out.println("重新连接 " + channel.remoteHost() +":"+ channel.remotePort());
                			channel.connect(channel.remoteHost(), channel.remotePort());
                		}
                	}
                },
                RECONNECT_DELAY,
                RECONNECT_DELAY,
                TimeUnit.MILLISECONDS);
	}
	
	public void send(EndPoint peer, Bytes msg) {
		PeerChannel channel = p2pHandler.getPeerChannel(peer);
		if(channel == null) {
			throw new P2PException("peer " + peer.enpoint() + " is not connected.");
		}
		channel.send(msg);
	}
	
	public void remove(EndPoint peer) {
		p2pHandler.closePeerChannel(peer);
		
		Channel theChannel = null;
		for(Channel channel: channels) {
			if(EndPoint.toEndpointString(channel.remoteHost(), channel.remotePort()).equals(peer.enpoint())) {
				theChannel = channel;
				break;
			}
		}
		if(theChannel != null) {
			if(theChannel.isActive()) {
				theChannel.close();
			}
			channels.remove(theChannel);
		}
		
		EndPoint thePeer = null;
		for(EndPoint p: peers) {
			if(p.enpoint().equals(peer.enpoint())) {
				thePeer = p;
				break;
			}
		}
		if(thePeer != null) {
			peers.remove(thePeer);
		}
	}

	public int getPort() {
		return port;
	}

	public String getHost() {
		return host;
	}
	
	protected String toEndpoint() {
		return getHost() + ":" + getPort();
	}
	
	protected void setP2PBaseHandler(P2PBaseHandler handler) {
		this.p2pHandler = handler;
	}
	
	
	private static class NamedThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public NamedThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                                  Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
	}
}

