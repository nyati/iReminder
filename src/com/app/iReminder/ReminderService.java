package com.app.iReminder;

import com.app.iReminder.R;
import com.app.lib.GeoDist;
import com.app.lib.GpsPoint;
import com.app.lib.Reminder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class ReminderService extends Service {

	private NotificationManager mNM;
    private LocationManager lm;
    private LocationListener locationListener;
    private ReminderDbAdapter mDbHelper;
    private GeoDist mGeoDist;
    private Location mLastLoc;
 
    /**
     * true of the reminder service process is running
     */
    public static boolean isRunning;
    
    private static enum Id{
    	REM_SVC_START_ID,
    	REM_SVC_STOP_ID,
        /** 
         * integer constant representing what number
         * the alert ids will start from to prevent overlap 
         * with previous 2 IDs.
         */
    	ALERT_START_ID
    }    
    
    /**
     * min distance interval (in meters) to update GPS location coordinates
     */
    public static final float DIST_INTERVAL = 0;    
    /**
     * min time (in milliseconds) to update GPS location coordinates
     */
    public static final long TIME_INTERVAL = 10000;    
    /**
     * Identifier used to uniquely identify reminder notifications
     */
    private int alert_id;
    
    private static final String TAG = "ReminderService";
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class ReminderBinder extends Binder {
        ReminderService getService() {
            return ReminderService.this;
        }
    }

    @Override
    public void onCreate() {
        
    	Log.i(TAG, " service on create called");
        mDbHelper = new ReminderDbAdapter(this);
        mDbHelper.open();
        
        mGeoDist = new GeoDist();
        mLastLoc = null;
        
    	mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        //---use the LocationManager class to obtain GPS locations---
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);    
        
        locationListener = new MyLocationListener();
    }

    
    // This is the object that receives interactions from clients. 
    private final IBinder mBinder = new ReminderBinder();
    
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}

	// called when reminder service is started
	@Override
	public void onStart(Intent intent, int startId) {
	    handleCommand(intent);
	    Log.i(TAG, "Received start id " + startId + ": " + intent);
	    System.out.println("Reminder Service *********");
	    
	}

	/* works on 2.0 and above only
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        handleStartCommand(intent)
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
	*/
	
	// handles the intent received to start reminder service and initiates
	// background services
	void handleCommand(Intent intent) {
        lm.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 
                TIME_INTERVAL, // time between location updates
                DIST_INTERVAL, // min distance between location updates
                locationListener); 
            
        // Display a notification about reminder service starting. We put an icon in the status bar.
        showServiceNotification(Id.REM_SVC_START_ID.ordinal());
                    
        isRunning = true;
        alert_id = Id.ALERT_START_ID.ordinal();
	}
    
    @Override
    public void onDestroy() {
        //stop listening for GPS updates 
        lm.removeUpdates(locationListener);
        
        //mReminderCursor.close();
        mDbHelper.close();
        
        // Tell the user we stopped.
        showServiceNotification(Id.REM_SVC_STOP_ID.ordinal());
        Toast.makeText(this, R.string.reminder_service_stopped, Toast.LENGTH_SHORT).show();

        // cancel all notifications from status bar
        mNM.cancelAll();
        
        isRunning = false;
    }
    
    /**
     * Show a notification while this service is running.
     */
    private void showServiceNotification(int id) {
   	
    	Log.e(TAG, "service ID: " + id);
    	
    	 // Set the icon, scrolling text and time stamp
    	Notification notification = new Notification(R.drawable.sun, "",
                System.currentTimeMillis());
    	// The PendingIntent to launch our activity if the user selects this notification
    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ReminderList.class), 0);
    	
    	Id svc_id = Id.values()[id];
    	    	
    	switch(svc_id){
    	
    		case REM_SVC_START_ID:
    	    	
    	        // Cancel the persistent notification.
    	        mNM.cancel(R.string.reminder_service_stopped);
    			// we'll use the same text for the ticker and the expanded notification
    	        notification.tickerText = getText(R.string.reminder_service_started);
    	        // Set the info for the views that show in the notification panel.
    	        notification.setLatestEventInfo(this, 
    	        		getText(R.string.reminder_service_label),
    	        		getText(R.string.reminder_service_started), 
    	        		contentIntent);
   			
    	        // Reminder service is on-going background service.
    	        notification.flags |= Notification.FLAG_ONGOING_EVENT;
    	        
    	        // do not clear notification if the user clicks on clear all
    	        // unless service is stopped
    	        notification.flags |= Notification.FLAG_NO_CLEAR;
    	        
    	        // only avcailable for API level 5 and higher.
    	        //notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
    	        
    	        Log.e(TAG, "Start ticker text: " + notification.tickerText);
    	        break;
    	        
    	        
    		case REM_SVC_STOP_ID:
    			
    	        // Cancel the persistent notification.
    	        mNM.cancel(R.string.reminder_service_started);
    	    	// we'll use the same text for the ticker and the expanded notification
    	        notification.tickerText = getText(R.string.reminder_service_stopped);
    	        // Set the info for the views that show in the notification panel.
    	        notification.setLatestEventInfo(this, 
    	        		getText(R.string.reminder_service_label),
    	        		getText(R.string.reminder_service_stopped), 
    	        		contentIntent);
    	        
    	        Log.e(TAG, "Stop ticker text: " + notification.tickerText);
    	        
    	    	// auto cancel when user clicks on the notification   	
    	    	notification.flags |= Notification.FLAG_AUTO_CANCEL;
    	        
    	        break;
    	}    	

    	//notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
        
    	// Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify(id, notification);

    }

    // alert_type is the type of alert notification the user elected to receive
    // for the particular reminder
    // this methods sets up the notification 
    private Notification createReminderNotification(Cursor reminderCursor){
    	
    	String title = reminderCursor.getString(
    			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_TITLE));
    	
    	String body = reminderCursor.getString(
    			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_BODY));
    	
    	long rowId = reminderCursor.getLong(
    			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_ROWID));
    	
    	
    	Intent notificationIntent = new Intent(this, ReminderEdit.class);
    	notificationIntent.putExtra(ReminderDbAdapter.KEY_ROWID, rowId);
    	// NOTE: Each PendingIntent must have a unique request_code (alert_id)
    	// so that it is not overwritten when multiple notifications
    	// pop up at the same time.
    	PendingIntent contentIntent = PendingIntent.getActivity(
    			this, alert_id, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);

    	CharSequence tickerText = title;  
    		//getResources().getString(R.string.new_reminder); //ticker-text
    	CharSequence contentTitle = title; // expanded message title
    	CharSequence contentText = body; // expanded message text
    	long when = System.currentTimeMillis();         // notification time
    	Context context = getApplicationContext();      // application Context
    	
    	// the next two lines initialize the Notification, using the configurations above
    	Notification notification = new Notification(R.drawable.bulb, tickerText, when);
    	notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

    //	 custom reminder notification layout
    	notification.contentIntent = contentIntent;
    	RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.reminder_notification);
    	contentView.setImageViewResource(R.id.notify_image, R.drawable.redpin_rem);
    	contentView.setTextViewText(R.id.notify_text, tickerText);
		notification.contentView = contentView;
    	
    	
    	return notification;
    	
    }
    
    /* Sends a audio/visual/sensory alert to the user based on his reminder
     * notification preferences for each reminder is triggered
     * 
     */
    private void alertUser(Cursor reminderCursor, Notification notification){

    	// get alert type from the cursor
    	int alert = reminderCursor.getInt(
    			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_ALERT));

  	
    	Reminder.Alert alert_type = Reminder.Alert.values()[alert];
    	
    	switch( alert_type ){
		
			case RIN:
				notification = setupRing(notification);
				break;
			case VIB:
				notification = setupVibrate(notification);
				break;
			case FLS:
				notification = setupFlash(notification);
				break;
			case RIN_VIB:
				notification = setupRing(notification);
				notification = setupVibrate(notification);
				break;
			case VIB_FLS:
				notification = setupVibrate(notification);
				notification = setupFlash(notification);
				break;
			case RIN_FLS:
				notification = setupVibrate(notification);
				notification = setupFlash(notification);
				break;
			case RIN_VIB_FLS:
				notification = setupRing(notification);
				notification = setupVibrate(notification);
				notification = setupFlash(notification);
				break;	
			default:
		 		break;
    	}
    	
    	// auto cancel when user clicks on the notification   	
    	notification.flags |= Notification.FLAG_AUTO_CANCEL;
    	
    	// keep reminding until user responds
    	notification.flags |= Notification.FLAG_INSISTENT;
    	
    	// NOTE: Here "alert_id" is a unique id, since every notification must 
    	// be pop up as a separate one. 
    	mNM.notify(alert_id, notification);
    	alert_id += 1;
    }
    
    private Notification setupRing(Notification notification){
    	notification.sound = Uri.parse("android.resource://com.app.iReminder/" + R.raw.beep_jazz);
		// keep ringing until the user opens notification window or cancels 
    	// notification
		//notification.flags |= Notification.FLAG_INSISTENT;		
		return notification;
    }
    private Notification setupVibrate(Notification notification){
    	notification.defaults |= Notification.DEFAULT_VIBRATE;
    	return notification;
    }
    private Notification setupFlash(Notification notification){
		//notification.defaults |= Notification.DEFAULT_LIGHTS;
		
    	notification.ledARGB = 0xff00ff00;
		notification.ledOnMS = 1000;  //led on for 1 sec
		notification.ledOffMS = 500; // led off for 0.5 sec
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
	
		return notification;
    }
    
    /**
     * Checks to see if user is in proximity of the reminder range
     * @param reminderCursor
     */
    private boolean inProximity(Location myLoc, Cursor reminderCursor){
    	
    	//get specified radius from database
    	double radius = reminderCursor.getDouble(
    			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_RADIUS));; //get from reminder cursor
    	
    	// lat of reminder
    	double lat = Double.parseDouble(reminderCursor.getString(
    	    			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_LAT)));
    	
    	System.out.println("Latitude : " + reminderCursor.getString(
    			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_LAT)));
    	
    	System.out.println("Longitude : " + reminderCursor.getString(
    			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_LNG)));
    	
    	// lon of reminder	
    	double lon = Double.parseDouble(reminderCursor.getString(
    			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_LNG)));	
    	
    	System.out.println("Lat : " +lat);
    	System.out.println("lon: " + lon);
    	//mLastLoc.setLatitude(lat);
    	//mLastLoc.setLongitude(lon);
    	
    	//NOTE: JFK exact GPS coordinate: 40.644412, -73.782745
    	
    	// for DEBUG only
    	String title = reminderCursor.getString(
    	    			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_TITLE));		
    			
    	// my current gps loc
    	GpsPoint myGpsLoc = new GpsPoint(myLoc.getLatitude(), myLoc.getLongitude());
    	
    	System.out.println("myLat : " +myLoc.getLatitude());
    	System.out.println("mylon: " + lon);
    	
    	// gps loc the reminder is set for
    	GpsPoint remGpsLoc = new GpsPoint(lat, lon);
    	
    	Log.e(TAG, "Reminder " + title + " Gps: " + remGpsLoc.getPointAsString());
    	Log.e(TAG, "Reminder " + title + " Gps lat: " + remGpsLoc.getLat() +  " GPS LON: " + remGpsLoc.getLon());
    	System.out.println("Reminder : " + remGpsLoc.getPointAsString());
    	
    	
    	//TESTING
    	
    	//remGpsLoc.setLon(-118.291);
    	//remGpsLoc.setLat(34.0319);
    	
    	//Log.e(TAG, "Reminder " + title + " Gps lat: " + remGpsLoc.getLat() +  " GPS LON: " + remGpsLoc.getLon());
    	
    	
    	// whether or not my curr gps loc is in range of rem's gps loc
    	return mGeoDist.inRange(myGpsLoc, remGpsLoc, radius);
    }
    
    /**
     * To determine entry/exit just need to store the gps coordinates
     * of the last visited location. If the current coordinate falls inside
     * the bounding box of a new reminder or outside the bounding boxes of any
     * reminders AND if the last location point did not, then we
     * have exited the location. Similarly, if the new loc point falls inside
     * of a bounding box for a new reminder, and the last one did not, we have
     * have entered the new location. Check DB to see if any reminders are set to
     * trigger based on entry/exit at that loc and show reminders.
     * Whether or not a point falls inside of a bounding box can be determined
     * based on the row_id field of the reminders returned when the database is queried
     * upon loc change.
     * @param myLoc
     * @param reminderCursor
     * @return
     */
    private boolean checkTriggerConditions(Location myLoc, Cursor reminderCursor){
    	
    	// initially, last loc is same as my current location
    	if(mLastLoc == null){ 
    		mLastLoc = myLoc;
		
    	}

    	    	
    	Log.e(TAG, "LastLoc: " + mLastLoc.getLatitude() + ", " + mLastLoc.getLongitude());
    	Log.e(TAG, "myLoc: " + myLoc.getLatitude() + ", " + myLoc.getLongitude());
    	
    	boolean lastloc_in_range = inProximity(mLastLoc, reminderCursor);
    	boolean currloc_in_range = inProximity(myLoc, reminderCursor);
    	
    	Log.e(TAG, "last loc in range?: " + lastloc_in_range);
    	Log.e(TAG, "curr loc in range?: " + currloc_in_range);
    	
    	int trigger_event = reminderCursor.getInt(
    			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_EVENT));// get from reminderCursor

    	// for DEBUG only
    	String title = reminderCursor.getString(
    			reminderCursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_TITLE));
    	
    	
    	//entry event
    	// last loc not in radius, current is
    	if( !lastloc_in_range && currloc_in_range ){
    		
    		Log.e(TAG, "Entry Event - " + title);
    		// trigger reminder if it is set for on_entry
    		return (trigger_event == Reminder.Event.ON_ENTRY.ordinal() ||
    				trigger_event == Reminder.Event.ON_ENTRY_EXIT.ordinal());
    	}    			
    			
    	//exit event
    	//last loc in radius, current not
    	if( lastloc_in_range && !currloc_in_range ){
    		
    		Log.e(TAG, "Exit Event" + title);
    		// trigger reminder of it is set for on_exit
    		return (trigger_event == Reminder.Event.ON_EXIT.ordinal() ||
    				trigger_event == Reminder.Event.ON_ENTRY_EXIT.ordinal());
    	}
    	
    	Log.e(TAG, "No Entry or Exit Events - " + title);
    	Log.e(TAG, "------------------------------------");
    	// when last loc and curr loc are both within range or both out of range
    	// ie, no entry or exit events 
    	return false;
    	
    }
    
    
    private class MyLocationListener implements LocationListener 
    {

    	@Override
        public void onLocationChanged(Location myLoc) {
            
    		// DEBUG CODE START    		
    		if(mLastLoc != null ){
    		   double radius = 0.01;	
    		   GpsPoint last_point = new GpsPoint(mLastLoc.getLatitude(), mLastLoc.getLongitude());
    		   GpsPoint curr_point = new GpsPoint(myLoc.getLatitude(), myLoc.getLongitude());
    		   if(!mGeoDist.inRange(last_point, curr_point, radius)){
    			   Log.e(TAG, "Moved more than " + radius + " meters");
    		   }
    		}
    		// DEBUG CODE END
    		
    		
        	// fetch all reminders which are enabled
            Cursor reminderCursor = mDbHelper.fetchEnabledReminders();
          	
            if(reminderCursor == null){
            	mLastLoc = myLoc;
            	// no matching reminders found, take no action
            	return;            
            }
            
            Log.e(TAG, "Check point 1");
            
            while(reminderCursor.moveToNext()){
            	// check if trigger conditions are met
            	if( checkTriggerConditions(myLoc, reminderCursor) ){            	
  	            	// create reminder notification
	            	Notification notification = createReminderNotification(reminderCursor);
	            	// alert user of reminder based on preferred alert type
	            	alertUser(reminderCursor, notification);
	            	
	            	Log.e(TAG, "Check point 2");
            	}
            }
            
            reminderCursor.close();
            
            // update my last loc.
            mLastLoc = myLoc;
           
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

    }
    
    
}
