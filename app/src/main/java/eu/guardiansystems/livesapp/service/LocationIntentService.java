package eu.guardiansystems.livesapp.service;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import static eu.guardiansystems.livesapp.MqttApplication.APPLICATION;
import eu.guardiansystems.livesapp.android.config.Base;
import eu.guardiansystems.livesapp.models.EmrLocation;
import eu.guardiansystems.livesapp.models.EmrUser;

public class LocationIntentService extends IntentService {

		public LocationIntentService() {
				super("LocationIntentService");
		}

		@Override
		protected void onHandleIntent(Intent intent) {
            Base.log("Location changed -> updating!");
				Location location = (Location) intent.getParcelableExtra(LocationServices.FusedLocationApi.KEY_LOCATION_CHANGED);
                EmrUser current = APPLICATION.getCurrentUser();

				if (location != null && current != null) {
                        //convert to emrlocation
						EmrLocation newLocation = newLocation(location);

						current.setCurrentLocation(location.getLatitude(), location.getLongitude());
						APPLICATION.setCurrentUser(current);

                        //send location update
						APPLICATION.updateLocation(newLocation);

						if (APPLICATION.missionActivity != null) {
								APPLICATION.missionActivity.updateSelf(new LatLng(location.getLatitude(), location.getLongitude()));
						}
				} else {
						Base.log("Null location changed????");
				}
		}

		private EmrLocation newLocation(Location location) {
				EmrLocation newLocation = new EmrLocation(location.getLatitude(), location.getLongitude());
				newLocation.setAltitude(location.getAltitude());
				newLocation.setTimestamp(location.getTime());
				newLocation.setProvider(location.getProvider());
				return newLocation;
		}

}
