package net.pop1040.scan.devices;

import java.awt.image.BufferedImage;
import java.net.InetAddress;

import net.pop1040.json.JSONObject;
import net.pop1040.scan.DeviceHandler;

public abstract class Scanner extends DeviceHandler{
	
	protected Mode mode;
	Object lock;

	public Scanner(String name) {
		super(name);
	}

	@Override
	public JSONObject getDesc() {
		JSONObject obj = new JSONObject();
		
		obj.setString("name", getName());
		obj.setString("type", getType());
		obj.setString("icon", "/favicon.svg");
		obj.setJSONObject("status", getStatus());
		
		return obj;
	}
	
	public abstract JSONObject getStatus();
	
	public static enum Mode{
		IDLE("Idle"),
		SCANNING("Scanning"),
		TRANSFERING("Transfering"),
		ERRORED("Errored");
		
		String name;
		
		private Mode(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
	}
	
	public static class ScannerJob{
		InetAddress client;
		BufferedImage img;
		String uuid;
		public ScannerJob(InetAddress address, String uuid) {
			client    = address;
			this.uuid = uuid;
		}
	}
	
}
