package com.app.iReminder;

import com.app.lib.Bookmark;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class BookmarkDbAdapter {

	public static final String KEY_ROWID = "_id";	
    public static final String KEY_NAME = "name";
    public static final String KEY_LAT = "lat";
    public static final String KEY_LON = "lon";
    public static final String KEY_ADDR = "address";

    // Used when a row id must be passed in but itmust be invalid
    // Ex: used in addBoomark function of BookmarkList class
    public static final long DEFAULT_INVALID_ROWID = Long.MIN_VALUE; 
    
    private static final String TAG = "BookmarkDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
        "create table bookmarks (_id integer primary key autoincrement, " + 
        "name text not null, " +
        "lat real, " +
        "lon real, " +
        "address text " +
        ");";

    private static final String DATABASE_NAME = "bookmarksdb";
    private static final String DATABASE_TABLE = "bookmarks";
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
            db.execSQL("DROP TABLE IF EXISTS bookmarks");
            onCreate(db);
        }
    }
	
	
	public BookmarkDbAdapter(Context ctx) {
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
    public BookmarkDbAdapter open() throws SQLException {
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
    public long createBookmark(Bookmark bookmark) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NAME, bookmark.getName());
        initialValues.put(KEY_LAT, bookmark.getLat());
        initialValues.put(KEY_LON, bookmark.getLon());
        initialValues.put(KEY_ADDR, bookmark.getAddress());

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
    public boolean updateBookmark(long rowId, Bookmark b){
        ContentValues args = new ContentValues();
        args.put(KEY_NAME, b.getName());
        args.put(KEY_ADDR, b.getAddress());
        args.put(KEY_LAT, b.getLat());
        args.put(KEY_LON, b.getLon());
    
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
   
    }
   
    /**
     * Delete the note with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteBookmark(long rowId) {

        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchBookmark(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_NAME, KEY_ADDR, KEY_LAT, KEY_LON}, 
                    KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        
        return mCursor;

    }
    

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllBookmarks() throws SQLException {

        return mDb.query(DATABASE_TABLE, 
        		new String[] {KEY_ROWID, KEY_NAME, KEY_LAT, 
        		KEY_LON, KEY_ADDR}, 
        		null, null, null, null, null);
    }

    

    public Cursor fetchBookmarks(String[] select_cols, String match_cond) throws SQLException {
    	
    	if(select_cols.length <= 0){
    		select_cols = new String[]{KEY_ROWID, KEY_NAME, KEY_LAT, 
            		KEY_LON, KEY_ADDR};
    	}
    	
    	Cursor mCursor = 
    		
    		mDb.query(true, 
    				DATABASE_TABLE, 
    				select_cols, 
    				match_cond, 
    				null, null, null, null, null);
    	
    	//returns cursor positioned at -1
    	return mCursor;	
    }   
}

