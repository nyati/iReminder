package com.app.lib;

public class Reminder {

	private String title;
    private String body;
    private String addr;
    private int state; 
    private int alert; 
    private double range;
    private int event;
    private double lat;
    private double lon;
    
    // values of state field 
    public static enum State{
    	DISABLED,
    	ENABLED 
    	//Not boolean so snooze can also be added later
    }
    
    // values of alert field
    public static enum Alert { 
    	NNN, RIN, VIB, FLS, RIN_VIB, VIB_FLS, RIN_FLS, RIN_VIB_FLS    
    }    
   	
    // values of event field
    public static enum Event {    	
    	ON_ENTRY, 
    	ON_EXIT, 
    	ON_ENTRY_EXIT
    }
    
    // default range in miles for the reminders when user 
    // does not specify a range
    public static double DEFAULT_RANGE = 0.25; // approx. 0.25 miles
    
    // default alert type if none is specified
    public static Alert DEFAULT_ALERT = Alert.RIN;
    // default state type if none is specified
    public static State DEFAULT_STATE = State.ENABLED;
    // default event type if none is specified
    public static Event DEFAULT_EVENT = Event.ON_ENTRY;
    
	public Reminder() {
		// all fields are null		
	}
	
	public Reminder(String title, String body, String addr, int state,
			int alert, double range, int event, double lat, double lon) {
		super();
		this.title = title;
		this.body = body;
		this.addr = addr;
		this.state = state;
		this.alert = alert;
		this.range = range;
		this.event = event;
		this.lat = lat;
		this.lon = lon;
	}

	/**
	 * @return the lat
	 */
	public double getLat() {
		return lat;
	}

	/**
	 * @param lat the lat to set
	 */
	public void setLat(double lat) {
		this.lat = lat;
	}

	/**
	 * @return the lon
	 */
	public double getLon() {
		return lon;
	}

	/**
	 * @param lon the lon to set
	 */
	public void setLon(double lon) {
		this.lon = lon;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}


	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}


	/**
	 * @return the body
	 */
	public String getBody() {
		return body;
	}


	/**
	 * @param body the body to set
	 */
	public void setBody(String body) {
		this.body = body;
	}


	/**
	 * @return the addr
	 */
	public String getAddr() {
		return addr;
	}


	/**
	 * @param addr the addr to set
	 */
	public void setAddr(String addr) {
		this.addr = addr;
	}


	/**
	 * @return the state
	 */
	public int getState() {
		return state;
	}


	/**
	 * @param state the state to set
	 */
	public void setState(int state) {
		this.state = state;
	}


	/**
	 * @return the alert
	 */
	public int getAlert() {
		return alert;
	}


	/**
	 * @param alert the alert to set
	 */
	public void setAlert(int alert) {
		this.alert = alert;
	}


	/**
	 * @return the range
	 */
	public double getRange() {
		return range;
	}


	/**
	 * @param range the range to set
	 */
	public void setRange(double range) {
		this.range = range;
	}


	/**
	 * @return the event
	 */
	public int getEvent() {
		return event;
	}


	/**
	 * @param event the event to set
	 */
	public void setEvent(int event) {
		this.event = event;
	}

}
