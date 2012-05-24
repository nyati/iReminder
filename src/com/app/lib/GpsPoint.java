package com.app.lib;

public class GpsPoint {

	private double lat;
	private double lon;
		
	public GpsPoint(double la, double lo) {
		// TODO Auto-generated constructor stub
	
		lat = la;
		lon = lo;
	}

	public String getPointAsString(){
		
		String p_point = new String(lat + "," + lon);
		
		return p_point;
	}
	

	public double getLat() {
		return lat;
	}


	public void setLat(double lat) {
		System.out.println("SETlat : " + lat);
		this.lat = lat;
	}


	public double getLon() {
		return lon;
	}


	public void setLon(double lon) {
		System.out.println("SETlon : " + lon);
		this.lon = lon;
	}

}
