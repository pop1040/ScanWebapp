package net.pop1040.scan;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import net.pop1040.json.JSONObject;

import static net.pop1040.scan.MainServer.log;
import static net.pop1040.scan.MainServer.err;

@SuppressWarnings("unused")
public class ClientHandler implements Runnable {
	
	public static SimpleDateFormat dateFormater = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss", Locale.US);
	Socket socket;
	
	public ClientHandler(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			InputStream  in  = new BufferedInputStream(socket.getInputStream());
			OutputStream out = new BufferedOutputStream(socket.getOutputStream());
			
			//Scanner input = new Scanner(in);
			String content=null;
			
			ArrayList<String> header = new ArrayList<String>();
			while(true) {
				//String s = input.nextLine();
				StringBuilder s = new StringBuilder();
				int c;
				while((c = in.read()) != -1 && ((char)c)!='\n' && s.length() < 32768)s.append((char)c);
				if(s.toString().trim().length()==0 || header.size() > 128)break;
				header.add(s.toString());
			}
			
			//String request = input.nextLine();
			log("New Connection");
			log("Address = " + socket.getInetAddress().getHostAddress());
			//System.out.println(request);
			for(String s : header)System.out.println(s);
			
			
			if(header.isEmpty()) {
				//input.close();
				socket.close();
				return;
			}
			
			String[] head = header.get(0).split(" ");
			if(head.length != 3) {
				socket.close();
				return;
			}
			
			String requestType = head[0];
			String path        = head[1];
			String version     = head[2];
			
			
			HashMap<String, ArrayList<String>> headerMeta = new HashMap<String, ArrayList<String>>();
			
			for(int i=1; i<header.size(); i++) {
				String s = header.get(i);
				//System.out.println(s);
				int index = s.indexOf(':');
				if(index != -1) {
					String name = s.substring(0, index).trim().toLowerCase();
					String field = s.substring(index+1).toLowerCase();
					if(name.equals("user-agent")) {
						headerMeta.put(name, new ArrayList<String>(Arrays.asList(field)));
						continue;
					}
					ArrayList<String> existingData = headerMeta.get(name);
					if(existingData == null) {
						ArrayList<String> newData = new ArrayList<String>();
						for(String f : field.split(","))newData.add(f.trim());
						headerMeta.put(name, newData);
					}else {
						for(String f : field.split(","))existingData.add(f.trim());
					}
				}
			}
			

			boolean useGzip=false;
			
			ArrayList<String> contentLength = headerMeta.get("content-length");
			if(contentLength != null && contentLength.size()==1 && requestType.equals("POST")) {
				
				try {
					int contentSize = Integer.parseInt(contentLength.get(0).trim());
					
					StringBuilder s = new StringBuilder();

					int c;
					for(int i=0; i<contentSize; i++)if((c = in.read()) != -1)s.append((char)c);else break; //stream reading is always fun
					
					content = s.toString();
					
				}catch(NumberFormatException e) {
					err("Bad Content Length Size \"" + contentLength.get(0) + "\"");
				}
				
			}
			
			ArrayList<String> encodingSupport = headerMeta.get("accept-encoding");
			if(encodingSupport != null){
				
				ArrayList<String> formats = new ArrayList<String>();
				HashMap<String, Double> priority = new HashMap<String, Double>();
				
				for(String s : encodingSupport) {
					String[] v = s.split(";");
					if(v.length != 0) {
						String formatName = v[0];
						formats.add(formatName);
						boolean flag=true;
						for(int i=1; i<v.length; i++) {
							int equalIndex = v[i].indexOf('=');
							if(v[i].substring(0, equalIndex).toLowerCase().equals("q") && v[i].substring(equalIndex+1).length() > 0) {
								try {
									double p = Double.parseDouble(v[i].substring(equalIndex)+1);
									if(p <= 1.0 && p >= 0.0) {
										if(flag) {
											flag=false;
											priority.put(formatName, Double.valueOf(p));
										}else System.err.println("priority specified twice for " + formatName + " (" + s + ")");
									}
								}catch(NumberFormatException e) {
									System.err.println("Bad encoding format priority for " + formatName + ", " + v[i]);
								}
							}
						}
						if(flag)priority.put(formatName, Double.valueOf(1.0));
					}else System.err.println("Bad encoding format \"" + s + "\"");
				}
				
				System.out.println("\naccepted formats: ");
				int maxL=0;
				for(String s : formats)if(s.length() > maxL)maxL=s.length();
				for(String s : formats)System.out.println(String.format("%"+maxL+"s, with priority " + priority.get(s), s));
				System.out.println();
				
				if(content != null) {
					System.out.println("Content:");
					System.out.println(content);
					System.out.println();
				}
				
				/*double maxPriority = 0;
				for(String s : formats) {
					Double d = priority.get(s);
					if(d != null && maxPriority > d.doubleValue())maxPriority=d.doubleValue();
				}
				*/
				double gzipP    =-1d;
				double anyP     =-1d;
				double identityP=-1d;
				for(String s : formats) {
					if(s.equals("gzip"    ) && priority.get(s) != null)gzipP     = priority.get(s).doubleValue();
					if(s.equals("identity") && priority.get(s) != null)identityP = priority.get(s).doubleValue();
					if(s.equals("*"       ) && priority.get(s) != null)anyP      = priority.get(s).doubleValue();
				}
				if(gzipP > identityP && gzipP > anyP && gzipP != -1d)useGzip=true;
			}
			
			boolean isFirefox=false;
			
			{
				ArrayList<String> userAgent = headerMeta.get("user-agent");
				if(userAgent != null && userAgent.size()==1) {
					String agent = userAgent.get(0);
					isFirefox = agent.contains("firefox/") && !agent.contains("seamonkey/");
					System.out.println((MainServer.colorLoging?"User agent: \u001B[31m":"User agent: ") + agent.trim() + (MainServer.colorLoging?"\u001B[0m, isFirefox=":", isFirefox=") + MainServer.colorBool(isFirefox));
				}
			}
			
			//Handle devices requests to actually control devices
			
			if(path.toLowerCase().startsWith("/devices/")) {
				
				String devicePath = path.substring(9).toLowerCase();
//				log(devicePath);
				
				if(devicePath.equalsIgnoreCase("listDevices")) {
					
					byte[] data = getDeviceList(useGzip);
					
					out.write((
							"HTTP/1.1 200 OK\n" + 
							getDate() + 
							(useGzip?"Content-Encoding: gzip\n":"Content-Encoding: identity\n") +
							"Content-Type: application/json; charset=utf-8\n" +
							"Content-Language: en-US\n" +
							"Content-Length: " + data.length + "\n" +
							"Connection: close\n" +
							"\n"
									).getBytes());
					out.write(data);
					out.flush();
					out.close();
					return;
				}else if (devicePath.startsWith("jobs/")) {
					try {
						int deviceIndex = Integer.parseInt(devicePath.substring(5));
						
						JSONObject obj = MainServer.devices.getJobs(deviceIndex, socket.getInetAddress());
						
						if(obj != null) {
							byte[] data = obj.format(4).getBytes();
							if(useGzip)data = MainServer.gzip(data);
							
							out.write((
									"HTTP/1.1 200 OK\n" + 
									getDate() + 
									(useGzip?"Content-Encoding: gzip\n":"Content-Encoding: identity\n") +
									"Content-Type: application/json; charset=utf-8\n" +
									"Content-Language: en-US\n" +
									"Content-Length: " + data.length + "\n" +
									"Connection: close\n" +
									"\n"
											).getBytes());
							out.write(data);
							out.flush();
							out.close();
							
						}
					}catch(NumberFormatException e) {
						
					}
				}else if(devicePath.startsWith("startjob/") && content != null) {
					try {
//						System.out.println(devicePath.substring(9));
						int deviceIndex = Integer.parseInt(devicePath.substring(9));
						
						JSONObject obj = MainServer.devices.newJob(deviceIndex, socket.getInetAddress(), content);
						
						if(obj != null) {
							byte[] data = obj.format(4).getBytes();
							if(useGzip)data = MainServer.gzip(data);
							
							out.write((
									"HTTP/1.1 200 OK\n" + 
									getDate() + 
									(useGzip?"Content-Encoding: gzip\n":"Content-Encoding: identity\n") +
									"Content-Type: application/json; charset=utf-8\n" +
									"Content-Language: en-US\n" +
									"Content-Length: " + data.length + "\n" +
									"Connection: close\n" +
									"\n"
											).getBytes());
							out.write(data);
							out.flush();
							out.close();
							
						}
					}catch(NumberFormatException e) {
						
					}
				}else if(devicePath.startsWith("downloadjob/")) {
					try {
//						System.out.println(devicePath.substring(12));
						String urlData = devicePath.substring(12);
						String[] split = urlData.split("/");
						if(split.length==3) {
							content = "{\"type\":\"" + split[2] + "\",\"uuid\":\"" + split[1] + "\",\"base64\":false}"; //so inefficient but whatever am in a hurry
						}
						if(split.length > 0) {
							
							int deviceIndex = Integer.parseInt(split[0]);
							
							
							Pair<byte[], String> pair = MainServer.devices.downloadJob(deviceIndex, socket.getInetAddress(), content);
							
							if(pair != null) {
	
								byte[] data=pair.a;
								String mimeType = pair.b;
								
								//byte[] data = obj.format(4).getBytes();
								if(useGzip)data = MainServer.gzip(data);
								
								out.write((
										"HTTP/1.1 200 OK\n" + 
										getDate() + 
										(useGzip?"Content-Encoding: gzip\n":"Content-Encoding: identity\n") +
										"Content-Type: " + mimeType + (mimeType.startsWith("text")||mimeType.startsWith("applicationo") ? "; charset=utf-8" : "") + "\n" +
										"Content-Language: en-US\n" +
										"Content-Length: " + data.length + "\n" +
										"Connection: close\n" +
										"\n"
												).getBytes());
								out.write(data);
								out.flush();
								out.close();
								
							}
						}
					}catch(NumberFormatException e) {
						
					}
				}
				
			}
			
			int lastSlash = path.lastIndexOf('/');
			int lastDot   = path.lastIndexOf('.');
			
			if(lastSlash == path.length()-1)path = path + "index.html"; // make /thing/ get from /thing/index.html, so the default is /index.html when you just go to the ip
			else if(lastSlash > lastDot)path = path + ".html"; //no need for file extensions if its an html page we are looking for
			
			
			
			if(MainServer.staticResources.containsKey(path.toLowerCase())) {
				
				StaticResource r = MainServer.staticResources.get(path.toLowerCase());
				out.write((
						"HTTP/1.1 200 OK\n" + 
						getDate() + 
						(useGzip?"Content-Encoding: gzip\n":"Content-Encoding: identity\n") +
						"Content-Type: " + (r.getContentType().startsWith("text")?r.getContentType() + "; charset=utf-8\n": r.getContentType() + '\n') +
						"Content-Language: en-US\n" +
						"Content-Length: " + (useGzip?r.getGzipData():r.getData()).length + '\n' +
						"Connection: close\n" +
						"\n"
								).getBytes());
				
				out.write(useGzip?r.getGzipData():r.getData());
				out.flush();
				out.close();
				return;
			}
			out.write((
					"HTTP/1.1 404 Not Found\n" + 
					getDate() + 
					(useGzip?"Content-Encoding: gzip\n":"Content-Encoding: identity\n") +
					"Content-Type: text/html; charset=utf-8\n" +
					"Content-Language: en-US\n" +
					"Connection: close\n" +
					"\n"
					).getBytes());
			
			if(useGzip) {
				System.out.println("Using Gzip");
				out = new GZIPOutputStream(out);
			}
			if(isFirefox)System.out.println("isFirefox");
			
			
			PrintWriter output = new PrintWriter(out);
			output.println("<html><head>");
			output.println(isFirefox?"<link rel=\"icon\" type=\"image/svg+xml\" href=\"/favicon.svg\">":"<link rel=\"icon\" type=\"image/png\" href=\"/favicon.ico\">");
			output.println("<title>404 Not Found</title></head><body><center><h1> " + path + " Not Found</h1></center></body></html>");
			output.flush();

			output.close();
			in.close();
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
		
		if(socket != null)try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static byte[] getDeviceList(boolean useGzip) {
		return MainServer.devices.getDeviceListJSON(useGzip);
	}

	public static String getDate() {
		StringBuilder b = new StringBuilder("Date: ");
		b.append(dateFormater.format(Calendar.getInstance().getTime()));
		b.append(" GMT\n");
		return b.toString();
	}
	
	public static class Pair<T, G>{
		public T a;
		public G b;
		public Pair(T a, G b){
			this.a=a;
			this.b=b;
		}
	}
	
}
