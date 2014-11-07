package edu.msu.wifiadhoc;

import java.util.Observable;
import java.util.Observer;

import android.os.Message;

public class MsgObserver implements Observer{
	
	private MainActivity activity;
	private String message = null;
	
	public MsgObserver(MainActivity activity,MsgObservable msgobservable){
		this.activity = activity;
		msgobservable.addObserver(this);
	}
	
	public String getMsg() {
		return message;
	}
 
	@Override
	public void update(Observable o,Object arg){
		
		//Display the received message
		/*message = ((MsgObservable) o).getMsg();
		Message m = new Message();
		m.what = 0;
		activity.getUIupdate().sendMessage(m);*/
	}

}
