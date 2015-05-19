package eu.guardiansystems.livesapp.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class Shared {

		// LOCATION FIELDS
		public static final String LOCATION_LATITUDE = "latitude";
		public static final String LOCATION_LONGITUDE = "longitude";
		public static final String LOCATION_ALTITUDE = "altitude";
		public static final String LOCATION_TIMESTAMP = "timestamp";
		public static final String LOCATION_PROVIDER = "provider";

		public static final String CONTEXT = "context";
		public static final String RECEIVER = "receiver";
		public static final String COMMAND = "command";

		public final class Rest {
				public static final int DEFAULT_PORT = 80;
				public static final int TIMEOUT_MILLIS = 5000;
				public static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
				public static final String CONTENT_TYPE_URLENCODED = "application/x-www-form-urlencoded;charset=UTF-8";
		}


		/*
		 * Calculates the distance between 2 coordinates (lat/long) in meters
		 */
		public static double calculateDistance(double lat1, double long1, double lat2, double long2) {
				double earthRadius = 6371000; // in meters
				double dLat = Math.toRadians(lat2 - lat1);
				double dLng = Math.toRadians(long2 - long1);
				double sindLat = Math.sin(dLat / 2);
				double sindLng = Math.sin(dLng / 2);
				double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2) * Math.cos(lat1) * Math.cos(lat2);
				double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
				double dist = earthRadius * c;
				return dist;
		}

		/*
		 * Reads an InputStream and returns it as String
		 */
		public static String streamToString(InputStream is) throws IOException {
				BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				StringBuilder total = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
						total.append(line);
				}
				return total.toString();
		}

}
