package com.app.iReminder;

import java.util.ArrayList;

import com.app.iReminder.R;
import com.app.lib.Bookmark;
import com.app.lib.GLocalSearch;
import com.app.lib.GeoCodec;
import com.app.lib.GeoDist;
import com.app.lib.GpsPoint;
import com.app.lib.ItemFound;
import com.app.lib.JSONParser;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SearchNearby extends Activity {

	private JSONParser jp;
	GLocalSearch localSearch; 
	Location myLocation;
	String searchTerm; // current search term
	
    private ProgressDialog m_ResultsProgressDialog = null;
    private ProgressDialog m_AddrProgressDialog = null;
    private ArrayList<ItemFound> mItems = null;
    private ItemAdapter mAdapter;
	
    private LocationManager locManager;
    private LocationListener locListener;
    
    private GeoCodec geoCodec;
    private String myAddress;
    
    private ListView itemList;
    EditText searchText;
    TextView resultCount;
    TextView myAddr;
    
    //private ItemFound selectedItem;
    
    private Dialog directionsDialog;
      
    private String TAG = "SearchNearby";
    
    // min distance interval (in meters) to update GPS location coordinates
    public static final float DIST_INTERVAL = 400;
    
    // min time (in milliseconds) to update GPS location coordinates
    public static final long TIME_INTERVAL = 60000;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_nearby);
        setTitle(R.string.search_nearby);
        
        itemList = (ListView) findViewById(R.id.item_list);        
        Button nextButton = (Button) findViewById(R.id.next_page);
        Button prevButton = (Button) findViewById(R.id.prev_page);
        Button searchButton = (Button) findViewById(R.id.search_button);
        searchText = (EditText) findViewById(R.id.search_box);
        resultCount = (TextView) findViewById(R.id.result_count);
        myAddr = (TextView) findViewById(R.id.myAddrLabel);
        
        initDirectionsDialog();
        
        resultCount.setText(R.string.no_results);
        
        initLocationService();

        // initial value of myLocation
        myLocation = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        
        geoCodec = new GeoCodec(this); // initializes geocodec class
        
        //"http://ajax.googleapis.com/ajax/services/search/local?v=1.0&q=pizza&sll=40.1,-73"

        localSearch = new GLocalSearch();
        
        mItems = new ArrayList<ItemFound>();
        this.mAdapter = new ItemAdapter(this, R.layout.place_entry, mItems);
        itemList.setAdapter(this.mAdapter);
        //this.setListAdapter(this.mAdapter);
        
        searchTerm = ""; // default value of search term to prevent null pointer exceptions
        jp = new JSONParser();
        
        // defines what happens when a search result is clicked
        //setSearchItemClickEvent();
        
		// Listener for search button
        searchButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	searchTerm = searchText.getText().toString();            	
            	fillData(myLocation, searchTerm, "0");            	
            }
        });

		// Listener for next button
        nextButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
           		fillData(myLocation, searchTerm, jp.getNextPageStart());
            }
        });

		// Listener for prev button
        prevButton.setOnClickListener(new View.OnClickListener() {
        	
            public void onClick(View view) {
            	fillData(myLocation, searchTerm, jp.getPrevPageStart());
            }
        });
        
    }
    
    // options menu - shows common search categories
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.places_options_menu, menu);
        return true;
    }
    // specifies what happens when user clicks on one of the categories
    // the json parser is initialized and fillData is called to
    // initiate search and show results
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
    	// Handle item selection
        switch (item.getItemId()) {
        
        case R.id.restaurant:
        	searchTerm = getText(R.string.restaurant).toString();
        	break;
            
        case R.id.coffee:
        	searchTerm = getText(R.string.coffee).toString();
        	searchText.setText(getText(R.string.coffee));
        	break;
        
        case R.id.pizza:
        	searchTerm = getText(R.string.pizza).toString();
        	break;
        	
        case R.id.gas:
        	searchTerm = getText(R.string.gas).toString();
        	break;       	
        	
        default:
            return super.onOptionsItemSelected(item);
        }
        
        searchText.setText(searchTerm);
    	jp = new JSONParser();
    	fillData(myLocation, searchTerm, "0");
    	return true;
        
    }
    

    // initializes the directions dialog
    private void initDirectionsDialog(){
    	
    	directionsDialog = new Dialog(this);
        directionsDialog.setContentView(R.layout.directions_dialog1);
        directionsDialog.setTitle(R.string.directions_header);
        directionsDialog.setCancelable(true);
        
        directionsDialog.setOwnerActivity(this);
        
        //set up text
        TextView text = (TextView)directionsDialog.findViewById(R.id.direction_title);
        text.setText(R.string.directions_title);

        //set up image view
        ImageView img = (ImageView)directionsDialog.findViewById(R.id.directions_logo);
        img.setImageResource(R.drawable.maps);
    }
    
    // sets up buttons in the directions dialog and displays it
    private void showDirectionsDialog(ItemFound selectedItem){

        //set up buttons
        Button toHereFromMyLoc = (Button) directionsDialog.findViewById(R.id.to_dest_from_here);
        toHereFromMyLoc.setText("To " + selectedItem.getTitle().toUpperCase() + " from my location");
        // store the selectedItem object within the view so it can be used to access
        // 
        toHereFromMyLoc.setTag(selectedItem); 
        toHereFromMyLoc.setOnClickListener(new OnClickListener() {        	        	
        	@Override
            public void onClick(View v) {
	        	//launch browser           
	            ItemFound selectedItem = (ItemFound) v.getTag();
        		Uri uri = Uri.parse(selectedItem.getToHereFromMyLoc());
	            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
	            startActivity(intent);
	            directionsDialog.dismiss();
            }
        });
        
        Button toHereFromNewLoc = (Button) directionsDialog.findViewById(R.id.to_dest_from_another);
        toHereFromNewLoc.setText("To "+ selectedItem.getTitle().toUpperCase() + " from another place");
        toHereFromNewLoc.setTag(selectedItem);
        toHereFromNewLoc.setOnClickListener(new OnClickListener() {        	        	
        	@Override
            public void onClick(View v) {
	        	//launch browser
        		ItemFound selectedItem = (ItemFound) v.getTag();
	            Uri uri = Uri.parse(selectedItem.getToHereFromNewLoc());
	            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
	            startActivity(intent);
	            directionsDialog.dismiss();
            }
        });
        
        Button fromHereToNewLoc = (Button) directionsDialog.findViewById(R.id.from_another_to_dest);
        fromHereToNewLoc.setText("From "+ selectedItem.getTitle().toUpperCase() + " to another place");
        fromHereToNewLoc.setTag(selectedItem);
        fromHereToNewLoc.setOnClickListener(new OnClickListener() {        	        	
        	@Override
            public void onClick(View v) {
	        	//launch browser
        		ItemFound selectedItem = (ItemFound) v.getTag();
	            Uri uri = Uri.parse(selectedItem.getFromHereToNewLoc());
	            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
	            startActivity(intent);
	            directionsDialog.dismiss();
            }
        });
        
        Button close = (Button) directionsDialog.findViewById(R.id.close_dialog);
        close.setOnClickListener(new OnClickListener() {        	        	
        	@Override
            public void onClick(View v) {
	            directionsDialog.dismiss();
            }
        });

        
        //now that the dialog is set up, it's time to show it    
        directionsDialog.show();
    	
    }
    	
    // starts the thread that queries 
    private void fillData(Location myLoc, String searchFor, String start){
    	
    	if(myLoc == null){
    		return;
    	}
    	mAdapter.clear();
    	// create thread to perform the search and get results
        Thread thread =  new Thread(null, new GetSearchResults(myLoc, searchFor, start), "Background Thread");
        // start background thread to query server and get search results
        thread.start();
        // show progress dialog while the results are being computed
        m_ResultsProgressDialog = ProgressDialog.show(this,    
              "Please wait..", "Searching for: " + searchTerm, true);    	
    }
    
   
    // This thread queries google local and fetches the results returned
    // Also updates current address if the location has changed since the last
    // time search was performed.
    final class GetSearchResults implements Runnable{

      	private Location myLoc;
      	private String searchFor;
      	private String start;
      	
      	public GetSearchResults(Location my_loc, String search_for, String page_start){
      		this.myLoc = my_loc;
      		this.searchFor = search_for;
      		this.start = page_start;
      	}
      	
		@Override
		public void run() {
			// queries server and fetches results
		    try{
		      	// do the issuing and parse of query in a separate thread than the primary UI thread
		        localSearch.setSearchParams(myLoc, searchFor, start);		    	        
		        jp.parseResults(localSearch.getJSONSearchResults());		    	    	
		    	mItems = jp.getItemsFound();
		    
		    	// geocoding of my location into address piggybacked on
		    	// search result updates.
			    retrieveMyAddress();
		    	
		    } catch (Exception e) {
		    	e.printStackTrace();
		        //Log.e("BACKGROUND_PROC", e.getMessage());
		    }
		    
		    // update UI thread (that's why it's called using runOnUIThread)
		    // since this method itself is called on a thread separate from the UI thread
		    runOnUiThread(updateResultsInUi);
	   }
    } 

    // Updates the UI view with results fetched from the GetSearchResults thread
    private Runnable updateResultsInUi = new Runnable() {

        @Override
        public void run() {
            if(mItems != null && mItems.size() > 0){
                mAdapter.notifyDataSetChanged();
                for(int i=0;i<mItems.size();i++)
                	mAdapter.add(mItems.get(i));
            }else{
            	
            }
            // results are now available, dismiss the progress dialog
            m_ResultsProgressDialog.dismiss();
            mAdapter.notifyDataSetChanged();
            // update the result count text field
            String result_count_text = new String(
            		"Found: " +
            		Integer.toString(jp.getEstimatedResultCount()) +
            		", Page " + jp.getPageNum() + " of " + jp.getTotalPages()          		
            		); 
            resultCount.setText( result_count_text );
            
            // updates address in UI
            updateMyAddress();          
        }
    };
    
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "ON PAUSE CALLED");
        // stop listening for loc updates
        locManager.removeUpdates(locListener);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.i(TAG, "ON RESUME CALLED");
    	locManager.requestLocationUpdates(
    			LocationManager.GPS_PROVIDER, 
    			TIME_INTERVAL, // min time in ms between location updates
    			DIST_INTERVAL, // min distance in meters 
    			locListener);
    	
        // update and show my address
    	Thread thread =  new Thread(null, new GetMyAddress(), "Background Thread");
        // start background thread to geocode address
        thread.start();
        // show progress dialog while the results are being computed
        m_AddrProgressDialog = ProgressDialog.show(this,    
              "Please wait...", "Retrieving current address ...", true); 
    }
    
    // This thread geocodes current location coordinates into address
    final class GetMyAddress implements Runnable{
    	@Override
    	public void run(){
    		retrieveMyAddress();
    		runOnUiThread(updateAddressInUi);
    	}    
    }
    
    // Updates the UI view with address fetched from the GetMyAddress thread
    private Runnable updateAddressInUi = new Runnable() {
		@Override
		public void run(){ 
			updateMyAddress();
			m_AddrProgressDialog.dismiss();
		}     		
    };
    
    /**
     * geoCodes the user's current location into an address and updates the
     * value of myAddress instance variable
     */
    private void retrieveMyAddress(){
   		
    	if(myLocation == null){
   			return;
   		}
    	
    	GpsPoint p = new GpsPoint(myLocation.getLatitude(), myLocation.getLongitude());
   		String address = geoCodec.getAddressFromGpsPoint(p);
    	//List<String> addresses = geoCodec.getAddresses(p, 1);
    	if(address.length() > 0){
   			myAddress = address;
   		}else{
   			// if an address could not be found for current location
   			// display current loc as gps coordinate
   			myAddress = p.getPointAsString();
   		}
   		
   		Log.e(TAG, "myAddress: " + myAddress);		
   		
    }
    
    /**
     * Updates current address in UI
     */
    private void updateMyAddress(){
    	myAddr.setText("Search Near: " + myAddress);
    }
    
    
	private void initLocationService(){
		
		locManager = (LocationManager) 
        getSystemService(SearchNearby.LOCATION_SERVICE);
		
		locListener = new LocationListener()
		{
		   	@Override
	        public void onLocationChanged(Location myLoc) {
		   		myLocation = myLoc;
		   	}
		   	// the following abstract methods must be implemented
			@Override
	        public void onProviderDisabled(String provider) {
	            // TODO Auto-generated method stub
	        }
	        @Override
	        public void onProviderEnabled(String provider) {
	            // TODO Auto-generated method stub
	        }
	        @Override
	        public void onStatusChanged(String provider, int status, Bundle extras) {
	            // TODO Auto-generated method stub
	        }
		};
		
	}
    
      
    // custom adapter for binding data items in ItemFound ArrayList to each list entry
    // and list view
    private class ItemAdapter extends ArrayAdapter<ItemFound> {

        private ArrayList<ItemFound> items;

        public ItemAdapter(Context context, int textViewResourceId, ArrayList<ItemFound> items) {
                super(context, textViewResourceId, items);
                this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                
                if (v == null) {
                    LayoutInflater v_inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = v_inflater.inflate(R.layout.place_entry, null);
                }

                Button directions_btn = (Button) v.findViewById(R.id.directions);
                Button bookmark_btn = (Button) v.findViewById(R.id.bookmark);
                
                // define listener for directions button in the place_entry view
                directions_btn.setFocusable(true);
                directions_btn.setClickable(true);
                directions_btn.setTag(items.get(position));
                directions_btn.setOnClickListener(new OnClickListener(){
                	@Override
                	public void onClick(View view){
                		ItemFound item = (ItemFound) view.getTag();
                		showDirectionsDialog(item);
                	}                	
                  } //OnClickListener    
                );
                
                // define listener for bookmark button in the place_entry view
                bookmark_btn.setFocusable(true);
                bookmark_btn.setClickable(true);
                bookmark_btn.setTag(items.get(position));
                bookmark_btn.setOnClickListener(new OnClickListener(){
                	@Override
                	public void onClick(View view){
                		ItemFound item = (ItemFound) view.getTag();
                		// populate the relevant item data in the intent
                		// and send the intent for firing the BookmarkEdit activity
                		// which will create a bookmark for this place
                		Intent newBookmark = new Intent(view.getContext(), BookmarkEdit.class);
                		//action tells the bookmarkedit activity what needs to be done
                		newBookmark.putExtra(BookmarkEdit.Params.CALLER.toString(), 
                				BookmarkEdit.Caller.SEARCH_NEARBY.ordinal());
                    	newBookmark.putExtra(BookmarkDbAdapter.KEY_ROWID, BookmarkDbAdapter.DEFAULT_INVALID_ROWID); 
                		newBookmark.putExtra(Bookmark.Field.NAME.toString(), item.getTitle());
                		newBookmark.putExtra(Bookmark.Field.ADDR.toString(), 
                				item.getStreet() + ", " + item.getRegion());
                		newBookmark.putExtra(Bookmark.Field.LAT.toString(), item.getLatitude());
                		newBookmark.putExtra(Bookmark.Field.LON.toString(), item.getLongitude());
                		// NOTE: in the bookmark edit acitivity the lat/lon coordinates 
                		// can be used to issue a AJAX query (q field) to get the URL 
                		// for the directions to this place from user's current loc
                		view.getContext().startActivity(newBookmark);
                	}                	
                  } //OnClickListener    
                );                
                
                // define how to bind the ItemFound object data to associated view
                ItemFound i = items.get(position);
                
                if (i != null){
                        TextView title = (TextView) v.findViewById(R.id.place_name);
                        TextView address = (TextView) v.findViewById(R.id.address);
                        TextView phone = (TextView) v.findViewById(R.id.phone);
                        TextView distance = (TextView) v.findViewById(R.id.distance);
                        
                        if(title != null){
                        	title.setText("Name: "+ i.getTitle() );
                        }
                        if(address != null){
                        	address.setText("Address: "	+ 
                        			i.getStreet() + ", " +
                        			i.getRegion() + ", " +
                        			i.getCity()
                        			);
                        }
                        if(phone != null){
                        	phone.setText("Phone: "+ i.getPhone());

                        }
                        if(distance != null){
                        	GpsPoint place_gps = new GpsPoint(i.getLatitude(), i.getLongitude());
                        	GpsPoint my_gps = new GpsPoint(myLocation.getLatitude(), myLocation.getLongitude());
                        	GeoDist geoDist = new GeoDist();
                        	double dist = geoDist.getSurfaceDistance(my_gps, place_gps);
                        	dist = geoDist.roundDistance(dist, 1);
                        	distance.setText("Approx " + dist + " miles");
                        }
        		}
                
                
                return v;
        }
		
    }
    
}