package edu.msu.wifiadhoc;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.widget.Toast;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

	private WifiP2pManager manager;
    private Channel channel;
    private MainActivity activity;
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private boolean IsPeersAvailable = false;
    private boolean IsConnectionAvailable = false;
    private WifiPeersInAdhoc wifipeersinadhoc = new WifiPeersInAdhoc();
    private PeerListListener myPeerListListener = new PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            //Out with the old, in with the new.
            peers.clear();
            peers.addAll(peerList.getDeviceList());
            
            //Set IsPeersAvailable
            IsPeersAvailable = true;
            
            //Add the devices to WifiPeersInAdhoc and find the index of group owner
            for(int i=0;i<peers.size();i++) {
	            wifipeersinadhoc.addWifiP2pDevice(peers.get(i));
	            if(peers.get(i).isGroupOwner()) {
	            	wifipeersinadhoc.setGroupOwnerIndex(i);
	            }
            }
            
            //Decide which peer to connect
            int index;
            //No group has been formed yet, get a peer and connect
            if(wifipeersinadhoc.getGroupOwnerIndex()==-2) {	
            	index = 0;
            }
            //A group exists, find the group owner and connect
            else {
	            index = wifipeersinadhoc.getGroupOwnerIndex();
            }
            
            //Connect to the designated peer
            if(!wifipeersinadhoc.getIsConnected()) {
            	activity.connect(wifipeersinadhoc.getWifiP2pDevice(index));
            } 
        }
    };
    private ConnectionInfoListener myConnectionListener = new ConnectionInfoListener() {
    	@Override
    	public void onConnectionInfoAvailable(final WifiP2pInfo info) {

    		//Set IsConnectionAvailable
    		IsConnectionAvailable = true;
    		
            // InetAddress from WifiP2pInfo struct.
    		String  groupOwnerAddress = info.groupOwnerAddress.getHostAddress();

            // After the group negotiation, we can determine the group owner.
            if (info.groupFormed && info.isGroupOwner) {
                // Do whatever tasks are specific to the group owner.
                // One common case is creating a server thread and accepting
                // incoming connections.
            	//Start the data receiving in background thread
            	wifipeersinadhoc.setIsServer(true);
            	activity.ServerThreadStart();
            } else if (info.groupFormed) {
                // The other device acts as the client. In this case,
                // you'll want to create a client thread that connects to the group
                // owner.
            	wifipeersinadhoc.setIsServer(false);
            	activity.ClientThreadStart(groupOwnerAddress);
            }
        }
    };

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
    		MainActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }
    
    public boolean getIsPeersAvailable() {
    	return IsPeersAvailable;
    }
    
    public boolean getIsConnectionAvailable() {
    	return IsConnectionAvailable;
    }
    
    public WifiP2pDevice getList(int index) {
    	return peers.get(index);
    }
    
    public WifiPeersInAdhoc getWifiPeersInAdhoc() {
    	
    	return wifipeersinadhoc;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    // Wifi Direct is enabled
                	activity.setIsWifiP2pEnabled(true);
                } else {
                    // Wi-Fi Direct is not enabled
                	activity.setIsWifiP2pEnabled(false);
                    //activity.resetData();
                }
            }
            //Log.d(WiFiDirectActivity.TAG, "P2P state changed - " + state);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
        	// request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
            	manager.requestPeers(channel, myPeerListListener);
            }
            //Log.d(WiFiDirectActivity.TAG, "P2P peers changed");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
        	if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // We are connected with the other device, request connection
                // info to find group owner IP
            	Toast.makeText(activity.getApplicationContext(), "Connect succeed.",
                        Toast.LENGTH_SHORT).show();
            	
                manager.requestConnectionInfo(channel, myConnectionListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        	//DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
            //        .findFragmentById(R.id.frag_list);
            //fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
            //        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        }
    }

}
