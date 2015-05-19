package eu.guardiansystems.livesapp.models;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.rits.cloning.Cloner;
import java.util.ArrayList;
import java.util.List;

public class EmrCaseData {
		public class SimplifiedVolunteer {
				public SimplifiedVolunteer(String id, String latitude, String longitude, String timestamp) {
						this.id = id;
						this.location = new LatLng(Double.valueOf(latitude), Double.valueOf(longitude));
						this.timestamp = Double.valueOf(timestamp);
				}

				public String id;
				public LatLng location;
				public double timestamp;
		}

		@Expose
		private String id;
		@Expose
		private String address;
		@Expose
		private String notes;
		@Expose
		private String dispatcher_id;
		@Expose
		private LatLng location;
		@Expose
		private double location_latitude;
		@Expose
		private double location_longitude;

		@Expose
		private List<String> volunteers_active;
		@Expose
		private List<String> volunteers_notified;
		@Expose
		private String initializer_name;
		@Expose
		private String initializer_phone;
		@Expose
		private String volunteer_final;
		@Expose
		private long created;
		@Expose
		private long ended;

		//only in this model!
		private List<SimplifiedVolunteer> volunteers;
		private long server_offset;

		public String getAddress() {
				return address;
		}

		public void setAddress(String address) {
				this.address = address;
		}

		public long getCreated() {
				return created;
		}

		public void setCreated(long created) {
				this.created = created;
		}

		public String getDispatcher_id() {
				return dispatcher_id;
		}

		public void setDispatcher_id(String dispatcher_id) {
				this.dispatcher_id = dispatcher_id;
		}

		public long getEnded() {
				return ended;
		}

		public void setEnded(long ended) {
				this.ended = ended;
		}

		public String getId() {
				return id;
		}

		public void setId(String id) {
				this.id = id;
		}

		public String getInitializer_name() {
				return initializer_name;
		}

		public void setInitializer_name(String initializer_name) {
				this.initializer_name = initializer_name;
		}

		public String getInitializer_phone() {
				return initializer_phone;
		}

		public void setInitializer_phone(String initializer_phone) {
				this.initializer_phone = initializer_phone;
		}

		public LatLng getLocation() {
				if (location == null) {
						setLocation(new LatLng(location_latitude, location_longitude));
				}
				return location;
		}

		public void setLocation(LatLng location) {
				this.location = location;
		}

		public double getLocation_latitude() {
				return location_latitude;
		}

		public void setLocation_latitude(long location_latitude) {
				this.location_latitude = location_latitude;
		}

		public double getLocation_longitude() {
				return location_longitude;
		}

		public void setLocation_longitude(long location_longitude) {
				this.location_longitude = location_longitude;
		}

		public String getNotes() {
				return notes;
		}

		public void setNotes(String notes) {
				this.notes = notes;
		}

		public long getServer_offset() {
				return server_offset;
		}

		public void setServer_offset(long server_offset) {
				this.server_offset = server_offset;
		}

		/*
		 * returning dummy instance NOT INCLUDING notifiedUserList & websocketList
		 */
		public EmrCaseData getTrimmedCaseData() {
				Cloner cloner = new Cloner();
				EmrCaseData trimmed = cloner.deepClone(this);
				trimmed.setInitializer_name(null);
				trimmed.setInitializer_phone(null);
				trimmed.setVolunteers_active(null);
				trimmed.setVolunteers_notified(null);
				trimmed.setServer_offset(0);
				trimmed.setCreated(0);
				return trimmed;
		}

		/*
		 * Constructor definition... no param constructer not allowed for public access !
		 */
		private EmrCaseData() {
		}

		public List<SimplifiedVolunteer> getVolunteers() {
				if (volunteers == null) {
						List<SimplifiedVolunteer> all_volunteers = new ArrayList<SimplifiedVolunteer>();
						if (volunteers_active != null) {
								for (String volunteer : volunteers_active) {
										String[] vol = volunteer.split(":");
										all_volunteers.add(new SimplifiedVolunteer(vol[0], vol[1], vol[2], vol[3]));
								}
						}
						setVolunteer(all_volunteers);
				}
				return volunteers;
		}

		public void setVolunteer(List<SimplifiedVolunteer> volunteers) {
				this.volunteers = volunteers;
		}

		public String getVolunteer_final() {
				return volunteer_final;
		}

		public void setVolunteer_final(String volunteer_final) {
				this.volunteer_final = volunteer_final;
		}

		public List<String> getVolunteers_active() {
				return volunteers_active;
		}

		public void setVolunteers_active(List<String> volunteers_active) {
				this.volunteers_active = volunteers_active;
		}

		public List<String> getVolunteers_notified() {
				return volunteers_notified;
		}

		public void setVolunteers_notified(List<String> volunteers_notified) {
				this.volunteers_notified = volunteers_notified;
		}

		public String toJson() {
				Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
				return gson.toJson(this);
		}

}
