package com.app.iReminder;

import com.app.iReminder.R;
import com.app.lib.Reminder;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ReminderList extends ListActivity {
        
    private static enum Message{
    	CREATE_REMINDER, EDIT_REMINDER
    }
    
    private static final String TAG = "ReminderList";
    
    private ReminderDbAdapter mDbHelper;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reminder_list);
        setTitle(R.string.reminder_list);
        
        mDbHelper = new ReminderDbAdapter(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
        
        Button addReminder = (Button) findViewById(R.id.add_reminder);
        
        addReminder.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	createReminder();
            }
        });

    }
    
    private void fillData() {
        // open Db cursor if it's not already open
    	if(mDbHelper == null){
    		mDbHelper.open();
    	}
    	
        // This will be used by our SimpleCursorAdapter to bind fields in each row to
        // data from our cursor.  
        class ReminderViewBinder implements SimpleCursorAdapter.ViewBinder {

        	boolean retval = false;

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

            	int title_col = cursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_TITLE);
            	int addr_col = cursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_ADDR);
            	int state_col = cursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_STATE);
            	//int range_col = cursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_RADIUS);
            	//int event_col = cursor.getColumnIndexOrThrow(ReminderDbAdapter.KEY_EVENT);
            	
            	// refers to the text view corresponding to the columnIndex
            	TextView tv = (TextView) view;
            	
            	// bind fields in different columns to the appropriate textview associated with it

                // bind title column
                if( columnIndex == title_col){
                	tv.setText(/*getText(R.string.title_label) +*/ cursor.getString(title_col));
                	retval = true;
                }
                // bind address column
                if( columnIndex == addr_col){
                	tv.setText(
                			(String)getText(R.string.addr_label) + 
                			cursor.getString(addr_col));
                	retval = true;
                }
                
                /*
                // bind range col
                if( columnIndex == range_col){
                	tv.setText(
                			(String)getText(R.string.range_label) + 
                			Double.toString(cursor.getDouble(range_col)));
                	retval = true;
                }
                // bind event column
                if( columnIndex == event_col){
                	int event = cursor.getInt(event_col);
                	switch( event ){
                	case Reminder.ON_ENTRY:
                		tv.setText(
                				(String)getText(R.string.event_label) + 
                				(String)getText(R.string.on_entry_label));
                		break;
                	
                	case Reminder.ON_EXIT:
                		tv.setText(
                				(String)getText(R.string.event_label) + 
                				(String)getText(R.string.on_exit_label));
                		break;
                		
                	case Reminder.ON_ENTRY_EXIT:
                		tv.setText(
                				(String)getText(R.string.event_label) + 
                				(String)getText(R.string.on_entry_exit_label));
                    	break;                	
                	}
                	retval = true;
                }
                */
                
                // bind state col
            	if ( columnIndex == state_col) {
                    
                	int state = cursor.getInt(state_col);
                    
                	Reminder.State state_type = Reminder.State.values()[state];
                	
                    switch ( state_type ) {
                        case ENABLED:
                            tv.setTextColor(Color.GREEN);
                            tv.setText(
                            		(String)getText(R.string.state_label) +
                            		(String)getText(R.string.active));
                            retval = true;
                            break;
                        case DISABLED:
                            tv.setTextColor(Color.RED);
                            tv.setText(
                            		(String)getText(R.string.state_label) + 
                            		(String)getText(R.string.inactive));
                            retval = true;
                            break;
                        default:
                            tv.setTextColor(Color.WHITE);
                        	tv.setText(
                        			(String)getText(R.string.state_label) + 
                        			(String)getText(R.string.inactive));
                            retval = true;
                            break;
                    }
                }
               
                
                return retval;
            }
        }
    	
    	
    	// Get all of the rows from the database and create the item list
        Cursor reminderCursor = mDbHelper.fetchAllReminders();
        startManagingCursor(reminderCursor);
        
        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{
        		ReminderDbAdapter.KEY_TITLE, 
        		ReminderDbAdapter.KEY_ADDR,
        		//ReminderDbAdapter.KEY_RADIUS, 
        		//ReminderDbAdapter.KEY_EVENT, 
        		ReminderDbAdapter.KEY_STATE};
        
        // and an array of the fields we want to bind those fields to 
        int[] to = new int[]{ 
        		R.id.title, 
        		R.id.addr, 
        		//R.id.range, 
        		//R.id.event, 
        		R.id.state};
        
        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter reminders = 
        	    new SimpleCursorAdapter(this, R.layout.reminder_entry, reminderCursor, from, to);
        
        reminders.setViewBinder(new ReminderViewBinder());
        
        setListAdapter(reminders);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
    	
    	/*
    	
    	super.onPrepareOptionsMenu(menu);
    	
    	// TODO: implement logic to check whether reminder
    	// service is running. If it is running, remove the start
    	// reminder service menu item, otherwise, remove the stop
    	// reminder service menu item
    	
    	if(ReminderService.isRunning){
    		
  			menu.removeItem(R.id.start_rem_svc);
  			menu.add(R.id.stop_rem_svc);
    		    		
    	}else{
    		
    		menu.add(R.id.start_rem_svc);
    		menu.removeItem(R.id.stop_rem_svc);
    		
    	}
    	*/
    	return true;
    }
   
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.rem_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        
        case R.id.start_rem_svc:
            startReminderService();
            return true;
            
        case R.id.stop_rem_svc:
        	stopReminderService();
        	return true;
        	
        default:
            return super.onOptionsItemSelected(item);
        }
    }
        
	private void createReminder() {
        // TODO: fill in implementation
    	Intent i = new Intent(this, ReminderEdit.class);
    	startActivityForResult(i, Message.CREATE_REMINDER.ordinal());

    }

    private void startReminderService(){
    	// send intent to start reminder service
        Intent start_rem_svc = new Intent(this, ReminderService.class);        
        this.startService(start_rem_svc);
    }
    
    private void stopReminderService(){
        // send intent to stop reminder service
        Intent stop_rem_svc = new Intent(this, ReminderService.class);     	
    	this.stopService(stop_rem_svc);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.rem_context_menu, menu);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      
      switch (item.getItemId()) {
      
      case R.id.edit_reminder:
        editReminder(info.id);
        return true;
      
      case R.id.delete_reminder:
        deleteReminder(info.id);
        return true;
      
      default:
        return super.onContextItemSelected(item);
      }
    }
    
    private void deleteReminder(long id){
        mDbHelper.deleteReminder(id);
        fillData();
    }
    
    
    // Sends an intent to ReminderEdit activity to edit the note
    private void editReminder(long id){
        Intent i = new Intent(this, ReminderEdit.class);
        i.putExtra(ReminderDbAdapter.KEY_ROWID, id);
        startActivityForResult(i, Message.EDIT_REMINDER.ordinal());   
    }
    

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        editReminder(id);   
    }
    
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    	// this method is called just before onResume. Database handle needs
    	// to be opened here since it was closed by onPause.
    	if(mDbHelper == null){
    		mDbHelper.open();
    	}
    	fillData();
   	
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.i(TAG, "RESUMED");
     	if(mDbHelper == null){
     		mDbHelper.open();
     	}
     	fillData();
    }
    
    @Override
    protected void onDestroy(){
    	
    	super.onDestroy();
    	Log.i(TAG, "DESTROYED");
    	if(mDbHelper != null){
    		mDbHelper.close();
    	}
    }
    
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "PAUSED");
    	/*
        if(mDbHelper != null){
    		mDbHelper.close();
    	}
        */
    }
   
}
