package com.app.iReminder;

import java.util.ArrayList;
import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.app.iReminder.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;

public class MapMe extends MapActivity{
    
    private MapView mapView;
    private MapController mapController;
    private MyLocationOverlay myLocOverlay;
    private LocationManager locManager;
    private LocationListener locListener;
	private ReminderDbAdapter remDbHelper;
    private BookmarkDbAdapter bmarkDbHelper;
	
    // min distance interval (in meters) to update GPS location coordinates
    public static final float DIST_INTERVAL = 200;
    // min time (in milliseconds) to update GPS location coordinates
    public static final long TIME_INTERVAL = 60000;
	
    /* 	
     * Sets the default zoom level when users current location
     * is updated on map. Is between 1 to 21 inclusive, 1 being
     * widest zoom out, with each successive zoom magnifying by 
     * a factor of 2.  
    */ 	
    public static final int DEFAULT_ZOOM = 12;
    // keeps track of the current zoom level set by the user
    private int zoomLevel = 0;
    
	/** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_me);
                
        remDbHelper = new ReminderDbAdapter(this);
        remDbHelper.open();

        bmarkDbHelper = new BookmarkDbAdapter(this);
        bmarkDbHelper.open();
        
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        mapController = mapView.getController();

        //zoomLevel = DEFAULT_ZOOM;
        
        // shows all reminders on the map
        createReminderOverlays();
        // shows all bookmarks on the map
        createBookmarkOverlays();
        
        // shows my location on the map
        createMyLocationOverlay();
		// initialize loc manager and loc listener
        initLocationService();

    }
	
	private void initLocationService(){
		
		locManager = (LocationManager) 
        getSystemService(MapMe.LOCATION_SERVICE);
		
		locListener = new LocationListener()
		{
		   	@Override
	        public void onLocationChanged(Location myLoc) {
		   		showLocation(myLoc);
	        }
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
    
    /**
     * Animates map to my current location
     * @param myLoc
     */
    private void showLocation(Location myLoc) {
		if(myLoc != null){			
			GeoPoint me = getGeoPoint(myLoc.getLatitude(), myLoc.getLongitude());			
			//Toast.makeText(getBaseContext(), "New Loc", Toast.LENGTH_LONG).show();			
			mapController.animateTo(me);			
			mapController.setZoom(mapView.getZoomLevel());
		}
	}
	
	/**
	 * Shows my location on the map
	 * 
	 */
	private void createMyLocationOverlay(){
        
		myLocOverlay = new MyLocationOverlay(this, mapView);
		mapView.getOverlays().add(myLocOverlay);
		
		mapView.postInvalidate();
	}
	
	/**
	 * Shows icons where each reminder is set for on the map
	 * 
	 */
	private void createReminderOverlays(){
		
        Drawable marker = this.getResources().getDrawable(R.drawable.pushpin);
        
        marker.setBounds(0,0,marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        
        ReminderOverlay itemizedoverlay = new ReminderOverlay(marker, this);
       
        Cursor reminderCursor = remDbHelper.fetchAllReminders(new String[]{
        		ReminderDbAdapter.KEY_LAT, 
        		ReminderDbAdapter.KEY_LNG,
        		ReminderDbAdapter.KEY_TITLE,
        		ReminderDbAdapter.KEY_ADDR	});
        
        startManagingCursor(reminderCursor);
        
        while(reminderCursor.moveToNext()){

        	String title = reminderCursor.getString(
        			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_TITLE));
        	
        	double lat = reminderCursor.getDouble(
        			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_LAT));
        	
        	double lon = reminderCursor.getDouble(
        			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_LNG));
        	
