package com.app.lib;

import java.lang.Math;


/**
 * Uses the Great Circle Distance Algorithm to calculate the approx surface distance
 * between GPS Coordinates. 
 * Great Circle Algorithm: http://en.wikipedia.org/wiki/Great-circle_distance
 * Code based on: http://www.math.montana.edu/frankw/ccp/cases/Global-Positioning/spherical-coordinates/learn.htm
 *  
 *
 */
public class GeoDist {
	
	/**
	 * The x-y-z Cartesian coordinate of a point in 3-d space
	 */
	private class CartesianCoordinate {

	    private double x; private double y; private double z;
		
	    public CartesianCoordinate(double x, double y, double z){
	    	this.x = x; this.y = y; this.z = z;
	    }	    
		public double getX() { return x; }
		public double getY() { return y; }
		public double getZ() { return z; }
		public void print(){ System.out.println("x: " + x + "y: " + y + "z: " + z); }
	}
	
	/**
	 * Radius of earth
	 */
	private static final double R = 6367000; 
	
	/**
	 * Conversion factor for meter to mile 
	 */
	private static final double METER2MILE = 0.0006214;
	
	public GeoDist() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Calculates the surface distance between 2 GPS coorindates
	 * @param line_dist
	 * @return 
	 */
	public double getSurfaceDistance(GpsPoint p1, GpsPoint p2){
		
		CartesianCoordinate c1 = getCartCoord(p1);
		CartesianCoordinate c2 = getCartCoord(p2);
		// straight line distance between 2 points in 3d space
		double line_dist = getLineDistance(c1, c2);
		
		//System.out.println("line distance: "+line_dist);
		
		//System.out.println("argument of asin: " + line_dist/(2*R));
		
		// converts straight line distance to earth's surface distance
		// approx that takes into account the curvature of the earth
		double result = (2*R * (Math.asin(line_dist/(2*R))));
		
		return meterToMile(result);
	}
	
	/**
	 * Rounds the given distance to the specified number of decimal digits
	 * @param num_to_round
	 * @param num_decimals
	 * @return
	 */
	public double roundDistance(double num_to_round, int num_decimals){
		double result = num_to_round;
		double factor =  Math.pow(10, num_decimals);
		result = result * factor;
		result = Math.round(result);
		result = result/factor;				
		return result;
	}
	
	/**
	 * Checks whether the 2 GPS Coordinates are within a certain radius
	 * of each other.
	 * @param p1 gps point 1
	 * @param p2 gps point 2
	 * @param radius radius (in miles) within which to check
	 * @return true if the 2 points are within radius, false otherwise
	 */
	public boolean inRange(GpsPoint p1, GpsPoint p2, double radius){
		
		return (getSurfaceDistance(p1,p2) <= radius);
	}

	/**
	 * Converts distance from meter to miles
	 * @param meters
	 * @return miles
	 */
	private double meterToMile(double meters){
		return meters*METER2MILE;
	}
	
	/**
	 * Converts GPS Coordinates into point coordinates in spherical
	 * Coordinate system
	 * @param p  GPS coordinate
	 * @return spherical coordinate
	 */
	private CartesianCoordinate getCartCoord(GpsPoint p){
		
		// get angles in degrees from lat/lon
		double phi = getPhi(p.getLat());
		double theta = getTheta(p.getLon());
		
		// convert angles to radian measures
		phi = getRadians(phi);
		theta = getRadians(theta);
		
		// get Cartesian coordinates
		double x = R * Math.cos(theta) * Math.sin(phi);
		double y = R * Math.sin(theta) * Math.sin(phi);
		double z = R * Math.cos(phi);
		CartesianCoordinate c = new CartesianCoordinate(x,y,z);
		
				
		//System.out.println("Cart Coord: ");
		
		//c.print();
		
		return c;
		
	}
	
	/**
	 * calculates the Euclidean distance between 2 points in the 3d cartesian coordinate
	 * system
	 * @param c1
	 * @param c2
	 * @return
	 */
	private double getLineDistance(CartesianCoordinate c1, CartesianCoordinate c2){
		
		double delta_x = c1.getX() - c2.getX();
		double delta_y = c1.getY() - c2.getY();
		double delta_z = c1.getZ() - c2.getZ();
				
		double line_dist =  Math.sqrt( 
								Math.pow(delta_x, 2) + 
								Math.pow(delta_y, 2) + 
								Math.pow(delta_z, 2) );
		
		//System.out.println("line distance: "+line_dist);
		
		return line_dist;
	}
	
	
	/**
	 * Converts lat in degrees into radians
	 * @param lat
	 * @return
	 */
	private double getPhi(double lat){
		
		double phi = 0;
		
		if(lat > 0){
			phi = 90 - lat;
		}else{
			phi = 90 + lat;
		}
		
		//System.out.println("phi: " + phi + "degrees");
		
		return phi;
	}
	
	/**
	 * Converts lon in degrees into radian
	 * @param lon
	 * @return
	 */
	private double getTheta(double lon){
		
		//System.out.println("theta: " + lon + "degrees");
		
		return lon;	
	}
	
	private double getRadians(double angle_in_degrees){
		
		double angle_in_radians = (angle_in_degrees * 2 * Math.PI)/360;
		
		//System.out.println("angle_in_radians: " + angle_in_radians);
		
		return angle_in_radians;
		
	}
	
}
