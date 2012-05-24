package com.app.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.util.Log;

/**
 * This class takes a search string and converts it to a URL that can used
 * to query Google local via AJAX interface
 */
public class GLocalSearch {

	private String query;
	private String lat;
	private String lon;
	private String start; // start
	private String TAG = "GLocalSearch";
	// base url for the local search google AJAX service
	private static final String BASEURL = "http://ajax.googleapis.com/ajax/services/search/local?v=1.0";
	
	// default URL to be used when no results obtained via ajax query
	public static final String DEFAULT_URL = "http://local.google.com";
	
	public GLocalSearch(){
		lat = "0.0";
		lon = "0.0";
		query = null;
		start = "0";
	}
	
	public void setSearchParams(Location myLoc, String searchFor, String start_index) {
		// TODO Auto-generated constructor stub
		lat = Double.toString(myLoc.getLatitude());
		lon = Double.toString(myLoc.getLongitude());
		// google AJAX queries require spaces to represented as ascii %20 values 
		query = searchFor.replaceAll(" ", "%20");
		Log.e(TAG, "subs query: " + query);
		start = start_index;
		
		//return getJSONSearchResults();
	}

	private String getSearchUrl(){
		
		String searchUrl = BASEURL + 
			"&q=" + query + "&sll=" + lat + "," + lon +
			"&start=" + start +
			"&rsz=8" ; 
		
		Log.e(TAG, "searchUrl: " + searchUrl);
		
		return searchUrl; 
	}
	
	public JSONObject getJSONSearchResults(){

		URL url;
		JSONObject json = null;
		
		try {
			url = new URL(getSearchUrl());
			Log.e("GLocalSearch", "URL: " + url.toString());
			URLConnection connection = url.openConnection();
			String line;
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(connection.getInputStream()));
			while((line = reader.readLine()) != null) {
			 builder.append(line);
			}
			Log.e("GLocalSearch", builder.toString());
			json = new JSONObject(builder.toString());

		}catch(MalformedURLException e){ 
			e.printStackTrace(); 
		}catch(IOException e){ 
			e.printStackTrace();
		}catch(JSONException e){ 
			e.printStackTrace(); 
		}
				
		return json;
	}
	
	
}
