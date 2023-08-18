package com.archer.framework.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

public class ServerChannel {
	
	private Selector selector;
	
    private ServerSocketChannel serverChannel;
    
    private volatile boolean running;
    
    private HandlerWorker worker;
 
    private ServerWorkerThread workerThread;
    
    public ServerChannel() {}
    
    public ServerChannel bind(int port) throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        worker = new HandlerWorker();
        workerThread = new ServerWorkerThread(this);
        return this;
    }
 
    public void start() {
    	workerThread.start();
    }
    
    public void stop() {
    	running = false;
    	selector.wakeup();
    }

    public ServerChannel add(Handler ...handlers) {
    	worker.add(handlers);
    	return this;
    }
    
    public ServerChannel push(Handler handler) {
    	worker.push(handler);
    	return this;
    }
    
    public ServerChannel shift(Handler handler) {
    	worker.shift(handler);
    	return this;
    }
    
    public HandlerWorker handlerWorker() {
    	return worker;
    }
    
    public boolean handlerInitialized() {
    	return worker.handlerInitialized();
    }

    private void run() {
        try {
        	running = true;
            while (running) {
                if(selector.select() <= 0) {
                	continue;
                }
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey sk = it.next();
                    it.remove();
                    if(sk.isValid()) {
                    	handle(sk);
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                serverChannel.close();
                selector.close();
            } catch(Exception ignore) {}
            worker.close();
        }
    }
    
    private void handle(SelectionKey sk) {
		if(sk.isAcceptable()) {
			worker.onAccept(sk);
		} 
		if (sk.isReadable()) {
			worker.onRead(sk);
        }
		if (sk.isWritable()) {
			worker.onWrite(sk);
        }
    }
    
    private static class ServerWorkerThread extends Thread {
    	
    	ServerChannel server;
    	
    	public ServerWorkerThread(ServerChannel server) {
    		this.server = server;
    	}
    	
    	@Override
    	public void run() {
    		server.run();
    	}
    }
}
