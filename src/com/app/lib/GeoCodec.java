package com.app.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

public class GeoCodec {

	private Geocoder gc;
	
	private Context context;
	
	public GeoCodec(Context cntx) {
		// TODO Auto-generated constructor stub
	
		this.context = cntx;
		gc = new Geocoder(context, Locale.US);	
	
	}
	
	//multiply passed in lat / lng by 1E6
	// Returns the list of addresses nearest the given GPS coordinate
	public List<String> getAddresses(GpsPoint point, int num_addrs){

		// Array of nearest addresses for the specified GPS coordinate 
		List<String> foundAddresses = new ArrayList<String>(); // String[num_addrs];
    	
        try {
        	List<Address> addresses = gc.getFromLocation(                    
        			point.getLat(), point.getLon(), num_addrs);
        	
           	// for each address entry
           	for(int i=0; i < addresses.size(); i++){
            	
           		String addr_entry = "";
            		
            	// fetch all lines from current address entry
            	for (int j=0; j < addresses.get(0).getMaxAddressLineIndex();j++){
            		addr_entry += addresses.get(0).getAddressLine(j) + ", ";
            	}
            	// add address entry if not blank
            	if(addr_entry.length() > 0){
            		foundAddresses.add(addr_entry);
            	}
            		
            }
        	
        	//Toast.makeText(context, add, Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {                
            e.printStackTrace();
        } 

       return foundAddresses;
	}
    
	
	public List<GpsPoint> getGpsPoints(String addrInput, int num_points){
		
		List<GpsPoint> points = new ArrayList<GpsPoint> ();
		
        try {
            List<Address> foundAddresses = gc.getFromLocationName(addrInput, num_points); //Search addresses
    
            for (int i = 0; i < foundAddresses.size(); i++) {
      
              //Save results as Longitude and Latitude
               //@todo: if more than one result, then show a select-list
      
              Address x = foundAddresses.get(i);
      
              GpsPoint p = new GpsPoint(x.getLatitude(),x.getLongitude());
              
              points.add(p);

              
/*            Toast.makeText(context, "lat: " + points[i].getLat() + 
            		" lon: " + points[i].getLon(), 
            		Toast.LENGTH_SHORT).show();*/
 
            }

        } catch (IOException e) {
            e.printStackTrace();
            
        } 

 
        return points;
	}
	
	
	// returns first matching gps point for the specified address or null if invalid address
	public GpsPoint getGpsPointFromAddress(String addr){
		
		GpsPoint p = null;
		List<GpsPoint> points = getGpsPoints(addr, 1);
		
		if(!points.isEmpty()){
			p = points.get(0);
		}    		
		return p;
	}
  	// Returns first matching address for the specified gps point or null if invalid gps point
	public String getAddressFromGpsPoint(GpsPoint p){
		String address = "";
		List<String> addresses = getAddresses(p, 1);
		if(!addresses.isEmpty()){
			address = addresses.get(0);
		}
		return address;
	}

}

