package com.example.infections;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.TextPaint;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
//import android.widget.AdapterView.AdapterContextMenuInfo;

public class MainActivity extends Activity {
	
	private GoogleMap mMap;
	private MapView mmMap;
	String KEY_ITEM = "country"; // parent node
	String KEY_ID = "id";
	String KEY_NAME = "name";
	String KEY_INFECTIONS = "infections";
	String KEY_DEATHS = "deaths";

	static private String SERVER_URL = "http://pastebin.com/raw.php?i=rjus7qj4";
	ArrayList<HashMap<String, String>> mapItems;
	LocationManager mlocManager;
	MapController mapController;
	static double LIMIT = 5.0;
	Marker marker_search;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setTitle("Infections in Countries");
		mapItems =  new ArrayList<HashMap<String, String>>();
			
		getXMLFile(SERVER_URL, this);
		setUpMap();
		
		mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		//mapController = mmMap.getController();
		 
		EditText t = (EditText)findViewById(R.id.txtFind);
		t.setSingleLine();
		t.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_DONE) {
		            SearchLocation();
		            return true;	
		        }
		        return false;
		    }
		});
		

	}

	
	private double distance(double lat1, double lon1, double lat2, double lon2, char unit) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
	  	if (unit == "K".charAt(0)) {
	  		dist = dist * 1.609344;
	  	} else if (unit == "N".charAt(0)) {
	  		dist = dist * 0.8684;
	    }
	  	return (dist);
	}

	private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
    	return (rad * 180.0 / Math.PI);
    }
	    
	public void SearchLocation()
	{
		if (marker_search != null)
			marker_search.remove();
		
		EditText t = (EditText)findViewById(R.id.txtFind);
		if (!t.getText().toString().equals(""))
		{
			LatLng pos = getLatLng(t.getText().toString());
			//mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos,4));
			
			if (pos.latitude == 0 && pos.longitude == 0)
			{
				Toast.makeText(getApplicationContext(), "Location not found.", Toast.LENGTH_LONG).show();
				return;
			}
			
	    	marker_search = mMap.addMarker(new MarkerOptions().position(pos).title(t.getText().toString()));
			
	    	for (int i=0; i<mapItems.size(); i++){
	    		
	    		HashMap<String, String> m = (HashMap<String, String>)mapItems.get(i);
	    		if (distance(Double.parseDouble(m.get("KEY_POSITION_LAT")),
						Double.parseDouble(m.get("KEY_POSITION_LNG")), 
						marker_search.getPosition().latitude, 
						marker_search.getPosition().longitude, "K".charAt(0)) <= LIMIT)
	    		{
	    			mMap.addCircle(new CircleOptions()
	    			.strokeColor(Color.GRAY)
	    			.strokeWidth(3)
	    			.fillColor(0x40ff0000)
	    			.center(marker_search.getPosition())
	    			.radius(LIMIT*1000));
	    			break;
	    		} 
	         
	    	
					mMap.addCircle(new CircleOptions()
					.strokeColor(Color.GRAY)
					.strokeWidth(3)
					.fillColor(0x4000ff00)
					.center(marker_search.getPosition())
					.radius(LIMIT*1000));
    		}
			
	    	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker_search.getPosition(), 7));
	    	
	    	
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    //AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()) {
	    case R.id.action_mylocation:
	    	getMyLocation();
	    	break;
		}
	    return true;
	}
	public LatLng getLatLng(String n){

		Geocoder coder = new Geocoder(this);
		List<Address> address;
		try {

			address = coder.getFromLocationName(n,5);
			if (address == null) {
				return new LatLng(0, 0);
			}
			Address location = address.get(0);
			return new LatLng(location.getLatitude(), location.getLongitude() );

		} catch(Exception e) {
			return new LatLng(0, 0);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void setUpMap() {
		
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.MapView)).getMap();
		//mmMap = (MapView)findViewById(R.id.MapView);
	    if (mMap != null) {


        } else {
        	
        	Toast.makeText(getApplicationContext(), 
        			"Unable to setup a map, Please check your connection or contact the developer",
        			Toast.LENGTH_LONG).show();
        	finish();
        	
	    }
	}
	
	private void getXMLFile(final String url, final Context context)
	{
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            private ProgressDialog pd;
            
            @Override
            protected void onPreExecute() {
                     pd = new ProgressDialog(context);
                     pd.setTitle("Retreiving Data");
                     pd.setMessage("Please Wait While Collecting Data");
                     pd.setCancelable(false);
                     pd.setIndeterminate(true);
                     pd.show();
            }
            
            @Override
            protected Void doInBackground(Void... arg0) {
            	

            	 
            	
            	String xml = getXmlFromUrl(url); // getting XML
            	
            	if (xml != null)
            	{
	            	Document doc = getDomElement(xml); // getting DOM element
	            	
	            	if (doc != null)
	            	{
		            	NodeList nl = doc.getElementsByTagName(KEY_ITEM);
		            	 
		            	// looping through all item nodes <item>      
		            	for (int i = 0; i < nl.getLength(); i++) {
		            		Element e = (Element) nl.item(i);
		            		
		            		LatLng pos = getLatLng(getValue(e, KEY_NAME));
		            		
		            		if (pos.latitude != 0 && pos.longitude !=0)
		            		{
		            		
		            			/*mMap.addMarker(new MarkerOptions()
	        		        					.position(pos)
        		        						.title(getValue(e, KEY_NAME))
    		        							.snippet("No. of infections = " + getValue(e, KEY_INFECTIONS) +
    		        									"\nNo. of deaths = " + getValue(e, KEY_DEATHS))
    		        							.icon(BitmapDescriptorFactory
    		        									.fromResource(R.drawable.pin)));*/
		                    HashMap<String, String> map = new HashMap<String, String>();
		                    
		                    // adding each child node to HashMap key => value
		                    map.put(KEY_ID, getValue(e, KEY_ID));
		                    map.put(KEY_NAME, getValue(e, KEY_NAME));
		                    map.put(KEY_INFECTIONS, getValue(e, KEY_INFECTIONS));
		                    map.put(KEY_DEATHS, getValue(e, KEY_DEATHS));
		                    map.put("KEY_POSITION_LAT", Double.toString(pos.latitude));
		                    map.put("KEY_POSITION_LNG", Double.toString(pos.longitude));
		                    
		                    mapItems.add(map);
		            		}
		            	}
	            	}
            	}
            	
            	/*int count;
            	try {
            		
	            	URL urlcon = new URL(url );        
	                HttpURLConnection connection = (HttpURLConnection) urlcon.openConnection();
	                
	                connection.setConnectTimeout(5000);
	                connection.setInstanceFollowRedirects(true);
	                connection.connect();
	
	                int length = connection.getContentLength();
	
	                InputStream input = new BufferedInputStream(urlcon.openStream(), 8192);
	
	                String filename = "infections_data.xml";
	                OutputStream output = new FileOutputStream("/sdcard/Download/" + filename);
	
	                byte data[] = new byte[1024];
	                long total = 0;
	
	                while ((count = input.read(data)) != -1) {    	
	                    total += count;
	                    output.write(data, 0, count);
	                }
	
	                output.flush();
	                output.close();
	                input.close();
                
                    Message msg = handler.obtainMessage();
                    msg.arg1 = 3;
                    handler.sendMessage(msg); 
                    
            	} catch (SocketTimeoutException e2) {
            		
                    Message msg = handler.obtainMessage();
                    msg.arg1 = 1;
                    handler.sendMessage(msg); 
                    finish();
                    
            	} catch (IOException e1) {

                	Message msg = handler.obtainMessage();
                    msg.arg1 = 2;
                    handler.sendMessage(msg);
                    finish();
                    
            	} catch (Exception e) {

                    Message msg = handler.obtainMessage();
                    msg.arg1 = 1;
                    handler.sendMessage(msg);
                    finish();
            	}*/
            	
				return null;
            }
       
            public String getXmlFromUrl(String url) {
                String xml = null;
         
                try {
                    // defaultHttpClient
                    DefaultHttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(url);
         
                    HttpResponse httpResponse = httpClient.execute(httpPost);
                    HttpEntity httpEntity = httpResponse.getEntity();
                    xml = EntityUtils.toString(httpEntity);
         
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // return XML
                return xml;
            }
            
            public Document getDomElement(String xml){
                Document doc = null;
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                try {
         
                    DocumentBuilder db = dbf.newDocumentBuilder();
         
                    InputSource is = new InputSource();
                        is.setCharacterStream(new StringReader(xml));
                        doc = db.parse(is); 
         
                    } catch (ParserConfigurationException e) {
                        Log.e("Error: ", e.getMessage());
                        return null;
                    } catch (SAXException e) {
                        Log.e("Error: ", e.getMessage());
                        return null;
                    } catch (IOException e) {
                        Log.e("Error: ", e.getMessage());
                        return null;
                    }
                        // return DOM
                    return doc;
            }
            
            public String getValue(Element item, String str) {      
                NodeList n = item.getElementsByTagName(str);        
                return getElementValue(n.item(0));
            }
             
            public final String getElementValue( Node elem ) {
                     Node child;
                     if( elem != null){
                         if (elem.hasChildNodes()){
                             for( child = elem.getFirstChild(); child != null; child = child.getNextSibling() ){
                                 if( child.getNodeType() == Node.TEXT_NODE  ){
                                     return child.getNodeValue();
                                 }
                             }
                         }
                     }
                     return "";
              } 
            
            @Override
            protected void onPostExecute(Void result) {
            	updateMapWithPins();
            	pd.dismiss();
            	
            }
             
    };
    task.execute((Void[])null);
	}
	
	private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
              if(msg.arg1 == 1)
                    Toast.makeText(getApplicationContext(),
                    		"Unable to connect to download host, Please check your connection settings.\nApplication terminated.", 
                    		Toast.LENGTH_LONG).show();
              else if (msg.arg1 == 2)
            	  Toast.makeText(getApplicationContext(), 
            			  "Failed downloading XML file.\nApplication terminated.", 
            			  Toast.LENGTH_LONG).show();
              else if (msg.arg1 == 3)
            	  Toast.makeText(getApplicationContext(), 
            			  "XML Loaded.", 
            			  Toast.LENGTH_LONG).show();             
              
              

        }
    };
    
    public void updateMapWithPins()
    {

    	for (int i=0; i<mapItems.size(); i++){
    		HashMap<String, String> m = (HashMap<String, String>)mapItems.get(i);
			mMap.addMarker(new MarkerOptions()
			.position(getLatLng(m.get(KEY_NAME)))
			.title(m.get(KEY_NAME))
			.snippet("No. of infections = " + m.get(KEY_INFECTIONS) +
					" , No. of deaths = " + m.get(KEY_DEATHS)));

         }
    }
    
    private void getMyLocation()
    {
    	//mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10, null);
    	Location location = mlocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		
    	Marker mark = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title("My Location"));
		
    	for (int i=0; i<mapItems.size(); i++){
    		
    		HashMap<String, String> m = (HashMap<String, String>)mapItems.get(i);
    		if (distance(Double.parseDouble(m.get("KEY_POSITION_LAT")),
					Double.parseDouble(m.get("KEY_POSITION_LNG")), 
					mark.getPosition().latitude, 
					mark.getPosition().longitude, "K".charAt(0)) <= LIMIT)
    		{
    			mMap.addCircle(new CircleOptions()
    			.strokeColor(Color.GRAY)
    			.strokeWidth(3)
    			.fillColor(0x40ff0000)
    			.center(mark.getPosition())
    			.radius(LIMIT*1000));
    			break;
    			
    		}
    			mMap.addCircle(new CircleOptions()
    			.strokeColor(Color.GRAY)
    			.strokeWidth(3)
    			.fillColor(0x4000ff00)
    			.center(mark.getPosition())
    			.radius(LIMIT*1000));
	

     	}
    	
    	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mark.getPosition(), 15));
    }
}
