package test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import com.archer.net.Bytes;
import com.archer.net.Channel;
import com.archer.net.ChannelContext;
import com.archer.net.Debugger;
import com.archer.net.EventLoop;
import com.archer.net.ServerChannel;
import com.archer.net.handler.AbstractWrappedHandler;
import com.archer.net.handler.FrameReadHandler;
import com.archer.net.http.ContentType;
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpResponse;
import com.archer.net.http.HttpStatus;
import com.archer.net.http.HttpWrappedHandler;
import com.archer.net.http.NativeRequest;
import com.archer.net.http.NativeResponse;
import com.archer.net.message.Message;
import com.archer.net.ssl.NamedCurve;
import com.archer.net.ssl.ProtocolVersion;
import com.archer.net.SslContext;


public class ChannelTest {

	static {
		Debugger.setMode(true);
	}
	

	public static class Msg implements Message {
		
		String data;
		
		public void decode(Bytes in) {
			System.out.println("read in: " + in.at(0) + "-" + in.at(1));
			int len = in.readInt16();
			System.out.println("read in: len = " + len);
			byte[] bytes = in.read(len);
			data = new String(bytes, StandardCharsets.UTF_8);
		}
		
		public Bytes encode() {
			byte[] msg = "nihao, shishilani".getBytes(StandardCharsets.UTF_8);
			Bytes out = new Bytes();
			System.out.println("write out: len = " + msg.length);
			out.writeInt16(msg.length);
			out.write(msg);
			System.out.println("write out: " + out.at(0) + "-" + out.at(1));
			return out;
		}
	}

	public static void singleTextChannel() {
		
		EventLoop loop = new EventLoop();
		loop.addHandlers(
				new FrameReadHandler(0, 2, 2),
				new AbstractWrappedHandler<Msg>() {
					@Override
					public void onMessage(ChannelContext ctx, Msg input) {
						Channel channel = ctx.channel();
						String host = channel.remoteHost();
						int port = channel.remotePort();
						System.out.println(host+":"+port+" receive: " + input.data);
					}
					@Override
					public Msg decode(Bytes in) {
						Msg msg = new Msg();
						msg.decode(in);
						return msg;
					}
					
					@Override
					public void onConnect(ChannelContext ctx) {
						Channel channel = ctx.channel();
						String host = channel.remoteHost();
						int port = channel.remotePort();
						System.out.println(host+":"+port+" connected.");
						ctx.write(new Msg());
					}
					@Override
					public void onDisconnect(ChannelContext ctx) {
						Channel channel = ctx.channel();
						String host = channel.remoteHost();
						int port = channel.remotePort();
						System.out.println(host+":"+port+" disconnected.");
					}
					@Override
					public void onError(ChannelContext ctx, Throwable t) {
						t.printStackTrace();
					}
					@Override
					public void onAccept(ChannelContext ctx) {
					}
				});
		
		ServerChannel server = new ServerChannel();
		server.eventLoop(loop);
		server.listen(8081);
		

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("start to connect.");
		Channel cli = new Channel();
		cli.eventLoop(loop);
		cli.connect("127.0.0.1", 8081);
		

		System.out.println("start to connect.");
		Channel cli2 = new Channel();
		cli2.eventLoop(loop);
		cli2.connect("127.0.0.1", 8081);
		
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		cli.close();
	}
	
