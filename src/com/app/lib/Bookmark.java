package com.app.lib;

public class Bookmark {


	private String name;
	private double lat;
	private double lon;
	private String address;
	private long rowId;
	
	// used to identify object fields being passed
	// from Searchnearby to bookmarkedit acitivty via intents
	public static enum Field{
		NAME, ADDR, LAT, LON 
	}
	
	
	public Bookmark() {
		// TODO Auto-generated constructor stub
	}


	public long getRowId() {
		return rowId;
	}


	public void setRowId(long rowId) {
		this.rowId = rowId;
	}


	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}


	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * @return the lat
	 */
	public double getLat() {
		return lat;
	}


	/**
	 * @param lat the lat to set
	 */
	public void setLat(double lat) {
		this.lat = lat;
	}


	/**
	 * @return the lon
	 */
	public double getLon() {
		return lon;
	}


	/**
	 * @param lon the lon to set
	 */
	public void setLon(double lon) {
		this.lon = lon;
	}


	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}


	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}

}
