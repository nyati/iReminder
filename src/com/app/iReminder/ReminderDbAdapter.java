
package com.app.iReminder;

import com.app.lib.Reminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the Reminder App, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 */
public class ReminderDbAdapter {

	public static final String KEY_ROWID = "_id";
	
    public static final String KEY_TITLE = "title";
    public static final String KEY_BODY = "body";
    public static final String KEY_LAT = "lat";
    public static final String KEY_LNG = "lng";
    public static final String KEY_ADDR = "address";
    public static final String KEY_STATE = "state";
    public static final String KEY_ALERT = "alert";
    public static final String KEY_RADIUS = "radius";
    public static final String KEY_EVENT = "event";
    
    //public static final String KEY_TIME = "time";

    private static final String TAG = "ReminderDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
        "create table reminders (_id integer primary key autoincrement, " + 
        "title text not null, " +
        "body text not null, " +
        "lat real, " +
        "lng real, " +
        "address text, " +
        "state integer, " +
        "alert integer, " +
        "radius real, " + 
        "event integer );";

    private static final String DATABASE_NAME = "remindersdb";
    private static final String DATABASE_TABLE = "reminders";
    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS reminders");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public ReminderDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public ReminderDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    
    /**
     * Create a new note using the title and body provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    public long createReminder(Reminder r) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, r.getTitle());
        initialValues.put(KEY_BODY, r.getBody());
        initialValues.put(KEY_ADDR, r.getAddr());
        initialValues.put(KEY_STATE, r.getState());
        initialValues.put(KEY_ALERT, r.getAlert());
        initialValues.put(KEY_RADIUS, r.getRange());
        initialValues.put(KEY_EVENT, r.getEvent());
    	initialValues.put(KEY_LAT, r.getLat());
    	initialValues.put(KEY_LNG, r.getLon());
    	
    	return mDb.insert(DATABASE_TABLE, null, initialValues);
    }
    
    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of note to update
     * @param title value to set note title to
     * @param body value to set note body to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateReminder(long rowId, Reminder r){
        ContentValues args = new ContentValues();
        args.put(KEY_TITLE, r.getTitle());
        args.put(KEY_BODY, r.getBody());
        args.put(KEY_LAT, r.getLat());
        args.put(KEY_LNG, r.getLon());
        args.put(KEY_ADDR, r.getAddr());
        args.put(KEY_STATE, r.getState());
        args.put(KEY_ALERT, r.getAlert());
        args.put(KEY_RADIUS, r.getRange());
        args.put(KEY_EVENT, r.getEvent());
    
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
        
    }
    
    /**
     * Delete the note with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteReminder(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllReminders() throws SQLException {

        return mDb.query(DATABASE_TABLE, 
        		new String[] { KEY_ROWID, KEY_TITLE, KEY_ADDR, 
        		KEY_STATE, KEY_ALERT, KEY_RADIUS, KEY_EVENT}, 
        		null, null, null, null, 
        		KEY_STATE + " DESC"); // show enabled reminder at top of list
    }

    
    /**
     * Return a Cursor over the list of all notes in the database over specified
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllReminders(String[] fields) throws SQLException {

        return mDb.query(DATABASE_TABLE, 
        		fields, 
        		null, null, null, null, 
        		KEY_STATE + " DESC"); // show enabled reminder at top of list
    }
    
    
    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchReminder(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_TITLE, KEY_BODY, KEY_LAT, KEY_LNG, KEY_ADDR, 
                    KEY_STATE, KEY_ALERT, KEY_RADIUS, KEY_EVENT}, 
                    KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        
        return mCursor;

    }

    public Cursor fetchEnabledReminders() throws SQLException {
    	
    	String[] select_cols = new String[]{KEY_ROWID, KEY_TITLE, KEY_BODY, 
    			KEY_ADDR, KEY_LAT, KEY_LNG, KEY_ALERT, KEY_RADIUS, KEY_EVENT};

    	String match_cond = KEY_STATE + " = " + Reminder.State.ENABLED.ordinal();

    	Cursor mCursor = 
    		
    		mDb.query(true, 
    				DATABASE_TABLE, 
    				select_cols, 
    				match_cond, 
    				null, null, null, null, null);
    	
        //if (mCursor != null) {
        //    mCursor.moveToFirst();
        //}    	
    	
    	return mCursor;	
    }

 
}
