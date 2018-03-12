package net.pop1040.scan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class StaticResource {
	
	public StaticResource(File file, byte[] data, byte[] gzipData, String contentType) {
		this.file = file;
		this.data = data;
		this.gzipData = gzipData;
		this.contentType = contentType;
	}

	private File file;
	private byte[] data;
	private byte[] gzipData;
	private String contentType;
	
	public byte[] getData() {
		if(MainServer.liveMode){
			try {
				return Files.readAllBytes(file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return data;
	}
	
	public byte[] getGzipData() {
		if(MainServer.liveMode){
			try {
				return MainServer.gzip(Files.readAllBytes(file.toPath()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return gzipData;
	}
	
	public String getContentType() {
		return contentType;
	}

}
