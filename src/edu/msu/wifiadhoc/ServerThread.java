package edu.msu.wifiadhoc;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerThread extends Thread {
	
	//Upper limit of TCP connections
	private int ConnectLimit = 5;	
	
	//Thread handlers
	private RoutingThread routingthread = null;
	private SocketThread[] socketthread = new SocketThread[ConnectLimit];
	
	//The stream which writes data to UI thread or client peers
	private PipedOutputStream out_ui = null;
	private PipedOutputStream[] out_transmit_ct = new PipedOutputStream[ConnectLimit];
	private PipedOutputStream[] out_rcv_ct = new PipedOutputStream[ConnectLimit];

	//The stream which gets data from UI thread or client peers
	private PipedInputStream in_ui = null;
	private PipedInputStream[] in_rcv_ct = new PipedInputStream[ConnectLimit];
	private PipedInputStream[] in_transmit_ct = new PipedInputStream[ConnectLimit];
	
	public ServerThread(PipedOutputStream os,PipedInputStream is) {
		this.out_ui = os;
		this.in_ui = is;
	}
	
	@Override
	public void run() {
		
		ServerSocket serverSocket = null;
		Socket ScktRcv = null;
		int IndexConnect = 0;
		int i;
		
		try {
			//Create pipes for thread communication
			for(i=0;i<ConnectLimit;i++) {
				out_transmit_ct[i] = new PipedOutputStream();
				in_transmit_ct[i] = new PipedInputStream(out_transmit_ct[i]);
				out_rcv_ct[i] = new PipedOutputStream();
				in_rcv_ct[i] = new PipedInputStream(out_rcv_ct[i]);
			}

            //Create a receiver socket and wait for peer connections. This call blocks until a connection is accepted from a peer
            serverSocket = new ServerSocket(8988);
            serverSocket.setSoTimeout(100);	//Set the socket timeout
            
            //Start a routing thread
            routingthread = new RoutingThread(out_ui,in_ui,out_transmit_ct,in_rcv_ct);
            routingthread.setDaemon(true);
            routingthread.start();
            
            //Listen to connection request; start a new thread to handle it when a request is accepted
            while((!this.isInterrupted()) && IndexConnect<ConnectLimit) {
            	try {
	            	ScktRcv = serverSocket.accept();
	            	if(ScktRcv!=null) {
	            		socketthread[IndexConnect] = new SocketThread(IndexConnect,ScktRcv,out_rcv_ct[IndexConnect],in_transmit_ct[IndexConnect]);
	            		socketthread[IndexConnect].setDaemon(true);
	            		socketthread[IndexConnect].start();
	            		IndexConnect++;
	            		
	            		//Notify the server's UI about the network topology update
	            		out_ui.write(2);	//Message Length
	            		out_ui.write(1);	//Message Type
	            		out_ui.write(0x01);	//Message Source
	            		out_ui.write(0x01);	//Message Destination
	            		out_ui.write(IndexConnect);	//Current Sum Number of Clients
	            		out_ui.write(0);	//Destination:server
            			
	            		//Notify all the clients about the network topology update
	            		for(i=0;i<IndexConnect;i++) {
	            			out_transmit_ct[i].write(2);	//Message Length
	            			out_transmit_ct[i].write(1);	//Message Type
	            			out_transmit_ct[i].write(0x01);	//Message Source
	            			out_transmit_ct[i].write((int)(java.lang.Math.pow(2,i+1)));	//Message Destination
	            			out_transmit_ct[i].write(IndexConnect);	//Current Sum Number of Clients
	            			out_transmit_ct[i].write(i+1);	//Destination Client No.
	        			}
	            	}
            	} catch (IOException e) {
            		//Catch Logic
            	}
            }         
        } catch (IOException e) {
            //Catch Logic
        }
		finally {
			//Interrupt the threads
			if(routingthread!=null) {
				routingthread.interrupt();
				routingthread = null;
			}
			for(i=0;i<ConnectLimit;i++) {
				if(socketthread[i]!=null) {
					socketthread[i].interrupt();
					socketthread[i] = null;
				}
			}
			
			//Close the sockets when any exceptions occur
			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					//Catch Logic
				}
			}
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