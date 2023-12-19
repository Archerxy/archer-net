# archer-net
network framework written native c, support latest openssl(gmssl) 1.3 
support encrypted key and encrypted certificate 
 
## maven install 
mvn package '-Dmaven.test.skip=true'
mvn install:install-file -DgroupId="com.archer" -DartifactId="archer-net" -Dversion="1.1.3" -Dpackaging=jar -Dfile=<your work dir>\target\archer-net-1.1.3.jar

## gmssl support examples 
``` java 
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

    //event loop
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
        //ca crt
        byte[] caBytes= Files.readAllBytes(Paths.get(ca));

        //server crt
        byte[] crtBytes= Files.readAllBytes(Paths.get(crt));
        byte[] keyBytes= Files.readAllBytes(Paths.get(key));
        byte[] enCrtBytes= Files.readAllBytes(Paths.get(encrt));
        byte[] enKeyBytes= Files.readAllBytes(Paths.get(enkey));
        //client crt
        byte[] clicrtBytes= Files.readAllBytes(Paths.get(clicrt));
        byte[] clikeyBytes= Files.readAllBytes(Paths.get(clikey));
        byte[] enClicrtBytes= Files.readAllBytes(Paths.get(enclicrt));
        byte[] enClikeyBytes= Files.readAllBytes(Paths.get(enclikey));

        //start a gmssl server
        System.out.println("start server.");
        SslContext opt = new SslContext().trustCertificateAuth(caBytes).useCertificate(crtBytes, keyBytes).encrt(enCrtBytes).enkey(enKeyBytes);
        ServerChannel server = new ServerChannel(opt);
        server.eventLoop(loop);
        server.listen(8081);

        // wait
        Thread.sleep(1000);

        //start a gmssl client
        SslContext cliopt = new SslContext().trustCertificateAuth(caBytes).useCertificate(clicrtBytes, clikeyBytes).encrt(enClicrtBytes).enkey(enClikeyBytes);
        Channel cli = new Channel(cliopt);
        cli.eventLoop(loop);
        cli.connect("127.0.0.1", 8081);

        //wait
        Thread.sleep(2000);

        //close client
        cli.close();
				
    } catch (Exception e) {
        e.printStackTrace();
    }
```
## gmssl https server examples 
``` java
    String root = getCurrentWorkDir();

    String ca = root + "crt/ca.crt";
    String key = root + "crt/server.key";
    String crt = root + "crt/server.crt";
    String enkey = root + "crt/server_en.key";
    String encrt = root + "crt/server_en.crt";
		
    byte[] crtBytes= Files.readAllBytes(Paths.get(crt));
    byte[] keyBytes= Files.readAllBytes(Paths.get(key));
    byte[] enCrtBytes= Files.readAllBytes(Paths.get(encrt));
    byte[] enKeyBytes= Files.readAllBytes(Paths.get(enkey));

    System.out.println("start server.");
		
    EventLoop loop = new EventLoop();
    loop.addHandlers(new HttpWrappedHandler() {
	@Override
	public void handle(HttpRequest req, HttpResponse res) throws Exception {
		String uri = req.getUri();
		res.setContentType(ContentType.APPLICATION_JSON);
		if(uri.equals("/nihao")) {
			res.setStatus(HttpStatus.OK);
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
```
## http(s) client examples 
``` java
    NativeResponse baidu = NativeRequest.get("https://www.baidu.com");
    System.out.println(baidu.getStatus());
    System.out.println(new String(baidu.getBody()));

    //request localhost (above server) 
    String root = getCurrentWorkDir();
    String ca = root + "crt/ca.crt";
    NativeResponse localhost = NativeRequest.get("https://127.0.0.1:8080/hihao", new NativeRequest.Options().caPath(ca));
    System.out.println(localhost.getStatus());
    System.out.println(new String(localhost.getBody()));
``` 
