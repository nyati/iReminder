package com.app.lib;

import java.util.ArrayList;

public class ItemFound{

		private String title;
		private String city;
		private String country;
		private String content;
		private String phone;
		private String fax;
		private double latitude;
		private double longitude;
		private String street;
		private String region;
		private String toHereFromMyLoc;
		private String toHereFromNewLoc;
		private String fromHereToNewLoc;
		private ArrayList<String> addressLines;	

		public ItemFound() {
			// TODO Auto-generated constructor stub
			addressLines = new ArrayList<String>();
		}
		
		/**
		 * @return the toHereFromMyLoc
		 */
		public String getToHereFromMyLoc() {
			return toHereFromMyLoc;
		}

		/**
		 * @param toHereFromMyLoc the toHereFromMyLoc to set
		 */
		public void setToHereFromMyLoc(String toHereFromMyLoc) {
			this.toHereFromMyLoc = toHereFromMyLoc;
		}

		/**
		 * @return the toHereFromNewLoc
		 */
		public String getToHereFromNewLoc() {
			return toHereFromNewLoc;
		}

		/**
		 * @param toHereFromNewLoc the toHereFromNewLoc to set
		 */
		public void setToHereFromNewLoc(String toHereFromNewLoc) {
			this.toHereFromNewLoc = toHereFromNewLoc;
		}

		/**
		 * @return the fromHereToNewLoc
		 */
		public String getFromHereToNewLoc() {
			return fromHereToNewLoc;
		}

		/**
		 * @param fromHereToNewLoc the fromHereToNewLoc to set
		 */
		public void setFromHereToNewLoc(String fromHereToNewLoc) {
			this.fromHereToNewLoc = fromHereToNewLoc;
		}

		public void addAddressLine(String line){
			addressLines.add(line);
		}
		
		public ArrayList<String> getAddressLines(){
			return addressLines;
		}
		
		/**
		 * @return the region
		 */
		public String getRegion() {
			return region;
		}
		/**
		 * @param region the region to set
		 */
		public void setRegion(String region) {
			this.region = region;
		}
		/**
		 * @param title the title to set
		 */
		public void setTitle(String title) {
			this.title = title;
		}
		/**
		 * @param city the city to set
		 */
		public void setCity(String city) {
			this.city = city;
		}
		/**
		 * @param country the country to set
		 */
		public void setCountry(String country) {
			this.country = country;
		}
		/**
		 * @param content the content to set
		 */
		public void setContent(String content) {
			this.content = content;
		}
		/**
		 * @param phone the phone to set
		 */
		public void setPhone(String phone) {
			this.phone = phone;
		}
		/**
		 * @param fax the fax to set
		 */
		public void setFax(String fax) {
			this.fax = fax;
		}
		/**
		 * @param latitude the latitude to set
		 */
		public void setLatitude(String latitude) {
			this.latitude = Double.parseDouble(latitude);
		}
		/**
		 * @param longitude the longitude to set
		 */
		public void setLongitude(String longitude) {
			this.longitude = Double.parseDouble(longitude);
		}
		/**
		 * @param street the street to set
		 */
		public void setStreet(String street) {
			this.street = street;
		}
		
		/**
		 * @return the title
		 */
		public String getTitle() {
			return title;
		}
		/**
		 * @return the city
		 */
		public String getCity() {
			return city;
		}
		/**
		 * @return the country
		 */
		public String getCountry() {
			return country;
		}
		/**
		 * @return the content
		 */
		public String getContent() {
			return content;
		}
		/**
		 * @return the phone
		 */
		public String getPhone() {
			return phone;
		}
		/**
		 * @return the fax
		 */
		public String getFax() {
			return fax;
		}
		/**
		 * @return the latitude
		 */
		public double getLatitude() {
			return latitude;
		}
		/**
		 * @return the longitude
		 */
		public double getLongitude() {
			return longitude;
		}
		/**
		 * @return the street
		 */
		public String getStreet() {
			return street;
		}
		public String print(){
			String str = "title: "+ title + ", Street: "+street;
			
			return str;
		}
}
