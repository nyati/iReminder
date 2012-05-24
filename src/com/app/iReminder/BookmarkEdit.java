package com.app.iReminder;

import com.app.iReminder.R;
import com.app.lib.Bookmark;
import com.app.lib.GeoCodec;
import com.app.lib.GpsPoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class BookmarkEdit extends Activity {

	private Long mRowId;
	private EditText mNameText;
	private EditText mAddrText;
	private double OrigLat;
	private double OrigLon;
	
    private BookmarkDbAdapter bmarkDbHelper;
    private GeoCodec geoCodec;
    private String mLastAddr;
    private Bookmark mBookmark;
    private LocationManager locManager;
    private LocationListener locListener;
	private Location myLocation;
    
	public static enum ResultCode{
		SAVED, INVALID_ADDR, SAVE_ERROR
	}
	// Activities from which intents are received to start this
	// activity
	public static enum Caller{
		SEARCH_NEARBY,
		BOOKMARK_LIST
	}
	// Parameters passed to BookmarkEdit class.
	public static enum Params{
		CALLER
	}
	
	public static enum AddrSource{
		CURRENT,  // "use current" location button is used
		SEARCH_NEARBY, // the bookmark button in search nearby feature was used
		DEFAULT, // address entered manually by user or populated automatically
	}
    // represents the source where the address is obtained from
	private AddrSource mAddrSource;
	private static final String TAG = "BookmarkEdit";
    public static enum Action {SAVE, CANCEL, DELETE, NEW_BOOKMARK};
    // min distance interval (in meters) to update GPS location coordinates
    public static final float DIST_INTERVAL = 400;
    // min time (in milliseconds) to update GPS location coordinates
    public static final long TIME_INTERVAL = 60000;
    private ProgressDialog m_ProgressDialog = null;
    private Context mCtx;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bookmark_edit);
        setTitle(R.string.edit_bookmark);
        
        mCtx = this;
        bmarkDbHelper = new BookmarkDbAdapter(this);
        bmarkDbHelper.open(); 
        
        mNameText = (EditText) findViewById(R.id.bookmark_name);
        mAddrText = (EditText) findViewById(R.id.bookmark_addr);
   
        geoCodec = new GeoCodec(this);        
        mLastAddr = new String();
        
        initLocationService(); // setup location service
        // initial value of myLocation
        myLocation = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
       
        
        mAddrSource = AddrSource.DEFAULT; // default is manual addr entry
        mBookmark = new Bookmark();
        
        // restore rowId from saved instance if it saved insatnce state is not null
        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(BookmarkDbAdapter.KEY_ROWID);		
        // otherwise get it from the intent sent by the BoomarkEdit activity
        // when an existing entry in the list is clicked
        if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			// NOTE: if the extras bundle has no entry of with with associated 
			// key, it the getLong function returns 0. This causes errors when
			// new bookmarks are created - since rowId must be null
			// To resolve this, the addBookmark() in BookmarkList passes in
			// -1 for rowid 
			long rowid = extras.getLong(BookmarkDbAdapter.KEY_ROWID);
			Log.e(TAG, "row id from bundle: " + rowid);
			if(rowid >= 0){
				mRowId = rowid;
			}else{
				mRowId = null;
			}
			
		}
        Log.e(TAG, "Rowid = " + mRowId);
        
        // if mRowId is still null a new bookmark is being created
        // new bookmarks can be created either by "Add Boomark" button in BookmarkList
        // or by "add bookmark" in search nearby. populateFields method 
        // checks the source and populates the fields in Edit Bookmark screen accordingly
        populateFields();

        setupButtons();
    }
    
    // populates the fields in the bookmark edit activity
    // with what's in the database when an existing reminder is opened. 
    // populates from search nearby data if mRowId is null and calling activity
    // is searchnearby. If the calling activity is BookmarkList, leaves the fields
    // blank and does nothing
    private void populateFields() {
        if (mRowId != null) {
            // populate from database - existing bookmark entry on bookmark list is
        	// clicked
        	Cursor bookmark = bmarkDbHelper.fetchBookmark(mRowId);
            startManagingCursor(bookmark);
            mNameText.setText(bookmark.getString(
                    bookmark.getColumnIndexOrThrow(BookmarkDbAdapter.KEY_NAME)));
            
            mAddrText.setText(bookmark.getString(
                    bookmark.getColumnIndexOrThrow(BookmarkDbAdapter.KEY_ADDR)));
            
            OrigLat = bookmark.getDouble(bookmark.getColumnIndexOrThrow(BookmarkDbAdapter.KEY_LAT));
            OrigLon = bookmark.getDouble(bookmark.getColumnIndexOrThrow(BookmarkDbAdapter.KEY_LON));
            
            Log.e(TAG, "Lat and long in populate : " +OrigLat + " , " + OrigLon);
        }else{
        	// populate from search nearby results if caller is SearchNeabry Activity
        	Bundle extras = getIntent().getExtras();
        	if(extras != null){
        		int caller = extras.getInt(BookmarkEdit.Params.CALLER.toString());
	        	if(caller == BookmarkEdit.Caller.SEARCH_NEARBY.ordinal()){
	        		
	        		mNameText.setText(extras.getString(Bookmark.Field.NAME.toString()));
	        		mAddrText.setText(extras.getString(Bookmark.Field.ADDR.toString()));
	        		
	        		// populate the lat/lon fields of the bookmark object
	        		mBookmark.setLat(extras.getDouble(Bookmark.Field.LAT.toString()));
	        		mBookmark.setLon(extras.getDouble(Bookmark.Field.LON.toString()));
	        		// set the addr source to search nearby so VerifyAndSave thread 
	        		// can handle it appropriately
	        		mAddrSource = AddrSource.SEARCH_NEARBY;
	        		
	        	}
	        	// don't do anything if calling activity is not BookmarkList
        	}
        }
        // save the initial value of address field
        mLastAddr = mAddrText.getText().toString();
    }
    
    

    private void setupButtons(){

        Button useCurrentButton = (Button) findViewById(R.id.current_loc);
        Button confirmButton = (Button) findViewById(R.id.save_bookmark);
        Button cancelButton = (Button) findViewById(R.id.cancel_bookmark);
        Button deleteButton = (Button) findViewById(R.id.delete_bookmark);

        // Listener for "Use Current" button
        useCurrentButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	// set the lat/lon for the reminder 
            	mBookmark.setLat(
            			myLocation.getLatitude());
            	mBookmark.setLon(myLocation.getLongitude());
            	// set the addr field to my current loc and
            	// do the geocoding later
            	mAddrText.setText(getString(R.string.my_current_addr));
            	mAddrSource = AddrSource.CURRENT;
            }
        });
    
        
    	// Listener for confirm button
        confirmButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                performAction(Action.SAVE);
            }

        });
        
        
		// Listener for cancel button
        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	performAction(Action.CANCEL);
            }

        });
        
		// Listener for delete button
        deleteButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	performAction(Action.DELETE);
            }

        });       
   	
    }
    

    // Allows saving the state of the activity and restoring it on resume
	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //saveState();
        outState.putSerializable(BookmarkDbAdapter.KEY_ROWID, mRowId);
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
	
    private void performAction(Action action) {
        
    	switch( action ){
    	
    		case SAVE: 
    	    	
    			
    			mBookmark.setName(mNameText.getText().toString());
    			mBookmark.setAddress(mAddrText.getText().toString());
    			mBookmark.setLat(OrigLat);
    			mBookmark.setLon(OrigLon);
    			
    			 Log.e(TAG, "Lat and long in Save : " +mBookmark.getLat() + " , " + mBookmark.getLon());
    			Log.e(TAG, "Address : " + mBookmark.getAddress());
    			
    	    	// create thread to verify and save results
    	        Thread thread =  new Thread(null, new VerifyAndSave(mBookmark), "Background Thread");
    	        // start background thread
    	        thread.start();
    	        // show progress dialog while the results are being verified and saved
    	        m_ProgressDialog = ProgressDialog.show(this,    
    	              getString(R.string.bookmark_progress_dialog_title), 
    	              getString(R.string.bookmark_progress_dialog_body), 
    	              true);
    	        
            	break;

    		case CANCEL:
    			setResult(RESULT_CANCELED);
    			finish();
    			break;
    			
    		case DELETE:    		
    			if( mRowId != null){
    				bmarkDbHelper.deleteBookmark(mRowId);
    			}
    			setResult(RESULT_OK);
    			finish();
    			break;
    	}
    	
    }
	
    // Verifies (by geocoding) and saves user entered addresses by
	// performing database Create/update operation 
    // User prompted to re-enter address if invalid in a pop up dialog
    final class VerifyAndSave implements Runnable{
    	
    	private Bookmark bookmark;
    	private ResultCode result;
    	
      	public VerifyAndSave(Bookmark b){
      		this.bookmark = b;
      		this.result = ResultCode.INVALID_ADDR;
      	}
      	
      	// true if address field has been changed, false otherwise
        public boolean isAddrChanged(){
        	//Log.e(TAG, "Current Addr: " + mAddrText.getText().toString() );
        	//Log.e(TAG, "Original Addr: " + mLastAddr );
        	//Log.e(TAG, "Is Equal?: " + (mAddrText.getText().toString().trim().equals(mLastAddr)));
        	return !mAddrText.getText().toString().trim().equals(mLastAddr);
        }

		@Override
		public void run() {
		  	try{    	        
		  		// if address has changed and manually entered by user, 
		  		// geo-code address and see if valid
		  		if(isAddrChanged() && mAddrSource == AddrSource.DEFAULT){		  			
		  			GpsPoint p = geoCodec.getGpsPointFromAddress(mAddrText.getText().toString());
		  			// if addr is invalid do not save bookmark
		  			// and update pass/fail status
		  			if(p == null){
		  				result = ResultCode.INVALID_ADDR;
		  				runOnUiThread(new ShowStatus(result));
		  				return;	  			
		  			}else{
		  				// if addr is valid set the lat/long coordinates
		  				bookmark.setLat(p.getLat());
		  				bookmark.setLon(p.getLon());
		  			}
		  		}
		  		
		  		
		  		
		  		
		  		if(mAddrSource == AddrSource.CURRENT){
		  			Log.e(TAG, "Addr source current");
		  			// reverse geocode gps coordinates into address
		  			// the lat/lon is already populated by the listener for
		  			// the "use current" button
		  			GpsPoint p = new GpsPoint(bookmark.getLat(), bookmark.getLon());
		  			
		  			Log.e(TAG, "Current GPS point: " + p.getPointAsString());
		  			
		  			String address = geoCodec.getAddressFromGpsPoint(p);
		  			
		  			// if the gps points could not be correctly translated into
		  			// an address, use the gps coordinates for the address field.
		  			if(address.length() == 0){
		  				address = p.getPointAsString();
		  			}
		  			
		  			Log.e(TAG, "Current Address: " + address);
		  			// update the address attribute of reminder
		  			bookmark.setAddress(address);
		  		}
		  		
		  		// when AddrSource is SEARCH_NEARBY, no geocoding is needed
		  		// since selected a location provides both
		  		// the address and gps coordinate of the location.
		  		
		  		// save to database
    	        if (mRowId == null) { // new reminder
    	        	long id = bmarkDbHelper.createBookmark(bookmark);
    	            if (id > 0) {
    	                // updated RowId of saved reminder so it
    	                // can be restored
    	            	mRowId = id; 
    	                result = ResultCode.SAVED;
    	            }else{
    	            	result = ResultCode.SAVE_ERROR;
    	            }
    	        } else { // update existing reminder
    	            if(bmarkDbHelper.updateBookmark(mRowId, bookmark)){
    	            	result = ResultCode.SAVED;
    	            }else{
    	            	result = ResultCode.SAVE_ERROR;
    	            }
    	        }		  		
		  		
		   	} catch (Exception e) {
		   		e.printStackTrace();
		        //Log.e("BACKGROUND_PROC", e.getMessage());
		    }
        	runOnUiThread(new ShowStatus(result));
		}
    	
    }	
    
    // Updates the UI view (if not saved show dialog)
    final class ShowStatus implements Runnable{
    	
    	private ResultCode result;
    	
    	public ShowStatus(ResultCode s){
    		this.result = s;
    	}
    	
    	public AlertDialog createFailureAlert(){
    		
    		AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
    		
    		String message = "";
    		
        	switch(result){
    			case INVALID_ADDR:
    				message = getString(R.string.invalid_address);
    				break;
        		case SAVE_ERROR:
        			message = getString(R.string.save_error);
        			break;
        	}   		
    		
    		// here methods can be chained since each method returns the AlertBuilder
    		// object
    		builder.setMessage(
    				message)
    		       .setCancelable(false)
    		       .setPositiveButton("Close", new DialogInterface.OnClickListener() {
    		           public void onClick(DialogInterface dialog, int id) {
    		        	   dialog.cancel(); 
    		           }
    		       });    		       
    		       
    		return builder.create();
    	}
    	
        @Override
        public void run() {
        	// dismiss progress dialog
        	m_ProgressDialog.dismiss();
        	
        	Log.e(TAG, "result: " + result.name() );
        	
        	if(result == ResultCode.INVALID_ADDR || result == ResultCode.SAVE_ERROR){ // failed
        		AlertDialog failAlert = createFailureAlert();
        		failAlert.show();
        	}else{
        		// succeeded 
    			BookmarkEdit.this.setResult(RESULT_OK);
    			BookmarkEdit.this.finish();
        	}       	
        	
        };
    };

    
	
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "ON PAUSE CALLED");
        //saveState();
        //bmarkDbHelper.close(); 
        
        // stop listening for loc updates
        locManager.removeUpdates(locListener);
    }

    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.i(TAG, "ON RESUME CALLED");
     	if(bmarkDbHelper == null){
    	  bmarkDbHelper.open();
     	}
    	populateFields();
    	
    	locManager.requestLocationUpdates(
    			LocationManager.GPS_PROVIDER, 
    			TIME_INTERVAL, // min time in ms between location updates
    			DIST_INTERVAL, // min distance in meters 
    			locListener);
    }
    
    @Override
    protected void onDestroy(){
    	
    	super.onDestroy();
    	Log.i(TAG, "ON DESTROY CALLED");
    	if(bmarkDbHelper != null){
    		bmarkDbHelper.close();
    	}
    }

}
