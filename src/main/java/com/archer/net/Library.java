package com.archer.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

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
//			System.load(getCurrentWorkDir() + "src/main/resources/lib/libarnet.dll");
//			System.load(getCurrentWorkDir() + "/lib/libarnet.so");
			
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
		
		Path dst = Paths.get(HOME.toString(), resource);
		if(!Files.exists(dst)) {
			Path parent = dst.getParent();
			if(!Files.exists(parent)) {
				try {
					Files.createDirectories(parent);
				} catch (IOException e) {
					throw new RuntimeException("can not mkdir " + parent.toString());
				}
			}
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
		System.load(dst.toAbsolutePath().toString());
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
}
