package eu.guardiansystems.livesapp.models;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import eu.guardiansystems.livesapp.android.config.Base;

public class EmrUser {

		@Expose
		private String id;
		@Expose
		private String email;
		@Expose(deserialize = false)
		private String password;
		@Expose
		private int level = -1;
		@Expose
		private String name;
		@Expose
		private String gender;
		@Expose
		private String mobile_number;

		@Expose
		private String zipcode;
		@Expose
		private String city;
		@Expose
		private String street;
		@Expose
		private String phone_number;
		@Expose
		private String house_number;
		@Expose
		private String country;
		@Expose
		private boolean receives_notifications;

		@Expose
		private long birthdate;
        @Expose
        private long created;
        @Expose
        private boolean active;
		@Expose
		private long active_time;
		@Expose
		private String active_dispatcher;
		@Expose
		private double current_location_latitude;
		@Expose
		private double current_location_longitude;
		@Expose
		private List<String> dispatchers_nearby;

		//ignored... not exposed
		private LatLng current_location;

		public EmrUser(String email, String password) {
				this.email = email;
				this.id = md5(email);
				this.password = password;
		}

		public String getActive_dispatcher() {
				return active_dispatcher;
		}

		public void setActive_dispatcher(String active_dispatcher) {
				this.active_dispatcher = active_dispatcher;
		}

		public double getActive_time() {
				return active_time;
		}

		public void setActive_time(int active_time) {
				this.active_time = active_time;
		}

		public String getPhoneNumber() {
				return phone_number;
		}

		public void setPhoneNumber(String phone_number) {
				this.phone_number = phone_number;
		}

		public String getHouseNumber() {
				return house_number;
		}

		public void setHouseNumber(String house_number) {
				this.house_number = house_number;
		}

		public boolean isReceivesNotifications() {
				return receives_notifications;
		}

		public void setReceivesNotifications(boolean receives_notifications) {
				this.receives_notifications = receives_notifications;
		}

		/*
		 * GETTERS AND SETTERS
		 */
		public String getEmail() {
				return email;
		}

		public void setEmail(String email) {
				this.email = email;
		}

		public String getPassword() {
				return password;
		}

		public void setPassword(String password) {
				this.password = password;
		}

		public String getId() {
				return id;
		}

		public void setId(String id) {
				this.id = id;
		}

		public String getName() {
				return name;
		}

		public void setName(String name) {
				this.name = name;
		}

		public String getGender() {
				return gender;
		}

		public void setGender(String gender) {
				this.gender = gender;
		}

		public String getMobileNumber() {
				return mobile_number;
		}

		public void setMobileNumber(String mobile_number) {
				this.mobile_number = mobile_number;
		}

		public String getStreet() {
				return street;
		}

		public void setStreet(String street) {
				this.street = street;
		}

		public String getCity() {
				return city;
		}

		public void setCity(String city) {
				this.city = city;
		}

		public String getZipcode() {
				return zipcode;
		}

		public void setZipcode(String zipcode) {
				this.zipcode = zipcode;
		}

		public String getCountry() {
				return country;
		}

		public void setCountry(String country) {
				this.country = country;
		}

		public long getBirthdate() {
				return birthdate;
		}

		public void setBirthdate(long birthdate) {
				this.birthdate = birthdate;
		}

		public double getCreated() {
				return created;
		}

		public void setCreated(int created) {
				this.created = created;
		}

		public int getLevel() {
				return level;
		}

		public void setLevel(int level) {
				this.level = level;
		}

		public LatLng getCurrentLocation() {
            return new LatLng(current_location_latitude, current_location_longitude);
		}

		public void setCurrentLocation(double lat, double lng) {
				this.current_location = new LatLng(lat, lng);
                this.current_location_latitude = lat;
                this.current_location_longitude = lng;
		}

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

		public List<String> getDispatcher() {
				return dispatchers_nearby;
		}

		public void setDispatcher(ArrayList<String> dispatchers_nearby) {
				this.dispatchers_nearby = dispatchers_nearby;
		}

		public void addDispatcher(String dispatcher_id) {
				this.dispatchers_nearby.add(dispatcher_id);
		}

		public void removeDispatcher(String dispatcher_id) {
				this.dispatchers_nearby.remove(dispatcher_id);
		}

		public String toJson() {
				Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
				return gson.toJson(this);
		}

		@Override
		public String toString() {
				return "[firstName=" + getName()
						+ ", email=" + getEmail()
						+ ", password=" + getPassword()
						+ ", birthdate=" + getBirthdate()
						+ ", gender=" + getGender()
						+ ", mobilePhone=" + getMobileNumber()
						+ ", street=" + getStreet()
						+ ", city=" + getCity()
						+ ", zipcode=" + getZipcode()
						+ ", country=" + getCountry()
						+ ", created=" + getCreated();
		}

		public String md5(String string) {
				try {
						// Create MD5 Hash
						MessageDigest digest = MessageDigest.getInstance("MD5");
						digest.update(string.getBytes());
						byte messageDigest[] = digest.digest();

						// Create Hex String
						StringBuilder hexString = new StringBuilder();
						for (int i = 0; i < messageDigest.length; i++) {
								String h = Integer.toHexString(0xFF & messageDigest[i]);
								while (h.length() < 2) {
										h = "0" + h;
								}
								hexString.append(h);
						}
						return hexString.toString();

				} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
				}
				return "";
		}


}
