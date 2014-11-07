package edu.msu.wifiadhoc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class RoutingThread extends Thread {

	//Upper limit of TCP connections
	private int ConnectLimit = 5;
		
	//The stream which writes data to UI thread or client peers
	private DataOutputStream out_ui = null;
	private DataOutputStream[] out_transmit_ct = new DataOutputStream[ConnectLimit];
	
	//The stream which gets data from UI thread or client peers
	private DataInputStream in_ui = null;
	private DataInputStream[] in_rcv_ct = new DataInputStream[ConnectLimit];	
	
	public RoutingThread(PipedOutputStream os_ui,PipedInputStream is_ui,PipedOutputStream[] os_transmit_ct,PipedInputStream[] is_rcv_ct) {
		this.out_ui = new DataOutputStream(os_ui);
		this.in_ui = new DataInputStream(is_ui);
		for(int i=0;i<ConnectLimit;i++) {
			this.out_transmit_ct[i] = new DataOutputStream(os_transmit_ct[i]);
			this.in_rcv_ct[i] = new DataInputStream(is_rcv_ct[i]);
		}
	}
	
	@Override
	public void run() {
		
        int i;
        
		while(!this.isInterrupted()) {
        	//Read data from the input stream and route to corresponding output streams
			MsgForward(in_ui);
			for(i=0;i<ConnectLimit;i++) {
				MsgForward(in_rcv_ct[i]);
			}
        }
	}
	
	//Forward messages according to destinations
	public void MsgForward(DataInputStream in) {
		
		int msg_len;	//Message length
        int msg_type;	//Message type
        int msg_src;	//Message source
        int msg_dst;	//Message destination
        int len;
        byte buf[]  = new byte[1024];
        int i,addr,tmp;
        
        try {
			if(in.available()>0) {
				msg_len = in.read();					
	        	msg_type = in.read();				
	        	msg_src = in.read();					
	        	msg_dst = in.read();	
	        	addr = msg_dst;
	        	len = 0;
	        	while(len<msg_len) {
	        		len += in.read(buf,len,msg_len-len);	//Get the Message Body (message content)
	        	}
	        	for(i=0;i<ConnectLimit+1;i++) {
	        		tmp = addr & (0x01);
		        	if((tmp==0x01)&&i==0) {	//Get the destination; forward it to the corresponding pipe
		        		out_ui.write(msg_len);
		        		out_ui.write(msg_type);
		        		out_ui.write(msg_src);
		        		out_ui.write(msg_dst);
		        		out_ui.write(buf,0,msg_len);
		        	} else if((tmp==0x01)&&i>0) {
		        		out_transmit_ct[i-1].write(msg_len);
		        		out_transmit_ct[i-1].write(msg_type);
		        		out_transmit_ct[i-1].write(msg_src);
		        		out_transmit_ct[i-1].write(msg_dst);
		        		out_transmit_ct[i-1].write(buf,0,msg_len);
		        	}
		        	addr = addr>>1;
	        	}
	    	}
        } catch(IOException e) {
			//Catch Logic
		}
	}
}
