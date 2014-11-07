package edu.msu.wifiadhoc;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	//Instantiate an IntentFilter and set it to listen for WifiP2pManager status changes
	private final IntentFilter intentFilter = new IntentFilter();
	private WifiP2pManager mManager;
	private Channel mChannel;
	private WiFiDirectBroadcastReceiver mReceiver;
	private boolean isWifiP2pEnabled = false;
	private WifiP2pDevice device;
	private String MsgReceived;
	private int MsgSource;
	private int ClientSum;
	private int ClientNum;
	private boolean MsgWAPSent = true;
	
	//Thread and pipes for the client's data exchange
	private PipedOutputStream pout_transmit_client = null;
	private PipedInputStream pin_transmit_client = null;
	private PipedOutputStream pout_rcv_client = null;
	private PipedInputStream pin_rcv_client = null;
	private ClientThread clientthread;
	//Thread and pipes for the server's data exchange
	private PipedOutputStream pout_rcv_server = null;
	private PipedInputStream pin_rcv_server = null;
	private PipedOutputStream pout_transmit_server = null;
	private PipedInputStream pin_transmit_server = null;
	private ServerThread serverthread;
	
	private Timer timer = new Timer("Timer_Display",true);
	
	//For Listview
	public ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
	public SimpleAdapter listItemAdapter;
	
	//For message obtained from Internet
	public String MsgWAP = null;
	
	//private MsgObservable msgobservable = new MsgObservable();
	//private MsgObserver msgobserver = new MsgObserver(this,msgobservable);
	
	private Handler UIupdate = new Handler () {
	    public void handleMessage (Message msg) {
	        //Update the UI
	    	if(msg.what==1) {		//Show the message on UI
	    		display(MsgReceived);
	    	} else if(msg.what==2) {//Update check box availability on UI  	
	    		//Update check box availability
	    		if(!mReceiver.getWifiPeersInAdhoc().getIsServer()) {
	    			((CheckBox)findViewById(R.id.checkbox_go)).setEnabled(true);
	    			//Display the assigned client node ID
		    		((TextView)findViewById(R.id.head)).setText("Message History--CT"+Integer.toString(ClientNum));
	    		} else {
	    			//Display GO for group owner
		    		((TextView)findViewById(R.id.head)).setText("Message History--GO");
	    		}
	    		switch(ClientSum) {
	    			case 5: ((CheckBox)findViewById(R.id.checkbox_ct5)).setEnabled(true);
	    			case 4: ((CheckBox)findViewById(R.id.checkbox_ct4)).setEnabled(true);
	    			case 3: ((CheckBox)findViewById(R.id.checkbox_ct3)).setEnabled(true);
	    			case 2: ((CheckBox)findViewById(R.id.checkbox_ct2)).setEnabled(true);
	    			case 1: ((CheckBox)findViewById(R.id.checkbox_ct1)).setEnabled(true);
	    			default:;
	    		}
	    		switch(ClientNum) {
	    			case 1: ((CheckBox)findViewById(R.id.checkbox_ct1)).setEnabled(false); break;
	    			case 2: ((CheckBox)findViewById(R.id.checkbox_ct2)).setEnabled(false); break;
	    			case 3: ((CheckBox)findViewById(R.id.checkbox_ct3)).setEnabled(false); break;
	    			case 4: ((CheckBox)findViewById(R.id.checkbox_ct4)).setEnabled(false); break;
	    			case 5: ((CheckBox)findViewById(R.id.checkbox_ct5)).setEnabled(false); break;
	    			default:;
	    		}
	    	} else if(msg.what==3) {               
	    		//Try to connect to Internet
        		ConnectivityManager connMgr = (ConnectivityManager) 
        				getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    new DownloadWebpageText(MainActivity.this).execute(MsgReceived);
                    //Set this label to enable MsgWAP sending
                    MsgWAPSent = false; 
                }
	    	} else {    
	    		//Send the obtained Internet message to the requesting node
	    		if(!MsgWAPSent) {
		    		if(MsgWAP!=null) {
		    			byte buf[]  = new byte[1024];
			    		int len = 0;
		    	        try{
		    	        	buf = MsgWAP.getBytes("UTF-8");
		    	        	len = MsgWAP.length();
		    	        	if(mReceiver.getWifiPeersInAdhoc().getIsServer()) {
		    	        		pout_transmit_server.write(len);		//Message Head (message length,head and type not included)
		    	        		pout_transmit_server.write(0);			//Message Type (0 for data,1 for protocol)
		    	        		pout_transmit_server.write(0x01);		//Message Source (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
		    	        		pout_transmit_server.write((int)(java.lang.Math.pow(2,MsgSource)));	//Message Destination (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
		    		        	pout_transmit_server.write(buf,0,len);	//Message Body (message content)
		    	        	} else {
		    		        	pout_transmit_client.write(len);		//Message Head (message length,head and type not included)
		    		        	pout_transmit_client.write(0);			//Message Type (0 for data,1 for protocol)
		    		        	pout_transmit_client.write((int)(java.lang.Math.pow(2,ClientNum)));	//Message Source (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
		    		        	pout_transmit_client.write((int)(java.lang.Math.pow(2,MsgSource)));	//Message Destination (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
		    		        	pout_transmit_client.write(buf,0,len);	//Message Body (message content)
		    	        	}
		    	        } catch(IOException e) {
		    	        	//Catch logic
		    	        }
		    	        MsgWAPSent = true;
		    		}
	    		}
	    	}
	    }
	};
	
	public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //Indicates a change in the Wi-Fi Peer-to-Peer status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        //Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        //Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        //Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        
        /*get an instance of the WifiP2pManager, and call its initialize() method. 
        This method returns a WifiP2pManager.Channel object, which you'll use later 
        to connect your application to the Wi-Fi Direct Framework.*/
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
    }
    
    //Register the BroadcastReceiver with the intent values to be matched
    @Override
    public void onResume() {
        super.onResume();
        
        //Initialize the adapter for the listview
        listItemAdapter = new SimpleAdapter(this,
            	listItem,R.layout.list_view,
            	new String[] {"ItemNumber", "ItemMessage"},
            	new int[] {R.id.ItemNumber,R.id.ItemMessage});
        
        //Initialize and register the BroadcastReceiver
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(mReceiver, intentFilter);
        
        //Peers discovery
        discover();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        
        //Kill the timer
        timer.cancel();
        
        //Close the opened threads
        if(clientthread!=null) {
        	clientthread.interrupt();
        	clientthread = null;
        }
        if(serverthread!=null) {
        	serverthread.interrupt();
        	serverthread = null;
        }
        
        //Close the opened streams
        if(pout_transmit_client!=null) {
        	try {
				pout_transmit_client.close();
			} catch (IOException e) {
				//Catch Logic
			}
        }
        if(pin_transmit_client!=null) {
        	try {
				pin_transmit_client.close();
			} catch (IOException e) {
				//Catch Logic
			}
        }
        if(pout_rcv_client!=null) {
        	try {
				pout_rcv_client.close();
			} catch (IOException e) {
				//Catch Logic
			}
        }
        if(pin_rcv_client!=null) {
        	try {
				pin_rcv_client.close();
			} catch (IOException e) {
				//Catch Logic
			}
        }
        if(pout_transmit_server!=null) {
        	try {
				pout_transmit_server.close();
			} catch (IOException e) {
				//Catch Logic
			}
        }
        if(pin_transmit_server!=null) {
        	try {
				pin_transmit_server.close();
			} catch (IOException e) {
				//Catch Logic
			}
        }
        if(pout_rcv_server!=null) {
        	try {
				pout_rcv_server.close();
			} catch (IOException e) {
				//Catch Logic
			}
        }
        if(pin_rcv_server!=null) {
        	try {
				pin_rcv_server.close();
			} catch (IOException e) {
				//Catch Logic
			}
        }
        
        //Cancel all on-going P2P connections
        mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(), "Connection cancelled successfully!",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(getApplicationContext(), "Connection Cancellation Failed! Reason Code:" + reasonCode,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    //Get the WiFiDirectBroadcastReceiver
    public WiFiDirectBroadcastReceiver getWiFiDirectBroadcastReceiver() {
    	return mReceiver;
    }

    //Discover peers
    public void discover() {
    	
    	mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(), "Discovery Initiated!",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(getApplicationContext(), "Discovery Failed! Reason Code:" + reasonCode,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    //Get a peer and connect
    public void connect(WifiP2pDevice device) {

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.groupOwnerIntent = 0;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            	//Set IsConnected
        		mReceiver.getWifiPeersInAdhoc().setIsConnected(true);
        		//notify();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(), "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
                //notify();
            }
        });
    }
    
    //Establish pipes between UI thread and Data Transmit thread, start the later thread
    public void ClientThreadStart(String host) {
    	
    	try {
	    	pout_transmit_client = new PipedOutputStream();
	    	pin_transmit_client = new PipedInputStream(pout_transmit_client);
	    	
	    	pout_rcv_client = new PipedOutputStream();
	    	pin_rcv_client = new PipedInputStream(pout_rcv_client);

	    	clientthread = new ClientThread(host,pout_rcv_client,pin_transmit_client);
	    	clientthread.setDaemon(true);
	    	clientthread.start();
    	} catch(IOException e) {
    		//Catch Logic
    	}
    	
    	//Schedule the timer
    	timer.schedule(new TimerTask() {
    		@Override
    		public void run() {
    			int tmp = MsgRcv();
    			//If a new message received, notify the handler
    			Message m = new Message();
    			m.what = tmp;
    			UIupdate.sendMessage(m);
    		}
    	},1000,500);
    }
    
    //Establish pipes between UI thread and Data Rcv thread, start the later thread
    public void ServerThreadStart() {
    	
    	try {
	    	pout_rcv_server = new PipedOutputStream();
	    	pin_rcv_server = new PipedInputStream(pout_rcv_server);
	    	
	    	pout_transmit_server = new PipedOutputStream();
	    	pin_transmit_server = new PipedInputStream(pout_transmit_server);
	    	
	    	serverthread = new ServerThread(pout_rcv_server,pin_transmit_server);
	    	serverthread.setDaemon(true);
	    	serverthread.start();
    	} catch(IOException e) {
    		//Catch Logic
    	}
    	
    	//Schedule the timer
    	timer.schedule(new TimerTask() {
    		@Override
    		public void run() {
    			int tmp = MsgRcv();
    			//If a new message received, notify the handler
				Message m = new Message();
    			m.what = tmp;
    			UIupdate.sendMessage(m);
    		}
    	},1000,500);
    }
    
    //Called when the user clicks the Send button
    public void sendMessage(View view) { 
    		
    	//Peers not available, please wait!
    	/*if(!mReceiver.getIsPeersAvailable()){
    		Toast.makeText(getApplicationContext(), "Peers not available, please wait!",
                    Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	//Connection not available, please wait!
    	if(!mReceiver.getIsConnectionAvailable()){
    		Toast.makeText(getApplicationContext(), "Connection not available, please wait!",
    				Toast.LENGTH_SHORT).show();
    		return;
    	}*/
    	
        //Get the content in the text box
    	int dst_addr = 0;
    	byte buf[]  = new byte[1024];
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        int len = message.length();
        if(len<=0) {
        	Toast.makeText(getApplicationContext(), "Empty message!",
                    Toast.LENGTH_SHORT).show();
        	return;
        }

        //If it is a url, connect to the corresponding website
        if(message.startsWith("http")||message.startsWith("HTTP")||message.startsWith("www")||message.startsWith("WWW")) {
        	if(message.startsWith("www")||message.startsWith("WWW")) {
        		message = "http://" + message;
        	}
        	message = message.toLowerCase();
            ConnectivityManager connMgr = (ConnectivityManager) 
                getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                new DownloadWebpageText(this).execute(message);
                return;
            }
            
            //If Internet connection of current node is unavailable, try to ask other nodes for help 
            for(int i=0;i<=ClientSum;i++) {
            	if(i==ClientNum) {	//Current node has no Internet connection
            		continue;
            	}
            	dst_addr += (int)(java.lang.Math.pow(2,i));
            }	
            
        	//Send message to the node i to ask for help
        	try{
             	buf = message.getBytes("UTF-8");
             	len = message.length();
             	if(mReceiver.getWifiPeersInAdhoc().getIsServer()) {
             		pout_transmit_server.write(len);		//Message Head (message length,head and type not included)
             		pout_transmit_server.write(2);			//Message Type (0 for data,1 for protocol)
             		pout_transmit_server.write(0x01);		//Message Source (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
             		pout_transmit_server.write(dst_addr);	//Message Destination (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
     	        	pout_transmit_server.write(buf,0,len);	//Message Body (message content)
             	} else {
     	        	pout_transmit_client.write(len);		//Message Head (message length,head and type not included)
     	        	pout_transmit_client.write(2);			//Message Type (0 for data,1 for protocol)
     	        	pout_transmit_client.write((int)(java.lang.Math.pow(2,ClientNum)));	//Message Source (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
     	        	pout_transmit_client.write(dst_addr);	//Message Destination (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
     	        	pout_transmit_client.write(buf,0,len);	//Message Body (message content)
             	}
             } catch(IOException e) {
             	//Catch logic
             }

        	return;
        }
        
        //Get the destination address from the check boxes
        dst_addr = 0;
        if(((CheckBox)findViewById(R.id.checkbox_go)).isChecked()) {
        	dst_addr += 1;
        }
        if(((CheckBox)findViewById(R.id.checkbox_ct1)).isChecked()) {
        	dst_addr += 2;
        }
        if(((CheckBox)findViewById(R.id.checkbox_ct2)).isChecked()) {
        	dst_addr += 4;
        }
        if(((CheckBox)findViewById(R.id.checkbox_ct3)).isChecked()) {
        	dst_addr += 8;
        }
        if(((CheckBox)findViewById(R.id.checkbox_ct4)).isChecked()) {
        	dst_addr += 16;
        }
        if(((CheckBox)findViewById(R.id.checkbox_ct5)).isChecked()) {
        	dst_addr += 32;
        }
        if(dst_addr<=0) {
        	Toast.makeText(getApplicationContext(), "No Receiver Selected!",
                    Toast.LENGTH_SHORT).show();
        	return;
        }
        
        //Send the message
        try{
        	buf = message.getBytes("UTF-8");
        	if(mReceiver.getWifiPeersInAdhoc().getIsServer()) {
        		pout_transmit_server.write(len);		//Message Head (message length,head and type not included)
        		pout_transmit_server.write(0);			//Message Type (0 for data,1 for protocol)
        		pout_transmit_server.write(0x01);		//Message Source (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
        		pout_transmit_server.write(dst_addr);	//Message Destination (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
	        	pout_transmit_server.write(buf,0,len);	//Message Body (message content)
        	} else {
	        	pout_transmit_client.write(len);		//Message Head (message length,head and type not included)
	        	pout_transmit_client.write(0);			//Message Type (0 for data,1 for protocol)
	        	pout_transmit_client.write((int)(java.lang.Math.pow(2,ClientNum)));	//Message Source (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
	        	pout_transmit_client.write(dst_addr);	//Message Destination (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
	        	pout_transmit_client.write(buf,0,len);	//Message Body (message content)
        	}
        } catch(IOException e) {
        	//Catch logic
        }
        
        //Clear the text box
        editText.setText("");
        
        //Accumulate the count of transmitted messages
        mReceiver.getWifiPeersInAdhoc().setTransmitMsgCnt(mReceiver.getWifiPeersInAdhoc().getTransmitMsgCnt()+1);
        
        //Get the handle of the list view
        ListView list = (ListView) findViewById(R.id.list_view);
        
        //Get the current time
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        
        //Fill the data into the Hash map
        HashMap<String, Object> map = new HashMap<String, Object>();  
        if(mReceiver.getWifiPeersInAdhoc().getIsServer()) {
        	map.put("ItemNumber", "GO " + dateFormat.format(cal.getTime()));
        } else {
        	map.put("ItemNumber", "CT" + Integer.toString(ClientNum) + " " + dateFormat.format(cal.getTime()));  
        }
        map.put("ItemMessage", message);  
        listItem.add(map);  
        
        //Display the items
        list.setAdapter(listItemAdapter);
        
        //Scroll to the bottom
        list.setSelection(list.getBottom());
    }
    
    //Receive a new message(the returned value 0 for failure, 1 for data message, 2 for protocol message and 3 for Internet message)
    public int MsgRcv() {
    	
    	//Get the received message
        byte buf[]  = new byte[1024];
        String message = null;
        int msg_len;
        int msg_type;
        int msg_src;
        int msg_dst;
        int len;
        PipedInputStream pin_rcv = null;
        
        try{
        	if(mReceiver.getWifiPeersInAdhoc().getIsServer()) {
        		pin_rcv = pin_rcv_server;
        	} else {
        		pin_rcv = pin_rcv_client;
        	}
        	
        	if(pin_rcv.available()<=0) {
        		return 0;
        	}
        	msg_len = pin_rcv.read();	//Get the Message Head (message length,head and type not included)
        	msg_type = pin_rcv.read();	//Get the Message Type (0 for data,1 for protocol)
        	msg_src = pin_rcv.read();	//Get the Message Source (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
        	msg_dst = pin_rcv.read();	//Get the Message Destination (Binary,a "1" in the ith(0~7) bit stands for the ith client, 0x01 for server)
        	len = 0;
        	while(len<msg_len) {
        		len += pin_rcv.read(buf,len,msg_len-len);	//Get the Message Body (message content)
        	}
        	if(msg_type==0) {			//Judge the message type; store it into message if it is a data frame
        		message = new String(buf,0,msg_len,"UTF-8");
        	} else if(msg_type==1) {	//Set the availability of check boxes
        		ClientSum = buf[0];
        		ClientNum = buf[1];
        		return 2;
        	} else {	//Internet Connection Request
        		//Obtain the url
        		message = new String(buf,0,msg_len,"UTF-8");
        		MsgReceived = message;
        		
                //Remember who is requesting Internet access
                MsgSource = (int)java.lang.Math.round((java.lang.Math.log(msg_src)/java.lang.Math.log(2)));

                return 3;
        	}
        } catch(IOException e) {
        	//Catch logic
        	return 0;
        }
        
        //Update the received message and source node number
        MsgReceived = message;
        MsgSource = (int)java.lang.Math.round((java.lang.Math.log(msg_src)/java.lang.Math.log(2)));
        
        //Accumulate the count of received messages
        mReceiver.getWifiPeersInAdhoc().setRcvMsgCnt(mReceiver.getWifiPeersInAdhoc().getRcvMsgCnt()+1);
        
        return 1;
    }
    
    public void display(String message) {
    	
        //Get the handle of the list view
        ListView list = (ListView) findViewById(R.id.list_view);
        
        //Get the current time
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        
        //Fill the data into the Hash map 
        HashMap<String, Object> map = new HashMap<String, Object>();  
        if(MsgSource==0) {
        	map.put("ItemNumber", "GO " + dateFormat.format(cal.getTime()));
        } else {
        	map.put("ItemNumber", "CT" + Integer.toString(MsgSource) + " " + dateFormat.format(cal.getTime()));  
        } 
        map.put("ItemMessage", message);  
        listItem.add(map);  
        
        //Display the items
        list.setAdapter(listItemAdapter);
        
        //Scroll to the bottom
        list.setSelection(list.getBottom());
    }
}
