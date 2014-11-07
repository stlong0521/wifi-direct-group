package edu.msu.wifiadhoc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientThread extends Thread {
	
	private String host;
	private DataOutputStream out;	//The stream which gets data from the socket and piped to UI thread
	private DataInputStream in;		//The stream which gets data from UI thread and writes to socket
	
	public ClientThread(String host,OutputStream os,InputStream is) {
		this.host = host;
		this.out = new DataOutputStream(os);
		this.in = new DataInputStream(is);
	}
	
	@Override
	public void run() {
		//Start to transmit data to another peer
        int port = 8988;
        int len;
        Socket ScktTransmit = null;
        OutputStream outputstream = null;
        InputStream inputstream = null;
        byte buf[]  = new byte[1024];
        boolean IsConnectionSuccess = false;

        while(!IsConnectionSuccess) {
	        try {
	            //Create a client socket with the host, port, and timeout information.
	        	ScktTransmit = new Socket();
	        	ScktTransmit.bind(null);
	        	ScktTransmit.connect((new InetSocketAddress(host, port)), 5000);
	        	outputstream = ScktTransmit.getOutputStream();
	        	inputstream = ScktTransmit.getInputStream();
	        	
	            //Create a byte stream and pipe it to the output stream of the socket.
	        	while(!this.isInterrupted()) {
	        		//Read data from the socket and pipe to UI thread
	        		if(inputstream.available()>0) {
	        			len = inputstream.read(buf);
	        			if(len>0) {
			        		out.write(buf,0,len);
			        	}
	        		}
	            	
	            	//Read data form UI thread and write to the socket
	        		if(in.available()>0) {
	        			len = in.read(buf);
			        	if(len>0) {
			        		outputstream.write(buf,0,len);
			        	}
	        		}
	        	}
	        } catch (IOException e) {
	            //catch logic
	        }
	        //Clean up any open sockets when done transferring or if an exception occurred.
	        finally {
	        	if (outputstream != null) {
	        		try {
	        			outputstream.close();
	        		} catch (IOException e) {
	                    //catch logic
	                }
	        	}
	            if (ScktTransmit != null) {
	            	if (ScktTransmit.isConnected()) {
	            		IsConnectionSuccess = true;
	                }
	            	try {
	                	ScktTransmit.close();
	                } catch (IOException e) {
	                    //catch logic
	                }
	            }
	        }
        }
	}
}