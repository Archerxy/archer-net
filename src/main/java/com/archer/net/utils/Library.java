package com.archer.net.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Library {
	public static final String WINDOWS = "Windows";
	public static final String LINUX = "Linux";
	
	public static final String P_DIR = ".archer";
	public static final String CUR_OS = System.getProperty("os.name");
	public static final Path HOME = Paths.get(System.getProperty("user.home"), P_DIR);
	
	
	public static void loadLib(String winResouce, String linuxResouce) {
		String resource = null;
		if(CUR_OS.contains(WINDOWS)) {
			resource = winResouce;
		} else if(CUR_OS.contains(LINUX)) {
			resource = linuxResouce;
		} else {
			throw new RuntimeException("platform "+CUR_OS+" not supported.");
		}
		
		if(!Files.exists(HOME)) {
			try {
				Files.createDirectories(HOME);
			} catch (IOException e) {
				throw new RuntimeException("can not mkdir " + HOME.toString());
			}
		}
		
		Path dst = Paths.get(HOME.toString(), resource);
		if(!Files.exists(dst)) {
			try(InputStream stream = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(resource);) {
				byte[] resourceBytes = new byte[stream.available()];
				int off = 0, count = 0;
				while((count = stream.read(resourceBytes, off, resourceBytes.length - off)) > 0) {
					off += count;
				}
				writeFile(resourceBytes, dst);
			} catch(Exception e) {
				throw new RuntimeException("internal error: " + e.getLocalizedMessage());
			}
		}
		loadLib(dst.toAbsolutePath().toString());
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
			Files.write(dst, data, StandardOpenOption.CREATE);
		} catch (IOException e) {
			throw new RuntimeException("internal error: " + e.getLocalizedMessage());
		}
	}
	
	public static void loadLib(String path) {
		System.load(path);
	}
}
