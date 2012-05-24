package com.app.iReminder;

import com.app.iReminder.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

///*
public class Home extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        
        Button myReminders = (Button) findViewById(R.id.my_reminders);
        Button searchNearby = (Button) findViewById(R.id.search_nearby);
        Button bookmarkLoc = (Button) findViewById(R.id.loc_bookmarks);
        Button mapMe = (Button) findViewById(R.id.map_me);
   
		// Listener for above
        myReminders.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	Intent i = new Intent(Home.this, ReminderList.class);
            	startActivity(i);
            }
        });

		
        searchNearby.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	Intent i = new Intent(Home.this, SearchNearby.class);
            	startActivity(i);
            }
        });    
        
        bookmarkLoc.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	Intent i = new Intent(Home.this, BookmarkList.class);
            	startActivity(i);
            }
        });  
    
        mapMe.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
            	Intent i = new Intent(Home.this, MapMe.class);
            	startActivity(i);
            }
        }); 
    
    }  

}
