package eu.guardiansystems.livesapp.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class EmrLocation {
		@Expose
		private String provider;
		@Expose
		private double latitude;
		@Expose
		private double longitude;
		@Expose
		private double altitude;
		@Expose
		private long timestamp;

		public EmrLocation(double latitude, double longitude) {
				this.latitude = latitude;
				this.longitude = longitude;
		}

		public EmrLocation() {
		}

		/*
		 * GETTERS AND SETTERS
		 */
		public double getLatitude() {
				return latitude;
		}

		public void setLatitude(double latitude) {
				this.latitude = latitude;
		}

		public double getLongitude() {
				return longitude;
		}

		public void setLongitude(double longitude) {
				this.longitude = longitude;
		}

		public double getAltitude() {
				return altitude;
		}

		public void setAltitude(double altitude) {
				this.altitude = altitude;
		}

		public String getProvider() {
				return provider;
		}

		public void setProvider(String provider) {
				this.provider = provider;
		}

		public long getTimestamp() {
				return timestamp;
		}

		public void setTimestamp(long timestamp) {
				this.timestamp = timestamp;
		}

		public String toJson() {
				Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
				return gson.toJson(this);
		}

		@Override
		public String toString() {
				return String.format("(locationData: %d, %d, %d, %s, %f, %f)", latitude, longitude, altitude, provider, timestamp);
		}

}
