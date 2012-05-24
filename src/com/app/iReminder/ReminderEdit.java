package com.app.iReminder;

import com.app.iReminder.R;
import com.app.lib.GeoCodec;
import com.app.lib.GpsPoint;
import com.app.lib.Reminder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

public class ReminderEdit extends Activity {

	private Long mRowId;
	private EditText mTitleText;
	private EditText mAddrText;
	private EditText mBodyText;
	private double OrigLat,OrigLon;
	
	private RadioButton mEnable;
	private RadioButton mDisable;
	
	private CheckBox mRing;
	private CheckBox mVibrate;
	private CheckBox mFlash;
	
	private CheckBox mEntry;
	private CheckBox mExit;
	
	private EditText mRangeText;
	
    private ReminderDbAdapter mDbHelper;
    
    private enum Action {CONFIRM, CANCEL, DELETE};
    
    /**
     * Activity intent request code to identify sent/received intents
     */
    private static enum Message{
    	SELECT_BOOKMARK   	
    }
        
    private GeoCodec geoCodec;
    
    private ProgressDialog m_ProgressDialog = null;
    
    private String mLastAddr;
    
	public static enum ResultCode{
		SAVED, INVALID_ADDR, SAVE_ERROR
	}
	
	public static enum AddrSource{
		BOOKMARKS, // address is obtained from bookmarked locations
		CURRENT,  // "use current" location button is used 
		MANUAL, // address entered manually by use
	}
    // represents the source where the address is obtained from
	private AddrSource mAddrSource;
	
	private static final String TAG = "ReminderEdit";
    
