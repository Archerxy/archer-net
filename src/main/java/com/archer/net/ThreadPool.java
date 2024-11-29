package com.archer.net;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadPool {
	
	private volatile boolean running;
	private PooledThread[] threads;
	
	public ThreadPool(int threadNum) {
		this.threads = new PooledThread[threadNum];
		this.running = false;
	}
	
	public void submit(ThreadPoolTask task) {
		long id = Thread.currentThread().getId();
		PooledThread theThread = null;
		for(int i = 0; i < threads.length; i++) {
			if(threads[i].loopThreadId == 0) {
				theThread = threads[i];
				continue ;
			}
			if(threads[i].loopThreadId == id) {
				theThread = threads[i];
				break ;
			}
		}
		if(theThread == null) {
			int index = 0;
			long time = threads[0].lastAccess;
			for(int i = 1; i < threads.length; i++) {
				if(threads[i].lastAccess > time) {
					time = threads[i].lastAccess;
					index = i;
				}
			}
			threads[index].loopThreadId = id;
			theThread = threads[index];
		}
		theThread.queue.offer(task);
		theThread.lastAccess = System.currentTimeMillis();
		synchronized(theThread.cond) {
			theThread.cond.notify();
		}
	}
	
	
	public void start() {
		if(running) {
			return ;
		}
		running = true;
		for(int i = 0; i < threads.length; i++) {
			threads[i] = new PooledThread(this);
			threads[i].start();
		}
	}
	
	public void stop() {
		this.running = false;
		for(int i = 0; i < threads.length; i++) {
			synchronized(threads[i].cond) {
				threads[i].cond.notifyAll();
			}
		}
	}
	
	public boolean isRunning() {
		return running;
	}
	
	private static class PooledThread extends Thread {
		ConcurrentLinkedQueue<ThreadPoolTask> queue = new ConcurrentLinkedQueue<>();
		ThreadPool pool;
		Object cond = new Object();
		long loopThreadId = 0;
		long lastAccess;
		
	    public PooledThread(ThreadPool pool) {
			this.pool = pool;
		}

		@Override
	    public void run() {
			while(pool.running) {
				ThreadPoolTask task = queue.poll();
				if(task == null) {
					try {
						synchronized(cond) {
							cond.wait();
						}
					} catch (InterruptedException ignore) {}
					
					continue ;
				}
				task.run();
			}
	    }
	}
}

