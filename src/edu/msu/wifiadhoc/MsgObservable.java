package edu.msu.wifiadhoc;

import java.util.Observable;

public class MsgObservable extends Observable {
	
	private String Msg = null;
	 
	public String getMsg(){
		return Msg;
	}
	 
	public void setMsg(String msg){
	    if(this.Msg != msg){ 
	    	this.Msg = msg; 
	    	setChanged();
	    }
	    notifyObservers(); 
	}
}