	private Reminder mReminder; // object representing the current reminder
    private LocationManager locManager;
    private LocationListener locListener;
	private Location myLocation;
    // min distance interval (in meters) to update GPS location coordinates
    public static final float DIST_INTERVAL = 400;
    // min time (in milliseconds) to update GPS location coordinates
    public static final long TIME_INTERVAL = 60000;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reminder_edit);
        setTitle(R.string.edit_reminder);

        mDbHelper = new ReminderDbAdapter(this);
        mDbHelper.open(); 
        
        mTitleText = (EditText) findViewById(R.id.title);
        mBodyText = (EditText) findViewById(R.id.body);
        mAddrText = (EditText) findViewById(R.id.addr);
        
        mEnable = (RadioButton)findViewById(R.id.enable);
        mDisable = (RadioButton) findViewById(R.id.disable);
        // sets default value for reminder state: enabled by default
        // used when a new reminder is created
        setReminderState(Reminder.DEFAULT_STATE.ordinal());
        
        mRing = (CheckBox) findViewById(R.id.ring);
        mVibrate = (CheckBox) findViewById(R.id.vibrate);
        mFlash = (CheckBox) findViewById(R.id.flash);
        // set default alert if none specified
        setReminderAlert(Reminder.DEFAULT_ALERT.ordinal()); 
        
        mEntry = (CheckBox) findViewById(R.id.on_entry);
        mExit = (CheckBox) findViewById(R.id.on_exit);
        setReminderEvent(Reminder.DEFAULT_EVENT.ordinal());
        
        mRangeText = (EditText) findViewById(R.id.range);
        // default range is 1 m
        mRangeText.setText(Double.toString(Reminder.DEFAULT_RANGE)); 

        mReminder = new Reminder();
        
        initLocationService(); // setup location service
        // initial value of myLocation
        myLocation = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        
        mAddrSource = AddrSource.MANUAL; // default is manual addr entry
        
        // default action is to cancel the reminder
        //action = Action.CANCEL;
        geoCodec = new GeoCodec(this);
        
        mLastAddr = new String();
        
        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(ReminderDbAdapter.KEY_ROWID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(ReminderDbAdapter.KEY_ROWID)
									: null;
		}
		Log.e(TAG, "Rowid = " + mRowId);
		populateFields();
        
		// sets up the event listeners for each button
		setupButtons();
    }

    private void setupButtons(){

        Button useCurrentButton = (Button) findViewById(R.id.use_current);
        Button fromBookmarksButton = (Button) findViewById(R.id.select_from_bookmarks);
        Button confirmButton = (Button) findViewById(R.id.confirm_rem);
        Button cancelButton = (Button) findViewById(R.id.cancel_rem);
        Button deleteButton = (Button) findViewById(R.id.delete_rem);

        // Listener for "Use Current" button
        useCurrentButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	mAddrText.setText(getString(R.string.my_current_addr));
            	mAddrSource = AddrSource.CURRENT;
            }

        });
        
        // Listener for "From Bookmarks" button
        fromBookmarksButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	// results in populating the addr field and gps coordinate fields 
            	// of the Reminder object
            	selectAddressFromBookmarks(); 
            	mAddrSource = AddrSource.BOOKMARKS;
            }

        });        
        
    	// Listener for confirm button
        confirmButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                performAction(Action.CONFIRM);
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

    // Sends an intent to ReminderEdit activity to edit the note
    private void selectAddressFromBookmarks(){
        Intent i = new Intent(this, BookmarkList.class);
        // put something inside the extras bundle (doesn't matter what) so the BookmarkList
        // class can distinguish between intents received from this acitivity
        // and others and take appropriate action.
        i.putExtra(Message.SELECT_BOOKMARK.toString(), Message.SELECT_BOOKMARK.ordinal());
        startActivityForResult(i, Message.SELECT_BOOKMARK.ordinal());   
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    	// this method is called just before onResume. Database handle needs
    	// to be opened here since it was closed by onPause.
    	if(mDbHelper == null){
    		mDbHelper.open();
    	}
    	
    	// is soemthing bad happened display message to user and switch to manual address mode
		// if result code is not RESULT_OK
		// the selection did not work, default to manual address entry
    	if(resultCode != RESULT_OK){
    		mAddrSource = AddrSource.MANUAL;
    		return;
    	}
    	
    	// extract the result of selecting a bookmarked location here
    	Message message_code = Message.values()[requestCode];
    	switch(message_code){
    		case SELECT_BOOKMARK:
    			// extract address and gps point from bookmark selection
    			Bundle extras = intent.getExtras();
    			// populate reminder object fields with the info obtained
    			mReminder.setAddr(extras.getString(BookmarkDbAdapter.KEY_ADDR));
    			mReminder.setLat(extras.getDouble(BookmarkDbAdapter.KEY_LAT));
    			mReminder.setLon(extras.getDouble(BookmarkDbAdapter.KEY_LON));    			
    			// update the view with the new address obtained
    			mAddrText.setText(mReminder.getAddr());
    			
    			break;
    			
    	}
    }
   

    // Allows saving the state of the activity and restoring it on resume
	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //saveState();
        outState.putSerializable(ReminderDbAdapter.KEY_ROWID, mRowId);
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
	
	
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "ON PAUSE CALLED");
        //saveState();
        //mDbHelper.close(); 
        
        // stop listening for loc updates
        locManager.removeUpdates(locListener);
    }

    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.i(TAG, "ON RESUME CALLED");
     	if(mDbHelper == null){
    	  mDbHelper.open();
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
    	if(mDbHelper != null){
    		mDbHelper.close();
    	}
    }

    // populates the fields in the reminder edit acitivity
    // with what's in the database when an existing reminder is opened. 
    // Does nothing if a new reminder is being created
    private void populateFields() {
        if (mRowId != null) {
            Cursor reminder = mDbHelper.fetchReminder(mRowId);
            startManagingCursor(reminder);
            mTitleText.setText(reminder.getString(
                    reminder.getColumnIndexOrThrow(ReminderDbAdapter.KEY_TITLE)));
            mBodyText.setText(reminder.getString(
                    reminder.getColumnIndexOrThrow(ReminderDbAdapter.KEY_BODY)));
            
            OrigLat = reminder.getDouble(reminder.getColumnIndexOrThrow(ReminderDbAdapter.KEY_LAT));
            OrigLon = reminder.getDouble(reminder.getColumnIndexOrThrow(ReminderDbAdapter.KEY_LNG));
            
            Log.e(TAG, "Lat and long in populate : " +OrigLat + " , " + OrigLon);
            
            // update address field view from database only if 
            // user does not choose address from bookmarks
            if(mAddrSource != AddrSource.BOOKMARKS){
            	mAddrText.setText(reminder.getString(
                    reminder.getColumnIndexOrThrow(ReminderDbAdapter.KEY_ADDR)));
            }
            
            // set the range
            double range = reminder.getDouble(
                    reminder.getColumnIndexOrThrow(ReminderDbAdapter.KEY_RADIUS));
            mRangeText.setText(Double.toString(range));
                       
            // sets the  reminder state radio button
            setReminderState(
            		reminder.getInt(reminder.getColumnIndexOrThrow(ReminderDbAdapter.KEY_STATE)));

            // sets the reminder alert radio button
            setReminderAlert(reminder.getInt(
            		reminder.getColumnIndexOrThrow(ReminderDbAdapter.KEY_ALERT)));
            
            // set reminder event checkbox
            setReminderEvent(reminder.getInt(
            		reminder.getColumnIndexOrThrow(ReminderDbAdapter.KEY_EVENT)));
        }
        
        // save the initial value of address field
        mLastAddr = mAddrText.getText().toString().trim();
    }
    
    
    private void performAction(Action action) {
        
    	switch( action ){
    	
    		case CONFIRM: 
    	    	
    			//Reminder mReminder = new Reminder();
    			
    			mReminder.setTitle(mTitleText.getText().toString());
    			mReminder.setBody(mBodyText.getText().toString());
    			//mReminder.setAddr(mAddrText.getText().toString());
    			mReminder.setState(getReminderState());
    			mReminder.setAlert(getReminderAlert());
    			mReminder.setRange(Double.parseDouble(mRangeText.getText().toString()));
    			// NOTE: if user unchecks both entry and exit, it will be interpreted as 
    	        // on entry
    			mReminder.setEvent(getReminderEvent(mEntry, mExit));
       	        
    			// If mannual addr mode then use the addr that is in the text box
    			if(mAddrSource == AddrSource.MANUAL){
    				mReminder.setAddr(mAddrText.getText().toString());
    			}
    			// if "current addr" mode is selected, use the lat/lon of current loc
    			// at the time the "Save" button is clicked. Translation of gps into address
    			// is done in the verifyAndSave thread
    			if(mAddrSource == AddrSource.CURRENT){
                	// set the lat/lon for the reminder 
                	mReminder.setLat(myLocation.getLatitude());
                	mReminder.setLon(myLocation.getLongitude());
                	// set the addr field to my current loc and
                	// do the geocoding later in the VerifyAndSave thread
    			}
    			
    	    	// create thread to verify and save results
    	        Thread thread =  new Thread(null, new VerifyAndSave(mReminder), "Background Thread");
    	        // start background thread
    	        thread.start();
    	        // show progress dialog while the results are being verified and saved
    	        m_ProgressDialog = ProgressDialog.show(this,    
    	              getString(R.string.rem_progress_dialog_title), 
    	              getString(R.string.rem_progress_dialog_body), 
    	              true);
    	        
            	break;

    		case CANCEL:
    			setResult(RESULT_CANCELED);
    			finish();
    			break;
    			
    		case DELETE:    		
    			if( mRowId != null){
    				mDbHelper.deleteReminder(mRowId);
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
    	
    	private Reminder rem;
    	private ResultCode result;
    	
      	public VerifyAndSave(Reminder r){
      		this.rem = r;
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
		  		if(isAddrChanged() && mAddrSource == AddrSource.MANUAL){		  			
		  			GpsPoint p = geoCodec.getGpsPointFromAddress(mAddrText.getText().toString());
		  			// if addr is invalid do not save reminder
		  			// and update pass/fail status
		  			if(p == null){
		  				result = ResultCode.INVALID_ADDR;
		  				runOnUiThread(new ShowStatus(result));
		  				return;	  			
		  			}else{
		  				// if addr is valid set the lat/long coordinates
		  				rem.setLat(p.getLat());
		  				rem.setLon(p.getLon());
		  			}
		  		}
		  		
		  		if(!isAddrChanged() && mAddrSource == AddrSource.MANUAL){		  			
		  			GpsPoint p = geoCodec.getGpsPointFromAddress(mAddrText.getText().toString());
		  			// if addr is invalid do not save reminder
		  			// and update pass/fail status
		  			if(p == null){
		  				result = ResultCode.INVALID_ADDR;
		  				runOnUiThread(new ShowStatus(result));
		  				return;	  			
		  			}else{
		  				// if addr is valid set the lat/long coordinates
		  				rem.setLat(p.getLat());
		  				rem.setLon(p.getLon());
		  			}
		  		}
		  		
		  		if(mAddrSource == AddrSource.CURRENT){
		  			Log.e(TAG, "Addr source current");
		  			// reverse geocode gps coordinates into address
		  			// the lat/lon is already populated by the performAction method
		  			GpsPoint p = new GpsPoint(rem.getLat(), rem.getLon());
		  			
		  			Log.e(TAG, "Current GPS point: " + p.getPointAsString());
		  			
		  			String address = geoCodec.getAddressFromGpsPoint(p);
		  			
		  			// if the gps points could not be correctly translated into
		  			// an address, use the gps coordinates for the address field.
		  			if(address.length() == 0){
		  				address = p.getPointAsString();
		  			}
		  			
		  			Log.e(TAG, "Current Address: " + address);
		  			// update the address attribute of reminder
		  			rem.setAddr(address);
		  		}
		  		
		  		// when AddrSource is BOOKMARKS, no geocoding is needed
		  		// since selecting a bookmarked location provides both
		  		// the address and gps coordinate of the location.
		  		
		  		// save to database
    	        if (mRowId == null) { // new reminder
    	        	long id = mDbHelper.createReminder(rem);
    	            if (id > 0) {
    	                // updated RowId of saved reminder so it
    	                // can be restored
    	            	mRowId = id; 
    	                result = ResultCode.SAVED;
    	            }else{
    	            	result = ResultCode.SAVE_ERROR;
    	            }
    	        } else { // update existing reminder
    	            if(mDbHelper.updateReminder(mRowId, rem)){
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
    		
    		AlertDialog.Builder builder = new AlertDialog.Builder(ReminderEdit.this);
    		
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
    			ReminderEdit.this.setResult(RESULT_OK);
    			ReminderEdit.this.finish();
        	}
        	
        	
        };
    };
    
	// returns the currently set value of the reminder state radio button 
    private int getReminderState(){
    	
    	Reminder.State state = Reminder.State.ENABLED;
    	
    	if(mEnable.isChecked()){
    		state = Reminder.State.ENABLED;   	
    	}else{
    		state = Reminder.State.DISABLED;
    	} 
    	
    	return state.ordinal();
    }
    
    // sets the enabled/disabled reminder state of the radio button
    private void setReminderState(int state){
    	
    	Reminder.State state_type = Reminder.State.values()[state];
    	
    	switch(state_type){
    		case ENABLED:
    			mEnable.setChecked(true);
    			break;
    		case DISABLED:
    			mDisable.setChecked(true);
    			break;
    	}	
   		
    }
    
    private int getReminderAlert(){
    	Reminder.Alert alert = Reminder.Alert.NNN;
    	    	
    	if(mRing.isChecked() && mVibrate.isChecked() && mFlash.isChecked()){
    		alert = Reminder.Alert.RIN_VIB_FLS;
    	}
    	if(mRing.isChecked() && mVibrate.isChecked() && !mFlash.isChecked()){
    		alert = Reminder.Alert.RIN_VIB;
       	}
    	if(mRing.isChecked() && !mVibrate.isChecked() && mFlash.isChecked()){
    		alert = Reminder.Alert.RIN_FLS;
    	}
    	if(!mRing.isChecked() && mVibrate.isChecked() && mFlash.isChecked()){
    		alert = Reminder.Alert.VIB_FLS;
    	}
    	if(mRing.isChecked() && !mVibrate.isChecked() && !mFlash.isChecked()){
    		alert = Reminder.Alert.RIN;
       	}
    	if(!mRing.isChecked() && mVibrate.isChecked() && !mFlash.isChecked()){
    		alert = Reminder.Alert.VIB;
    	}
    	if(!mRing.isChecked() && !mVibrate.isChecked() && mFlash.isChecked()){
    		alert = Reminder.Alert.FLS;
    	}
    
    	return alert.ordinal();	
    }
    
    private void setReminderAlert(int alert){
    	
    	Reminder.Alert alert_type = Reminder.Alert.values()[alert];
    	    	    	
    	switch( alert_type ){
    	// NOTE: fully qualified enum names are not needed in case labels
    	// below since object type is inferred from that of "alert_type"
    	// in the switch statement above. 
    	// See http://elliottback.com/wp/enumerated-constants-in-java/
    		case RIN:
    			mRing.setChecked(true);
    			mVibrate.setChecked(false);
    			mFlash.setChecked(false);
    			break;
    		case VIB:
    			mRing.setChecked(false);
    			mVibrate.setChecked(true);
    			mFlash.setChecked(false);
    			break;
    		case FLS:
    			mRing.setChecked(false);
    			mVibrate.setChecked(false);
    			mFlash.setChecked(true);
    			break;
    		case RIN_VIB:
    			mRing.setChecked(true);
    			mVibrate.setChecked(true);
    			mFlash.setChecked(false);
    			break;
    		case VIB_FLS:
    			mRing.setChecked(false);
    			mVibrate.setChecked(true);
    			mFlash.setChecked(true);
    			break;
    		case RIN_FLS:
    			mRing.setChecked(true);
    			mVibrate.setChecked(false);
    			mFlash.setChecked(true);
    			break;
    		case RIN_VIB_FLS:
    			mRing.setChecked(true);
    			mVibrate.setChecked(true);
    			mFlash.setChecked(true);
    			break;	
    		default:
    	 		break;
    	}
    }
    

    private void setReminderEvent(int event) {
		
    	//Log.e(TAG, "getReminderEvent event is " + event );
    	
    	Reminder.Event event_type = Reminder.Event.values()[event];
    	
    	switch(event_type){
    		
	    	case ON_ENTRY:
	    		mEntry.setChecked(true);
	    		mExit.setChecked(false);
	    		//Log.e(TAG, "setting on entry");
	    		break;
			case ON_EXIT:
				mEntry.setChecked(false);
				mExit.setChecked(true);
				//Log.e(TAG, "setting on exit");
				break;
			case ON_ENTRY_EXIT:
				mEntry.setChecked(true);
				mExit.setChecked(true);
				//Log.e(TAG, "setting on entry and exit");
				break;
			default:
    	        // NOTE: if user unchecks both entry and exit, it will be interpreted as 
    	        // on entry
				mEntry.setChecked(true);
				mExit.setChecked(false);
				//Log.e(TAG, "setting default");
				break;
    	}
		
	}
    
    private int getReminderEvent(CheckBox entry, CheckBox exit) {
		
    	Reminder.Event event = Reminder.Event.ON_ENTRY;
    	
    	if(entry.isChecked() && exit.isChecked()){
    		event = Reminder.Event.ON_ENTRY_EXIT;
    	}
    	if(entry.isChecked() && !exit.isChecked()){
    		event = Reminder.Event.ON_ENTRY;
    	}
    	if(!entry.isChecked() && exit.isChecked()){
    		event = Reminder.Event.ON_EXIT;
    	}
    	
    	//Log.e(TAG, "getReminderEvent event is " + event.name() + " " + event.ordinal());
    	
    	return event.ordinal(); // ordinal returns integer position of enum constant
	}

}
