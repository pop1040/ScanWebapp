package net.pop1040.scan;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

public class MainServer {
	
	public static HashMap<String, StaticResource> staticResources = new HashMap<String, StaticResource>();
	public static boolean colorLoging = false;
	public static boolean liveMode    = false;
	
	public static DeviceList devices;
	
	public static final int PORT = 1337;
	
	public static void main(String[] args) throws IOException {
		
		int selectedPort = PORT;
		boolean colorOverride=false;
		
		
		//Handle command line arguments
		
		for(int i=0; i<args.length; i++){
			String s = args[i];
			if(s.equalsIgnoreCase("--force-color")) {
				if(i++<args.length) {
					s = args[i];
					if(s.equalsIgnoreCase("true") || s.equalsIgnoreCase("t") || s.equals("1"))colorLoging=true;
					colorOverride=false;
				}else {
					System.out.println("--force-color must be set with true, t, 1, false, f or 0");
					System.exit(1);
				}
			}
			if(s.equalsIgnoreCase("--port") || s.equalsIgnoreCase("-p")) {
				if(i++<args.length) {
					s = args[i];
					try {
						selectedPort = Integer.parseInt(s);
					}catch(NumberFormatException e) {
						System.out.println("Unnabel to parse port");
						System.exit(1);
					}
				}else {
					System.out.println("port not specified after " + s);
					System.exit(1);
				}
			}
			if(s.equalsIgnoreCase("--live-mode") || s.equalsIgnoreCase("-l")) {
				liveMode = true;
			}
			if(s.equalsIgnoreCase("--help") || s.equalsIgnoreCase("-h")) {
				System.out.println("Run webserver");
				System.out.println("Flags:");
				System.out.println();
				System.out.println("\t\t--force-color\tSet to true to force colored logs, set to false to disable colored logs");
				System.out.println("\t-p\t--port\t\tSet port, defaults to " + PORT);
				System.out.println("\t-l\t--live-mode\tRun in live mode where static data is read live from the folder. File list is not refreshed");
				System.out.println("\t-h\t--help\t\tShow this screen");
				System.exit(0);
			}
		}
		
		
		//Detect if color logging is supported, unless override is enabled
			
		if(!colorOverride){
			String term = System.getenv("TERM");
			//System.out.println(term + ", " + colorOverride + ", " + colorLoging);
			if(term != null && (term.contains("color") || term.contains("linux") || term.contains("vt100")))colorLoging=true;
		}
		
		
		//Check for srvPath.txt for files, load specified files. If anything doesn't exist, create it and set to default
		
		File srvPath = new File("srvPath.txt");
		File srvDirectory=null;
		boolean flag=false;
		if(srvPath.isFile()) { //see if srvPath.txt already exists
			try {
				FileReader reader = new FileReader(srvPath); //read file
				StringBuilder s = new StringBuilder(); //cuz fast
				int c;
				while((c=reader.read()) != -1)s.append((char)c);
				reader.close();
				srvDirectory = new File(s.toString()); //try to load what was in srvPath.txt as a file
				
				if(!srvDirectory.isDirectory()) {
					if(srvDirectory.mkdirs()) { //If the specified folder didn't exist, makes it
						log((colorLoging?"\u001B[32m"+srvDirectory.getAbsolutePath()+"\u001B[0m":srvDirectory.getAbsolutePath()) + " did not exsist, creating");
						log("This folder should contain all assets");
					}else {
						err("Unable to create the missing directory/containing directories of " + srvDirectory.getAbsolutePath() + ", are you sure you have permission to?");
						err("You can just manually create these directories if you don't want to give this application write permission to "  + srvDirectory.getAbsolutePath());
						System.exit(2);
					}
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				flag=true;
			}
		}else flag=true;
		if(flag){ //handles if srvPath.txt doesn't exist or can't be read
			File dir = srvPath.getAbsoluteFile().getParentFile(); //gets the folder from which relative paths like srvPath.txt are read from
			if(!dir.isDirectory())if(!dir.mkdirs()) { //if this is an issue you have a very special setup
				err("Unable to create srvPath.txt, directory supposed to contain srvPath.txt does not exsist and cannot be created. You've done something very strange as this is normally the same as the location of this executable");
				System.exit(1);
			}
			try {
				PrintWriter w = new PrintWriter(srvPath);
				w.print((srvDirectory = new File("scanFiles/")).getAbsolutePath() + System.lineSeparator()); //makes srvPath.txt since it didn't exist in this scenario, path is set to put a folder called scanFiles in the local directory
				w.close();
				if(!srvDirectory.mkdirs()) {
					err("Unable to create " + srvDirectory.getAbsolutePath() + ", please specify a valid directory in srvPath.txt ("+srvPath.getAbsolutePath()+") or allow the creation of the scanFiles directory");
					err("srvPath.txt needs to exsist and contain the location of server resources");
					System.exit(1); 
				}else {
					log("srvPath.txt (" + (colorLoging?"\u001B[32m"+srvPath.getAbsolutePath()+"\u001B[0m":srvPath.getAbsolutePath()) + ") did not exsist, creating");
					log("server resource directory was set to " + (colorLoging?"\u001B[32m"+srvDirectory.getAbsolutePath()+"\u001B[0m":srvDirectory.getAbsolutePath()));
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				err("Unable to create srvPath.txt ("+srvPath.getAbsolutePath()+"), " + e.getMessage());
				err("srvPath.txt needs to exsist and contain the location of server resources");
				System.exit(1); 
			}
		}
		
		if(srvDirectory == null) { //Sanity check
			err("Critical internal error srvDirectory somehow null, plz tell thomas or fix it youself (this should be open source)");
			System.exit(-1);
		}
		
		
		//Load static web resources
		
		File staticResourcesDir = new File(srvDirectory, "staticResources/");
		boolean loadStaticResources = true;
		
		if(!staticResourcesDir.isDirectory()) {
			if(!staticResourcesDir.mkdir()) {
				err("Unable to make directory \"" + staticResourcesDir.getAbsolutePath() + "\", static resources will not be loaded");
				loadStaticResources=false;
			}else {
				log("Static resources folder not found in " + (colorLoging?"\u001B[32m"+srvDirectory.getAbsolutePath()+"\u001B[0m":srvDirectory.getAbsolutePath()) + ", creating");
			}
		}
		
		if(loadStaticResources) {
			loadStaticResources(staticResourcesDir, "/");
		}
		
		
		//Init the devices
		
		devices = new DeviceList();
		
		devices.init(srvDirectory);
		
		devices.refreshDeviceListJSON();
		
		
		//Init WebServer itself
		
		ServerSocket serverSocket = new ServerSocket(selectedPort);
		
		try {
			//serverSocket.setSoTimeout(10000);
			
			while(true) {
				try {
					Socket socket = serverSocket.accept();
					log("accepted from " + socket.getInetAddress().getHostAddress());
					
					new Thread(new ClientHandler(socket)).start();
					
				}catch (SocketTimeoutException e) {
					err("Client timed out");
					e.printStackTrace();
				}
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		serverSocket.close();
	}
	
	
	/**
	 * Loads the static resources into ram. Clear the staticResources hashmap and call to refresh the cache. Not currently thread safe
	 * @param dir
	 * @param hostPath
	 */
	public static void loadStaticResources(File dir, String hostPath) {
		for(File f : dir.listFiles()) {
			if(f.isDirectory())loadStaticResources(f, hostPath + f.getName() + "/");
			else {
				try {
					byte[] data =  Files.readAllBytes(f.toPath());
					
					byte[] gzipedData = gzip(data);
					
					String type = Files.probeContentType(f.toPath());
					
					/*
					 * Even when live mode is enabled the files are still cached. The cached data is simply not used. 
					 * At some future point live server re-configuration may be added, this should allow toggling between
					 * live mode and static mode, as well as a cache refresh (simply clear the hashmap and call this
					 * function). If this has been implemented please update this comment.  
					 */
					staticResources.put(hostPath + f.getName().toLowerCase(), new StaticResource(f, data, gzipedData, type)); 
					
					log("Loaded "  + (colorLoging?"\u001B[32m" + hostPath + f.getName() + "\u001B[0m" : hostPath + f.getName()));
					
				} catch (IOException e) {
					err("Failed to load " + f.getAbsolutePath() + ", err=\"" + e.getMessage() + "\"");
					e.printStackTrace();
				} catch(OutOfMemoryError e) {
					err("Ran out of ram while trying to load " + f.getAbsolutePath() + ", allocate more memory or put less data in staticResources");
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void log(String s) {
		if(colorLoging)System.out.print("\u001B[33mINFO\u001B[0m:\t");
		else System.out.print("INFO:\t");
		System.out.println(s);
	}
	
	public static void err(String s) {
		if(colorLoging)System.out.print("\u001B[31mERR\u001B[0m:\t");
		else System.out.print("ERR:\t");
		System.out.println(s);
	}

	public static String colorBool(boolean bool) {
		if(colorLoging)return bool ? "\u001B[32mtrue\u001B[0m" : "\u001B[31mfalse\u001B[0m";
		return bool ? "true" : "false";
	}
	
	public static byte[] gzip(byte[] data) {
		
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		GZIPOutputStream gzipStream;
		try {
			gzipStream = new GZIPOutputStream(byteArray);
			gzipStream.write(data);
			gzipStream.flush();
			gzipStream.close();
		} catch (IOException e) {
			err("GZIP critical error, exiting");
			e.printStackTrace();
			System.exit(-1);
		}
		
		return byteArray.toByteArray();
	}
}
