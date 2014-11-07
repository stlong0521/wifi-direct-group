package edu.msu.wifiadhoc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import android.os.AsyncTask;
import android.widget.ListView;

public class DownloadWebpageText extends AsyncTask <String,Void,String> {
	
	private MainActivity activity;
	
	public DownloadWebpageText(MainActivity activity) {
		this.activity = activity;
	}
	
    @Override
    protected String doInBackground(String... url) {
          
        // params comes from the execute() call: params[0] is the url.
        try {
            return downloadUrl(url[0]);
        } catch (IOException e) {
            return "Unreachable website. URL may be invalid.";
        }
    }
    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(String result) {
        
    	//Set the public message obtained from Internet
    	activity.MsgWAP = result;
    	
        //Get the handle of the list view
        ListView list = (ListView)activity.findViewById(R.id.list_view);
        
        //Get the current time
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        
        //Fill the data into the Hash map
        HashMap<String, Object> map = new HashMap<String, Object>();  
        map.put("ItemNumber", "WAP " + dateFormat.format(cal.getTime()));
        map.put("ItemMessage", result);  
        activity.listItem.add(map);  
        
        //Display the items
        list.setAdapter(activity.listItemAdapter);
        
        //Scroll to the bottom
        list.setSelection(list.getBottom());
    }
    
    private String downloadUrl(String myurl) throws IOException {
    	
        InputStream is = null;
        int len = 30;	//Only display the first 20 characters of the retrieved web page content.
        URL url = new URL(myurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();   
        
        try {
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is, len);
            return contentAsString;
        } finally {	// Makes sure that the InputStream is closed after the app is finished using it.
            if (is != null) {
                is.close();
            } 
            if(conn!=null) {
            	conn.disconnect();
            }
        }
    }
    
    //Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");        
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }
}
