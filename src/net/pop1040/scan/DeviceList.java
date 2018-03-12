package net.pop1040.scan;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import net.pop1040.json.JSONArray;
import net.pop1040.json.JSONObject;
import net.pop1040.scan.ClientHandler.Pair;
import net.pop1040.scan.devices.ScannerScript;

import static net.pop1040.scan.MainServer.err;
import static net.pop1040.scan.MainServer.log;

public class DeviceList {
	
	ArrayList<DeviceHandler> devices = new ArrayList<DeviceHandler>();
	
	static HashMap<String, IDeviceInstantiator> instantiators = new HashMap<String, IDeviceInstantiator>();
	
	static {
		DeviceList.register("scannerScript", (name, settings) -> new ScannerScript(name, settings)); //Lambda ftw
	}  //If you don't know what a java lambda is, it is basically a really fast way of writing an anonymous inner type
	
	byte[] jsonDeviceList;
	byte[] jsonDeviceListGzip;

	public DeviceList() {
		
	}
	
	byte[] getDeviceListJSON(boolean useGzip) {
		return useGzip ? jsonDeviceListGzip : jsonDeviceList;
	}
	
	public synchronized void refreshDeviceListJSON(){
		JSONArray list = new JSONArray();
		
		for(DeviceHandler d : devices) {
			list.append(d.getDesc());
		}
		
		JSONObject data = new JSONObject();
		data.setJSONArray("devices", list);
		
		String json = data.format(4);
		
		
		jsonDeviceList = json.getBytes();
		jsonDeviceListGzip = MainServer.gzip(jsonDeviceList);
	}

	public void init(File srvDirectory) {
		
		File deviceConfig = new File(srvDirectory, "deviceConfig.json");
		if(deviceConfig.isFile()) {
			try {
				JSONObject baseObj = new JSONObject(new FileReader(deviceConfig));
				JSONArray devices = baseObj.getJSONArray("Devices");
				
				for(int i=0; i<devices.size(); i++) {
					
					JSONObject obj = devices.getJSONObject(i);
					
					String name = null;
					try {
						name = obj.getString("name");
						String type = obj.getString("deviceType");
						JSONObject settings = obj.getJSONObject("settings");
						
						//log("size: " + instantiators.size());
						
						IDeviceInstantiator instantiator = instantiators.get(type);
						if(instantiator == null)throw new RuntimeException("unknown device type, " + type + ", please check deviceConfig.js");
						
						this.devices.add(instantiator.instantiate(name, settings));
						
					}catch(RuntimeException e) {
						err("Unable to load device #" + i + " (the first device is #0), "+(name==null?"name could not be loaded":"name = " + name)+", error = " + e.getMessage());
					}
					
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (RuntimeException e) {
				err("Runtime exception while loading device list, msg=" + e.getMessage());
				System.exit(2);
			}
		}else {
			try {
				JSONObject empty = new JSONObject();
				empty.setJSONArray("Devices", new JSONArray());
				PrintWriter w = new PrintWriter(deviceConfig);
				w.println(empty.format(4));
				w.close();
				log("deviceConfig.json (\" + (MainServer.colorLoging?\"\\u001B[32m\"+deviceConfig.getAbsolutePath()+\"\\u001B[0m\":deviceConfig.getAbsolutePath()) + \") did not exist, creating");
			} catch (FileNotFoundException e) {
				err("deviceConfig.json does not exist and unable to create deviceConfig.json (" + (MainServer.colorLoging?"\u001B[32m"+deviceConfig.getAbsolutePath()+"\u001B[0m":deviceConfig.getAbsolutePath()) + "), exiting");
				System.exit(2);
			}
		}
		if(devices.isEmpty())log("No current devices are loaded, this server isn't going to do too much");
		
		
		
		
		for(DeviceHandler d : this.devices) {
			
			d.initThread();
			
		}
		
	}

	public static void register(String type, IDeviceInstantiator instantiator) {
		instantiators.put(type, instantiator);
	}
	
	public static interface IDeviceInstantiator{
		public DeviceHandler instantiate(String name, JSONObject settings);
	}
	
	public JSONObject getJobs(int deviceIndex, InetAddress inetAddress) {
		if(deviceIndex>=devices.size() || deviceIndex < 0)return null;
		return new JSONObject().setJSONArray("jobs", devices.get(deviceIndex).getJobs(inetAddress));
	}

	public JSONObject newJob(int deviceIndex, InetAddress inetAddress, String content) {
		if(deviceIndex>=devices.size() || deviceIndex < 0)return null;
		return devices.get(deviceIndex).newJob(inetAddress, content);
	}

	public Pair<byte[], String> downloadJob(int deviceIndex, InetAddress inetAddress, String content) {
		if(deviceIndex>=devices.size() || deviceIndex < 0)return null;
		return devices.get(deviceIndex).downloadJob(inetAddress, content);
	}
	
}
