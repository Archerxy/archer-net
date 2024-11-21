package com.archer.net;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ThreadPool {
	
	private volatile boolean running;
	private Thread[] threads;
	
	private Object cond = new Object();
	private ConcurrentLinkedQueue<ThreadPoolTask> queue = new ConcurrentLinkedQueue<>();
	
	public ThreadPool(int threadNum) {
		this.threads = new Thread[threadNum];
		this.running = false;
	}
	
	public void submit(ThreadPoolTask task) {
		queue.offer(task);
		synchronized(cond) {
			cond.notify();
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
		synchronized(cond) {
			cond.notifyAll();
		}
	}
	
	public boolean isRunning() {
		return running;
	}
	
	private static class PooledThread extends Thread {
		
		ThreadPool pool;
		
	    public PooledThread(ThreadPool pool) {
			this.pool = pool;
		}

		@Override
	    public void run() {
			while(pool.running) {
				ThreadPoolTask task = pool.queue.poll();
				if(task == null) {
					try {
						synchronized(pool.cond) {
							pool.cond.wait();
						}
					} catch (InterruptedException ignore) {}
					
					continue ;
				}
				task.run();
			}
	    }
	}
}

