package net.pop1040.scan;

import java.net.InetAddress;

import net.pop1040.json.JSONArray;
import net.pop1040.json.JSONObject;
import net.pop1040.scan.ClientHandler.Pair;

public abstract class DeviceHandler {
	
	protected String name;
	
	public DeviceHandler(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public abstract void initThread();
	

	/*public JSONObject getDesc() {
		JSONObject obj = new JSONObject();
		
		obj.setString("name", name);
		
		
		return obj;
	}*/
	
	public abstract JSONObject getDesc();
	
	public abstract boolean isErrored();

	public abstract JSONArray getJobs(InetAddress inetAddress);

	public abstract JSONObject newJob(InetAddress inetAddress, String content);

	public abstract Pair<byte[], String> downloadJob(InetAddress inetAddress, String content);
	
	public abstract String getType();

}
