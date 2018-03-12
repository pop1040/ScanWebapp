package net.pop1040.scan.devices;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.LinkedTransferQueue;

import javax.imageio.ImageIO;

import net.pop1040.json.JSONArray;
import net.pop1040.json.JSONObject;
import net.pop1040.scan.ClientHandler.Pair;
import net.pop1040.scan.MainServer;

import static net.pop1040.scan.MainServer.err;
import static net.pop1040.scan.MainServer.log;

public class ScannerScript extends Scanner {
	
	File script;
	File output;
	boolean deleteOnRead;
	
	
	final LinkedTransferQueue<ScannerJob> jobs = new LinkedTransferQueue<ScannerJob>();
	
	final ArrayList<ScannerJob> completedJobs = new ArrayList<ScannerJob>();

	public ScannerScript(String name, JSONObject settings) {
		super(name);
		lock = new Object();
		script = new File(settings.getString("scriptPath"));
		output = new File(settings.getString("outputPath"));
		mode = Mode.IDLE;
		deleteOnRead = settings.getBoolean("deleteOnRead");
	}

	@Override
	public JSONObject getStatus() {
		JSONObject obj = new JSONObject();
		synchronized (lock) {
			obj.setString("mode", mode.getName());
		}
		
		return obj;
	}

	@Override
	public void initThread() {
		
		log("Loaded Scanner Script " + name);
		
		new Thread(() -> {
			while (true) try {
				ScannerJob job = jobs.take();
				synchronized (lock) {
					mode = Mode.SCANNING;
				}
				log("Scanner (" + name + ") scanning");
				MainServer.devices.refreshDeviceListJSON();
					
				try {
					Process p = Runtime.getRuntime().exec(new String[] {script.getAbsolutePath()});
					int code = p.waitFor();
					
					if(code != 0)err("Scanner script exited with a non-zero status (" + (MainServer.colorLoging?"\u001B[31m" + code + "\u001B[0m":code) + ")");
					
					synchronized (lock) {
						mode = Mode.TRANSFERING;
					}
					log("Scanner (" + name + ") retreving image");
					MainServer.devices.refreshDeviceListJSON();
					
					int time = 0;
					while(!output.exists()) {
						try {
							Thread.currentThread().wait(100);
							time++;
						}catch(InterruptedException e) {
							
						}
						if(time>=3000) {
							err("Image was not found");
							synchronized (lock) {
								mode = Mode.ERRORED;
							}
							MainServer.devices.refreshDeviceListJSON();
							return;
						}
					}
					
					BufferedImage img = ImageIO.read(output);
					log("Scanner (" + name + ") loaded image, img = " + img.toString());
					job.img = img;
					if(deleteOnRead)output.delete();

					synchronized (lock) {
						completedJobs.add(job);
						mode = Mode.IDLE;
					}
					MainServer.devices.refreshDeviceListJSON();
					
				} catch (IOException e) {
					err("Scanner Errored IOException");
					e.printStackTrace();
					synchronized (lock) {
						mode = Mode.ERRORED;
					}
					MainServer.devices.refreshDeviceListJSON();
					return;
				}
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	@Override
	public boolean isErrored() {
		synchronized (lock) {
			return mode == Mode.ERRORED;
		}
	}

	@Override
	public JSONArray getJobs(InetAddress addr) {
		JSONArray arr = new JSONArray();
		
		synchronized (lock) {
			for(ScannerJob i : completedJobs)arr.append(new JSONObject().setBoolean("completed", true).setBoolean("canAccess", i.client.equals(addr)).setString("UUID", i.uuid));

			Iterator<ScannerJob> iter = jobs.iterator();
			while(iter.hasNext()) {
				ScannerJob i =iter.next();
				arr.append(new JSONObject().setBoolean("completed", false).setBoolean("canAccess", iter.next().client.equals(addr)).setString("UUID", i.uuid));
			}
		}
		
		return arr;
	}

	@Override
	public JSONObject newJob(InetAddress inetAddress, String content) { //content will in the future contain JSON with job settings to be stored in the job instance
		JSONObject obj = new JSONObject();
		synchronized (lock) {
			String uuid;
			jobs.add(new ScannerJob(inetAddress, uuid=UUID.randomUUID().toString())); //ignore my bad conventions
			obj.setBoolean("success", true);
			obj.setJSONObject("job", new JSONObject().setBoolean("completed", false).setBoolean("canAccess", true).setString("UUID", uuid));
		}
		return obj;
	}

	@Override
	public Pair<byte[], String> downloadJob(InetAddress inetAddress, String content) {
		try {
			JSONObject obj = new JSONObject(content);
			String type    = obj.getString("type");
			String uuid    = obj.getString("uuid");
			boolean base64 = obj.getBoolean("base64");
			
			log("Download request uuid=" + uuid + ", type=" + type);
			
			ScannerJob job=null;

			StringBuilder b = new StringBuilder();
			synchronized (lock) {
				for(ScannerJob i : completedJobs) {
					b.append(", " + i.uuid);
					if(i.uuid.equals(uuid)){
						job=i;
						break;
					}
				}
			}
			
			log("current jobs = " + b.toString());
			
			log("client=" + inetAddress + ", authorized addr=" + (job!=null?job.client:"no job found"));
			if(job==null || !inetAddress.equals(job.client)) {
				log("client not authorized, rejecting");
				return null;
			}
			
			synchronized (lock) {
				completedJobs.remove(job);
			}
			
			switch(type.toLowerCase()){
				case "png":return new Pair<byte[], String>(writeImage(job.img, "png",base64), base64?"text/plain":"image/png");
				case "jpg":return new Pair<byte[], String>(writeImage(job.img, "jpg",base64), base64?"text/plain":"image/jpg");
				case "gif":return new Pair<byte[], String>(writeImage(job.img, "gif",base64), base64?"text/plain":"image/gif");
			}
			
		}catch(RuntimeException e) {
			
		}
		return null;
	}

	private byte[] writeImage(BufferedImage img, String type, boolean base64) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ImageIO.write(img, type, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(base64)return Base64.getEncoder().encode(out.toByteArray());
		return out.toByteArray();
	}
	
	@Override
	public String getType() {
		return "scannerScript";
	}

}