	public static void tlsTest() {

		String root = getCurrentWorkDir();

		String ca = root + "crt/ca.crt";
		String key = root + "crt/server.key";
		String crt = root + "crt/server.crt";
		String enkey = root + "crt/server_en.key";
		String encrt = root + "crt/server_en.crt";
		String clikey = root + "crt/cli.key";
		String clicrt = root + "crt/cli.crt";
		String enclikey = root + "crt/cli_en.key";
		String enclicrt = root + "crt/cli_en.crt";

		
		EventLoop loop = new EventLoop();
		loop.addHandlers(
				new FrameReadHandler(0, 2, 2),
				new AbstractWrappedHandler<Msg>() {
					@Override
					public void onMessage(ChannelContext ctx, Msg input) {
						System.out.println("Handler: receive: " + input.data);
					}
					@Override
					public Msg decode(Bytes in) {
						Msg msg = new Msg();
						msg.decode(in);
						return msg;
					}
					
					@Override
					public void onConnect(ChannelContext ctx) {
						Channel channel = ctx.channel();
						String host = channel.remoteHost();
						int port = channel.remotePort();
						System.out.println("Handler: " + host+":"+port+" connected.");
						ctx.write(new Msg());
					}
					@Override
					public void onDisconnect(ChannelContext ctx) {
						Channel channel = ctx.channel();
						String host = channel.remoteHost();
						int port = channel.remotePort();
						System.out.println("Handler: " + host+":"+port+" disconnected.");
					}
					@Override
					public void onError(ChannelContext ctx, Throwable t) {
						t.printStackTrace();
					}
					@Override
					public void onAccept(ChannelContext ctx) {
						Channel channel = ctx.channel();
						String host = channel.remoteHost();
						int port = channel.remotePort();
						System.out.println(host+":"+port+" accepted.");
					}
				});
		try {
			byte[] caBytes= Files.readAllBytes(Paths.get(ca));
			byte[] crtBytes= Files.readAllBytes(Paths.get(crt));
			byte[] keyBytes= Files.readAllBytes(Paths.get(key));
//			byte[] enCrtBytes= Files.readAllBytes(Paths.get(encrt));
//			byte[] enKeyBytes= Files.readAllBytes(Paths.get(enkey));
			byte[] clicrtBytes= Files.readAllBytes(Paths.get(clicrt));
			byte[] clikeyBytes= Files.readAllBytes(Paths.get(clikey));
//			byte[] enClicrtBytes= Files.readAllBytes(Paths.get(enclicrt));
//			byte[] enClikeyBytes= Files.readAllBytes(Paths.get(enclikey));
			
			System.out.println("start server.");
			SslContext opt = new SslContext().trustCertificateAuth(caBytes).useCertificate(crtBytes, keyBytes)  //; //.encrt(enCrtBytes).enkey(enKeyBytes);
					.namedCurves(new NamedCurve[] {NamedCurve.NID_secp256k1});
			ServerChannel server = new ServerChannel(opt);
			server.eventLoop(loop);
			server.listen(8081);
		
			Thread.sleep(1000);

			SslContext cliopt = new SslContext().trustCertificateAuth(caBytes).useCertificate(clicrtBytes, clikeyBytes) //; //.encrt(enClicrtBytes).enkey(enClikeyBytes);
					.namedCurves(new NamedCurve[] {NamedCurve.NID_secp256k1});
			Channel cli = new Channel(cliopt);
			cli.eventLoop(loop);
			cli.connect("127.0.0.1", 8081);
			
			System.out.println("Java after connect");
			

			Thread.sleep(1000);
			Channel cli2 = new Channel(cliopt);
			cli2.eventLoop(loop);
			cli2.connect("127.0.0.1", 8081);
			
			try {
				Thread.sleep(6000);
				System.out.println("Java after sleep");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			cli.close();
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    public static String getCurrentWorkDir() {
        try {
            return (new File("")).getCanonicalPath() + File.separator;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	
	public static void httpClientTest() throws Exception {

		String root = getCurrentWorkDir();

		String ca = root + "crt/ca.crt";
		String key = root + "crt/server.key";
		String crt = root + "crt/server.crt";
		String enkey = root + "crt/server_en.key";
		String encrt = root + "crt/server_en.crt";
		String clikey = root + "crt/cli.key";
		String clicrt = root + "crt/cli.crt";
		String enclikey = root + "crt/cli_en.key";
		String enclicrt = root + "crt/cli_en.crt";
		
		
		byte[] caBytes= Files.readAllBytes(Paths.get(ca));
		byte[] crtBytes= Files.readAllBytes(Paths.get(crt));
		byte[] keyBytes= Files.readAllBytes(Paths.get(key));
//		byte[] enCrtBytes= Files.readAllBytes(Paths.get(encrt));
//		byte[] enKeyBytes= Files.readAllBytes(Paths.get(enkey));
		byte[] clicrtBytes= Files.readAllBytes(Paths.get(clicrt));
		byte[] clikeyBytes= Files.readAllBytes(Paths.get(clikey));
//		byte[] enClicrtBytes= Files.readAllBytes(Paths.get(enclicrt));
//		byte[] enClikeyBytes= Files.readAllBytes(Paths.get(enclikey));
		
//		byte[] body = "{\"nihao\":\"wo\"}".getBytes();
		
//		NativeResponse res = NativeRequest.get("https://api.weixin.qq.com/sns/jscode2session?appid=wx7044ac0149b3f437&secret=1fde4b8778750f7a3267bb8dfa23fe9e&js_code=0d3off1w3nyLo13j411w3L9cJQ1off1R&grant_type=authorization_code");
//		NativeResponse res = NativeRequest.get("https://www.zhihu.com");
//		System.out.println(res.getStatus());
//		System.out.println(new String(res.getBody()));
		
		for(int i = 0; i < 10; i++) {
//			NativeResponse res = NativeRequest.get("https://api.weixin.qq.com/sns/jscode2session?appid=wx7044ac0149b3f437&secret=1fde4b8778750f7a3267bb8dfa23fe9e&js_code=0d3off1w3nyLo13j411w3L9cJQ1off1R&grant_type=authorization_code");
//			NativeResponse res = NativeRequest.get("https://127.0.0.1:8765");
			NativeResponse res = NativeRequest.get("https://www.zhihu.com");
			System.out.println(i+" " + res.getStatus());
		}
//		byte[] file = Files.readAllBytes(Paths.get("E:\\WASM智能合约技术介绍.pptx"));
//		byte[] body = ("{\"nihao\":\""+ new String(file)+"\"}").getBytes();
//		NioResponse res = NioRequest.post("https://localhost:8888/nihao", body, new NioRequest.Options().verifyCert(false));
//		System.out.println("*********");
//		System.out.println(res.getResponseString());
	}
	

	public static void httpServerTest() throws Exception {

		String root = getCurrentWorkDir();

		String ca = root + "crt/ca.crt";
		String key = root + "crt/server.key";
		String crt = root + "crt/server.crt";
		String enkey = root + "crt/server_en.key";
		String encrt = root + "crt/server_en.crt";
		String clikey = root + "crt/cli.key";
		String clicrt = root + "crt/cli.crt";
		String enclikey = root + "crt/cli_en.key";
		String enclicrt = root + "crt/cli_en.crt";
		
		
		byte[] caBytes= Files.readAllBytes(Paths.get(ca));
		byte[] crtBytes= Files.readAllBytes(Paths.get(crt));
		byte[] keyBytes= Files.readAllBytes(Paths.get(key));
//		byte[] enCrtBytes= Files.readAllBytes(Paths.get(encrt));
//		byte[] enKeyBytes= Files.readAllBytes(Paths.get(enkey));
		byte[] clicrtBytes= Files.readAllBytes(Paths.get(clicrt));
		byte[] clikeyBytes= Files.readAllBytes(Paths.get(clikey));
//		byte[] enClicrtBytes= Files.readAllBytes(Paths.get(enclicrt));
//		byte[] enClikeyBytes= Files.readAllBytes(Paths.get(enclikey));
		
		System.out.println("start server.");
		
		EventLoop loop = new EventLoop();
		loop.addHandlers(new HttpWrappedHandler() {
			@Override
			public void handle(HttpRequest req, HttpResponse res) throws Exception {
				String uri = req.getUri();
				res.setContentType(ContentType.APPLICATION_JSON);
				System.out.println("header len = " + req.getContentLength() + ", real len = " + req.getContent().length);
				if(uri.equals("/nihao")) {
					res.setStatus(HttpStatus.OK);
//					byte[] file = Files.readAllBytes(Paths.get("E:\\WASM智能合约技术介绍.pptx"));
//					byte[] body = ("{\"nihao\":\""+ new String(file)+"\"}").getBytes();
//					res.setContent(body);
					res.setContent("{\"nihao\":\"ni\"}".getBytes());
				} else {
					res.setStatus(HttpStatus.NOT_FOUND);
					res.setContent("{\"nihao\":\"ni\"}".getBytes());
				}
			}

			@Override
			public void handleException(HttpRequest req, HttpResponse res, Throwable t) {
				t.printStackTrace();
				String body = "{" +
						"\"server\": \"Java/"+System.getProperty("java.version")+"\"," +
						"\"time\": \"" + LocalDateTime.now().toString() + "\"," +
						"\"status\": \"" + HttpStatus.SERVICE_UNAVAILABLE.getStatus() + "\"" +
					"}";

				res.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
				res.setContentType(ContentType.APPLICATION_JSON);
				res.setContent(body.getBytes());
			}
		});

		SslContext opt = new SslContext().trustCertificateAuth(caBytes).useCertificate(crtBytes, keyBytes);
		ServerChannel server = new ServerChannel(opt);
		server.eventLoop(loop);
		server.listen(8080);
	}
	
	public static void main(String[] args) {
//		if(args == null || args.length == 0) {
//			linuxTlsTest();
//		} else {
//			String m = args[0];
//			if("one".equals(m)) {
//				singleTextChannel();
//			} else if("two".equals(m)) {
//				testTextChannel();
//			}
//		}
		
//		singleTextChannel();
//		tlsTest();
		
		try {
//			httpServerTest();
			httpClientTest();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
