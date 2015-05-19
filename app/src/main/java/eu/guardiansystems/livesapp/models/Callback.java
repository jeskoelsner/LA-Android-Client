package eu.guardiansystems.livesapp.models;

import com.google.gson.annotations.Expose;

public class Callback {
		@Expose
		public Error error;

		public class Error {
				@Expose
				public String type;
				@Expose
				public String message;
		}

		@Expose
		public EmrUser user;

		@Expose
		public EmrCaseData casedata;

		@Expose
		public Long time;
}
