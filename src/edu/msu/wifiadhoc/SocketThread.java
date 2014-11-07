package edu.msu.wifiadhoc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketThread extends Thread{
	
	private int IndexConnect;		//No. of socket thread
	private Socket ScktRcv = null;
	
	private DataOutputStream out;	//The stream which gets data from the socket and piped to server thread
	private DataInputStream in;		//The stream which gets data from server thread and writes to socket
	
	public SocketThread(int indexconnect,Socket sckt,OutputStream os,InputStream is) {
		this.IndexConnect = indexconnect;
		this.ScktRcv = sckt;
		this.out = new DataOutputStream(os);
		this.in = new DataInputStream(is);
	}
	
	@Override
	public void run() {
		
		String message = null;
		int len;
		byte buf[]  = new byte[1024];
		
		try {
			//If this code is reached, a peer has connected and transferred data
	        InputStream inputstream = ScktRcv.getInputStream();   
	        OutputStream outputstream = ScktRcv.getOutputStream();		
	        
	        while(!this.isInterrupted()) {
            	//Read data from the socket and pipe to UI thread or other peer thread
            	if(inputstream.available()>0) {
            		len = inputstream.read(buf);
                	if(len>0) {
                		out.write(buf,0,len);
                	}
            	}
            	
            	//Read data form UI thread or other peer threads and write to the socket
            	if(in.available()>0) {
            		len = in.read(buf);
    	        	if(len>0) {
    	        		outputstream.write(buf,0,len);
    	        	}
        		}
            }
		}catch (IOException e) {
            //Catch Logic
        }
		finally {
			//Close the sockets when any exceptions occur
			if (ScktRcv != null) {
				try {
					ScktRcv.close();
				} catch (IOException e) {
					//Catch Logic
				}
			}
		}
	}
}