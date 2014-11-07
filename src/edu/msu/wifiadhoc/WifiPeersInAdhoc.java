package edu.msu.wifiadhoc;

import java.util.ArrayList;
import java.util.List;

import android.net.wifi.p2p.WifiP2pDevice;

public class WifiPeersInAdhoc {
	
	//Information about the node itself
	private boolean IsServer = false;			//Whether the node itself is server or not
	private int TransmitMsgCnt = 0;				//Count of transmitted messages
	private int RcvMsgCnt = 0;					//Count of received messages
	private boolean IsConnected = false;		//Success flag of the last WiFi connection
	private int GroupOwnerIndex = -2;			//Index(>=0) of group owner in the WifiP2pDevice array list
												//-1 represents the node itself is group owner
												//-2 represents no group has been formed

	//Information about peers
	private List<WifiP2pDevice> wifip2pdevice = new ArrayList<WifiP2pDevice>();
	//private boolean IsLastTransSuccess = false;	//Success flag of the last socket connection and data transfer
	
	//Public constructor
	public WifiPeersInAdhoc() {
		
	}

	/*---------------------------Methods of node operation itself-----------------------*/
	//Set the IsServer
	public void setIsServer(boolean isserver) {
		this.IsServer = isserver;
	}
	//Get the IsServer
	public boolean getIsServer() {
		return IsServer;
	}
	
	//Set the TransmitMsgCnt
	public void setTransmitMsgCnt(int msgcnt) {
		this.TransmitMsgCnt = msgcnt;
	}
	//Get the TransmitMsgCnt
	public int getTransmitMsgCnt() {
		return TransmitMsgCnt;
	}
	//Set the RcvMsgCnt
	public void setRcvMsgCnt(int msgcnt) {
		this.RcvMsgCnt = msgcnt;
	}
	//Get the RcvMsgCnt
	public int getRcvMsgCnt() {
		return RcvMsgCnt;
	}
	
	//Set the IsConnected
	public void setIsConnected(boolean isconnected) {
		this.IsConnected = isconnected;
	}
	//Get the IsConnected
	public boolean getIsConnected() {
		return IsConnected;
	}
	
	//Set the GroupOwnerIndex
	public void setGroupOwnerIndex(int groupownerindex) {
		this.GroupOwnerIndex = groupownerindex;
	}
	//Get the GroupOwnerIndex
	public int getGroupOwnerIndex() {
		return GroupOwnerIndex;
	}
	
	/*------------------------------Methods of peer operation--------------------------*/
	//Set the WifiP2pDevice
	public void addWifiP2pDevice(WifiP2pDevice wifip2pdevice) {
		this.wifip2pdevice.add(wifip2pdevice);
	}
	//Get the WifiP2pDevice
	public WifiP2pDevice getWifiP2pDevice(int index) {
		return wifip2pdevice.get(index);
	}
	
	//Set the IsLastTransSuccess
	/*public void setIsLastTransSuccess(boolean isLastTransSuccess) {
		
		this.IsLastTransSuccess = isLastTransSuccess;
	}
	//Get the IsLastTransSuccess
	public boolean getIsLastTransSuccess() {
		
		return IsLastTransSuccess;
	}*/
}
