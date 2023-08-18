package com.archer.net;

import java.io.IOException;

import com.archer.framework.net.Bytes;
import com.archer.framework.net.Channel;
import com.archer.framework.net.ClientChannel;
import com.archer.framework.net.Handler;

public class ChannelTest {

	public static void main(String[] args) {
		System.out.println("starting.");
		new ServerChannel().listen(8081);
		System.out.println("started.");
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			new ClientChannel("127.0.0.1", 8081).add(new Handler() {

				@Override
				public void onConnect(Channel channel) throws Exception {
					System.out.println("\n*************client log connected.");
					onWrite(channel, new Bytes("cli hello".getBytes()));
				}

				@Override
				public void onRead(Channel channel, Bytes in) throws Exception {
					byte[] msg = in.readAll();
					System.out.println("cli receive:" + new String(msg));
					onWrite(channel, new Bytes("cli hello".getBytes()));
				}

				@Override
				public void onWrite(Channel channel, Bytes out) throws Exception {
					toLastOnWrite(channel, out);
				}

				@Override
				public void onDisconnect(Channel channel) throws Exception {
					
				}

				@Override
				public void onError(Channel channel, Throwable t) {
					t.printStackTrace();
				}

				@Override
				public boolean isFinalHandler() {
					return false;
				}}).connect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
