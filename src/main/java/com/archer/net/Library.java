package com.archer.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import com.archer.net.util.Sha256Util;

final class Library {

	private static final String WIN_LIB = "lib/libarnet.dll";
	private static final String LINUX_LIB = "lib/libarnet.so";
	
	private static final String WINDOWS = "Windows";
	private static final String LINUX = "Linux";
	
	private static final String P_DIR = ".archer";
	private static final String CUR_OS = System.getProperty("os.name");
	private static final Path HOME = Paths.get(System.getProperty("user.home"), P_DIR);
	
	private static volatile boolean libLoadded = false;
	
	public static void loadDetectLibrary() {
		if(!libLoadded) {
			libLoadded = true;
			loadLib();
		}
	}
	
    public static String getCurrentWorkDir() {
        try {
            return (new File("")).getCanonicalPath() + File.separator;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	
	public static void loadLib() {
		String resource = null;
		if(CUR_OS.contains(WINDOWS)) {
			resource = WIN_LIB;
		} else if(CUR_OS.contains(LINUX)) {
			resource = LINUX_LIB;
		} else {
			throw new RuntimeException("platform "+CUR_OS+" not supported.");
		}
		
		Path dstPath = Paths.get(HOME.toString(), resource);
		try(InputStream stream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(resource);) {
			if(stream == null) {
				throw new RuntimeException("resource not found " + resource);
			}
			byte[] buf = new byte[5 * 1024 * 1024];
			int off = 0, count = 0;
			while((count = stream.read(buf, off, buf.length - off)) >= 0) {
				off += count;
			}
			byte[] src = Arrays.copyOfRange(buf, 0, off);
			boolean needWrite = false;
			if(!Files.exists(dstPath)) {
				needWrite = true;
			} else {
				byte[] dst = Files.readAllBytes(dstPath);
				if(src.length != dst.length) {
					needWrite = true;
				} else {
					byte[] srcHash = Sha256Util.hash(src);
					byte[] dstHash = Sha256Util.hash(dst);
					needWrite = isDifferent(srcHash, dstHash);
				}
			}
			if(needWrite) {
				writeFile(src, dstPath);
			}
		} catch(Exception e) {
			throw new RuntimeException("internal error: " + e.getLocalizedMessage());
		}
		System.load(dstPath.toAbsolutePath().toString());
	}
	
	private static boolean isDifferent(byte[] src, byte[] dst) {
		for(int i = 0; i < 32; i++) {
			if(src[i] != dst[i]) {
				return true;
			}
		}
		return false;
	}
	
	private static void writeFile(byte[] data, Path dst) {
		Path p = dst.getParent();
		if(p != null && !Files.exists(p)) {
			try {
				Files.createDirectories(p);
			} catch (IOException e) {
				throw new RuntimeException("can not mkdir " + p.toString());
			}
		}
		try {
			Files.write(dst, data);
		} catch (IOException e) {
			throw new RuntimeException("internal error: " + e.getLocalizedMessage());
		}
	}
}