        	String addr = reminderCursor.getString(
        			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_ADDR));
        	
        	GeoPoint point = getGeoPoint(lat, lon);
        	
        	OverlayItem overlayitem = new OverlayItem(point, title, addr);
        	
        	itemizedoverlay.addOverlay(overlayitem);
        }
        
        if(itemizedoverlay.size() > 0){ // handle the case when there are no reminders       
        	mapView.getOverlays().add(itemizedoverlay);        	
        }
		
	}

	/**
	 * Shows icons where each bookmark is set for on the map
	 * 
	 */
	private void createBookmarkOverlays(){
		
        Drawable marker = this.getResources().getDrawable(R.drawable.bmark);
        
        marker.setBounds(0,0,marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
        
        BookmarkOverlay itemizedoverlay = new BookmarkOverlay(marker, this);
       
        Cursor bookmarkCursor = bmarkDbHelper.fetchAllBookmarks(); 
        
        startManagingCursor(bookmarkCursor);
        
        while(bookmarkCursor.moveToNext()){

        	String name = bookmarkCursor.getString(
        			bookmarkCursor.getColumnIndexOrThrow(BookmarkDbAdapter.KEY_NAME));
        	
        	double lat = bookmarkCursor.getDouble(
        			bookmarkCursor.getColumnIndexOrThrow(BookmarkDbAdapter.KEY_LAT));
        	
        	double lon = bookmarkCursor.getDouble(
        			bookmarkCursor.getColumnIndexOrThrow(BookmarkDbAdapter.KEY_LON));
        	
        	String addr = bookmarkCursor.getString(
        			bookmarkCursor.getColumnIndexOrThrow(BookmarkDbAdapter.KEY_ADDR));
        	
        	GeoPoint point = getGeoPoint(lat, lon);
        	
        	OverlayItem overlayitem = new OverlayItem(point, name, addr);
        	
        	itemizedoverlay.addOverlay(overlayitem);
        }
        
        if(itemizedoverlay.size() > 0){
        	mapView.getOverlays().add(itemizedoverlay);
        }
		
	}	
	

	/**
	 * Converts lat/lon pair into GeoPoint
	 * @param lat
	 * @param lon
	 * @return GeoPoint
	 */
	private GeoPoint getGeoPoint(double lat, double lon){
    	
		Double d_lat = lat*1E6;
    	Double d_lon = lon*1E6;
    	
    	GeoPoint p = new GeoPoint(d_lat.intValue(), d_lon.intValue());
    	
    	return p;
	}
	
    @Override
    protected boolean isRouteDisplayed() {
        // TODO Auto-generated method stub
        return false;
    }     
	
    @Override
    protected boolean isLocationDisplayed(){
    	return myLocOverlay.isMyLocationEnabled();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MapMe", "ON PAUSE CALLED");
        // stop listening for loc updates
        locManager.removeUpdates(locListener);
        // stop detecting/updating my current location
        myLocOverlay.disableMyLocation();
    }

    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.i("MapMe", "ON RESUME CALLED");
     	
    	if(remDbHelper == null){
    	  remDbHelper.open();
     	}
    	if(bmarkDbHelper == null){
      	  bmarkDbHelper.open();
       	}
    	
    	Location lastLoc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	showLocation(lastLoc);
    	locManager.requestLocationUpdates(
    			LocationManager.GPS_PROVIDER, 
    			TIME_INTERVAL, // min time in ms between location updates
    			DIST_INTERVAL, // min distance in meters 
    			locListener);
    	myLocOverlay.enableMyLocation(); // register for loc updates from loc seervice
    	myLocOverlay.runOnFirstFix(new Runnable(){
    		public void run(){
    			mapController.setCenter(myLocOverlay.getMyLocation());
    		}
    	});
        
    	// restore zoom level if setup or set to default
    	if(zoomLevel == 0){
    		zoomLevel = DEFAULT_ZOOM;
    	}else{
    		zoomLevel = mapView.getZoomLevel();
    	}
    }
    
    @Override
    protected void onDestroy(){
    	
    	super.onDestroy();
    	Log.i("MapMe", "ON DESTROY CALLED");
    	myLocOverlay.disableMyLocation();
    	if(remDbHelper != null){
    		remDbHelper.close();
    	}
    	if(bmarkDbHelper != null){
    		bmarkDbHelper.close();
    	}
    }

    public class ReminderOverlay extends ItemizedOverlay{

    	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
    	
    	private Context mContext;
    	
    	public ReminderOverlay(Drawable defaultMarker, Context context) {
    		//super(defaultMarker);
    		super(boundCenterBottom(defaultMarker));
    		mContext = context;
    	}

    	@Override
    	protected boolean onTap(int index) {
    	  OverlayItem item = mOverlays.get(index);
    	  AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
    	  dialog.setTitle(item.getTitle());
    	  dialog.setMessage(item.getSnippet());
    	  dialog.show();
    	  return true;
    	}
    	
    	public void addOverlay(OverlayItem overlay) {
    	    mOverlays.add(overlay);
    	    populate();
    	}
    	
    	// returns the index of the overlay item from the mOverlays arraylist
    	@Override
    	protected OverlayItem createItem(int i) {
    	  return mOverlays.get(i);
    	}
    	
    	@Override
    	public int size() {
    		// TODO Auto-generated method stub
    		return mOverlays.size();
    	}
    	
    }

    public class BookmarkOverlay extends ItemizedOverlay{

    	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
    	
    	private Context mContext;
    	
    	public BookmarkOverlay(Drawable defaultMarker, Context context) {
    		//super(defaultMarker);
    		super(boundCenterBottom(defaultMarker));
    		mContext = context;
    	}

    	@Override
    	protected boolean onTap(int index) {
    	  OverlayItem item = mOverlays.get(index);
    	  AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
    	  dialog.setTitle(item.getTitle());
    	  dialog.setMessage(item.getSnippet());
    	  dialog.show();
    	  return true;
    	}
    	
    	public void addOverlay(OverlayItem overlay) {
    	    mOverlays.add(overlay);
    	    populate();
    	}
    	
    	// returns the index of the overlay item from the mOverlays arraylist
    	@Override
    	protected OverlayItem createItem(int i) {
    	  return mOverlays.get(i);
    	}
    	
    	@Override
    	public int size() {
    		// TODO Auto-generated method stub
    		return mOverlays.size();
    	}
    	
    }
    
}