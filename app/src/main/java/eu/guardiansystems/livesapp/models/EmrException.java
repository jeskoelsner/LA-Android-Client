package eu.guardiansystems.livesapp.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class EmrException {
		@Expose
		public String exception;

		public String getException() {
				return exception;
		}

		public void setException(String exception) {
				this.exception = exception;
		}

		public String toJson() {
				Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
				return gson.toJson(this);
		}

}
