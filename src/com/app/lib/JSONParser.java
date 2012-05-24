package com.app.lib;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JSONParser{

	// data
	private String TAG = "JSONParser";
	private ArrayList<ItemFound> items;
	private ItemFound itemFound;
	private int currentPageIndex;
	private int estimatedResultCount;	
	private ArrayList<Page> pages;
	
	private String responseDetails;
	private String responseStatus;
	
	private int startOffset; // number of pages returned in a single ajax query
	private int pageNum; // keeps track of what page number is currently been shown
						 // note pageNum start at 1 (even though page numbers 
						 // are 0-based indexed
	private int totalPages;
	
	private int lastPageStart;
	
	public JSONParser(){
		items = new ArrayList<ItemFound>();
		pages = new ArrayList<Page>();
		responseDetails = "";
		responseStatus = "";
		pageNum = 1;
		totalPages = 0;
		
		startOffset=0;
		lastPageStart=0;
	}


	public void parseResults(JSONObject json_results) {
		items.clear();
		pages.clear();
		try {
			parse(json_results);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getNextPageStart(){
		
		if(lastPageStart >= (estimatedResultCount - 1)){
			return Integer.toString(lastPageStart);
		}
		
		int nextPageStart = lastPageStart + startOffset;
		
		lastPageStart += startOffset;
		pageNum += 1;
		
		Log.w(TAG, "next page start: " + nextPageStart);
		
		return Integer.toString(nextPageStart);
	}
	
	public String getPrevPageStart(){
		
		if(lastPageStart == 0){
			return Integer.toString(lastPageStart);			
		}
		
		int prevPageStart = lastPageStart - startOffset;
		
		lastPageStart -= startOffset;
		pageNum -= 1;
		
		Log.w(TAG, "prev page start: " + prevPageStart);
		
		return Integer.toString(prevPageStart);

	}
	

	private boolean parse(JSONObject json) {
		try {
			if(json.has("responseData")){
				// response data object
				JSONObject jResponseData = json.getJSONObject("responseData");
			
				if(jResponseData.has("results")){
					// results array
					JSONArray jResults = jResponseData.getJSONArray("results");
					// populate items from the results array
					populateItems(jResults);
				} //results
				
				// get response Details  and Status
				if(json.has("responseDetails")){
					this.responseDetails = json.getString("responseDetails");
				}
				if(json.has("responseStatus")){
					this.responseStatus = json.getString("responseStatus");
				}
				Log.e(TAG, "responseDetails: " + responseDetails + 
						" responseStatus: " + responseStatus);
				
				if(jResponseData.has("cursor")){
					// cursor object
					JSONObject jCursor = jResponseData.getJSONObject("cursor");
					
					if(jCursor.has("pages")){
						// pages array inside the cursor object
						JSONArray jPages = jCursor.getJSONArray("pages");
						// populate pages array
						populatePages(jPages);
					}
					
					if(jCursor.has("estimatedResultCount")){
						// elements of cursor object
						this.estimatedResultCount = Integer.parseInt(
								jCursor.getString("estimatedResultCount").trim());
					
						this.totalPages = estimatedResultCount/startOffset;	
					}
					
					if(jCursor.has("currentPageIndex")){
						this.currentPageIndex = Integer.parseInt(
							jCursor.getString("currentPageIndex").trim());
	
					}
				}//cursor
			
						
			Log.e(TAG, "estimatedResultCount: " + estimatedResultCount + 
					" currentPageIndex: " + currentPageIndex);			
			Log.e(TAG, "startOffset: " + startOffset);
			Log.e(TAG, "totalPages: " + totalPages);
			
			} // response data
		}
		catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	/**
	 * Finds and populates the items found
	 * @param jResults
	 */
	private void populateItems(JSONArray jResults){
		jResults.toString();
		try {
			for(int i = 0; i < jResults.length(); i++){
				JSONObject jObj = jResults.getJSONObject(i);
				itemFound = new ItemFound();
				
				if(jObj.has("titleNoFormatting")){
					itemFound.setTitle(jObj.getString("titleNoFormatting"));
				}
				
				if(jObj.has("city")){
					itemFound.setCity(jObj.getString("city"));
				}
				
				if(jObj.has("country")){
					itemFound.setCountry(jObj.getString("country"));
				}
					
				if(jObj.has("content")){
					itemFound.setContent(jObj.getString("content"));
				}
				
				if(jObj.has("phoneNumbers")){
					JSONArray phoneObj = jObj.getJSONArray("phoneNumbers");
									
					for(int j = 0; j < phoneObj.length(); j++){
						JSONObject jPhoneObj = phoneObj.getJSONObject(j);
						if(jPhoneObj.getString("type").length() < 1)
							itemFound.setPhone(jPhoneObj.getString("number"));
						else if(jPhoneObj.getString("type").equals("Fax"))
							itemFound.setFax(jPhoneObj.getString("number"));
					}					
				}
				
				if(jObj.has("addressLines")){
					JSONArray addressObj = jObj.getJSONArray("addressLines");
					for(int j = 0; j < addressObj.length(); j++){
						itemFound.addAddressLine(addressObj.getString(j));
					}
				}
				
				itemFound.setLatitude(jObj.getString("lat"));
				itemFound.setLongitude(jObj.getString("lng"));
				itemFound.setStreet(jObj.getString("streetAddress"));
				itemFound.setRegion(jObj.getString("region"));
				itemFound.setToHereFromMyLoc(jObj.getString("ddUrl"));
				itemFound.setToHereFromNewLoc(jObj.getString("ddUrlToHere"));
				itemFound.setFromHereToNewLoc(jObj.getString("ddUrlFromHere"));
				
				Log.e(TAG, "ItemFound: " + itemFound.toString());
				Log.w(TAG, itemFound.print());
				items.add(itemFound);
			}
			Log.e(TAG, "parse() finished with " + items.size() + " items found!");
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void populatePages(JSONArray jPages){
		jPages.toString();
		try {
			startOffset = jPages.length();
			for(int i = 0; i < jPages.length(); i++){
				JSONObject jObj = jPages.getJSONObject(i);
				Page page = new Page();
				page.setLabel(jObj.getString("label"));
				page.setStart(jObj.getString("start"));
				pages.add(page);
			}			
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private class Page{
		
		private int start;
		private String label;
		/**
		 * @param start the start to set
		 */
		public void setStart(String start) {
			this.start = Integer.parseInt(start);
		}
		/**
		 * @param label the label to set
		 */
		public void setLabel(String label) {
			this.label = label;
		}
		/**
		 * @return the start
		 */
		public int getStart() {
			return start;
		}
		/**
		 * @return the label
		 */
		public String getLabel() {
			return label;
		}				
	}	

	public ArrayList<ItemFound> getItemsFound() {
		return items;
	}
	
	public ArrayList<Page> getPages() {
		return pages;
	}
	
	public int getPageNum() {
		return pageNum;
	}
	
	public int getTotalPages() {
		return totalPages;
	}
	
	/**
	 * @return the currentPageIndex
	 */
	public int getCurrentPageIndex() {
		return currentPageIndex;
	}

	/**
	 * @return the estimatedResultCount
	 */
	public int getEstimatedResultCount() {
		return estimatedResultCount;
	}

	/**
	 * @return the responseDetails
	 */
	public String getResponseDetails() {
		return responseDetails;
	}

	/**
	 * @return the responseStatus
	 */
	public String getResponseStatus() {
		return responseStatus;
	}
	
}

