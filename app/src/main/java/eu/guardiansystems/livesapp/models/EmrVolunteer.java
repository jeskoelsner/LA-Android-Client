package eu.guardiansystems.livesapp.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class EmrVolunteer {
		@Expose
		private String email;
		@Expose
		private String clientId;
		@Expose
		private EmrLocation location;

		public EmrVolunteer() {
				//defaults
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

		public String getClientId() {
				return clientId;
		}

		public void setClientId(String clientId) {
				this.clientId = clientId;
		}

		public EmrLocation getLocation() {
				return location;
		}

		public void setLocation(EmrLocation location) {
				this.location = location;
		}

		public String toJson() {
				Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
				return gson.toJson(this);
		}

}
