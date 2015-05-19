package eu.guardiansystems.livesapp.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import java.util.ArrayList;
import java.util.HashMap;
import org.w3c.dom.Document;
import static eu.guardiansystems.livesapp.MqttApplication.APPLICATION;
import eu.guardiansystems.livesapp.R;
import eu.guardiansystems.livesapp.android.config.Base;
import eu.guardiansystems.livesapp.android.ui.FontHelper;
import eu.guardiansystems.livesapp.android.ui.GMapDirections;
import eu.guardiansystems.livesapp.android.ui.UnlockListener;
import eu.guardiansystems.livesapp.android.ui.UnlockView;
import eu.guardiansystems.livesapp.models.EmrCaseData;
import eu.guardiansystems.livesapp.models.EmrCaseData.SimplifiedVolunteer;
import eu.guardiansystems.livesapp.models.EmrLocation;
import eu.guardiansystems.livesapp.models.Shared;

public class MissionActivity extends FragmentActivity implements View.OnClickListener, UnlockListener, CompoundButton.OnCheckedChangeListener {

		private GoogleMap map;
		private GMapDirections mapd;
		private Document directions;
		private Polyline lastPolyline;
		private EmrCaseData activeCaseData;
		private ViewSwitcher flipBack;
		private TextView caseAddress;
		private TextView caseMeters;
		private TextView caseMetersHint;
		private TextView caseVolunteers;
		private Toast caseNote;
		private RelativeLayout unlockScreen;
		private RelativeLayout overlay;
		private UnlockView unlockView;
		private ToggleButton toggleZoom;
		private ToggleButton toggleVolunteers;
		private Chronometer caseTimer;
		private Button caseBack;
		private Button buttonHelp;
		private Button buttonArrived;
		private Resources resources;

		private Marker selfMarker;
		private Marker caseMarker;
		private HashMap<String, Marker> volunteerMarkers = new HashMap<String, Marker>();

		private MarkerOptions selfMarkerOption;
		private MarkerOptions caseMarkerOption;
		private MarkerOptions newVolunteerMarkerOption;

		private Projection mapProjection;

        private PowerManager.WakeLock wl;

		@Override
		protected void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);

				if (APPLICATION.missionActivity != null) {
						finish();
						return;
				}
				APPLICATION.setCurrentActivity(this, true);
				APPLICATION.DISPLAYED_CASE_ID = getIntent().getExtras().getString("CASEID");

				setContentView(R.layout.screen_mission);

				resources = getResources();

                Window window = getWindow();
				window.addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
                window.addFlags(LayoutParams.FLAG_TURN_SCREEN_ON);    
                window.addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD);
                window.addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED);

                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wl = pm.newWakeLock(PowerManager.ON_AFTER_RELEASE |PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE, "Livesapp");
                wl.acquire();

				// globally add 'droid sans' font again on samsung devices
				FontHelper fHelper = new FontHelper(this);
				fHelper.applyCustomFont((RelativeLayout) findViewById(R.id.missionRoot));

				// Get UI elements
				unlockScreen = (RelativeLayout) findViewById(R.id.unlockScreen);
				unlockView = (UnlockView) findViewById(R.id.unLocker);
				unlockView.setOnUnlockListener(this);

				overlay = (RelativeLayout) findViewById(R.id.overlay);
				overlay.setOnClickListener(this);

				flipBack = (ViewSwitcher) findViewById(R.id.flipBack);
				flipBack.setDisplayedChild(0);

				caseAddress = (TextView) findViewById(R.id.caseAddress);
				caseMeters = (TextView) findViewById(R.id.caseMeters);
				caseVolunteers = (TextView) findViewById(R.id.caseVolunteers);

				caseMetersHint = (TextView) findViewById(R.id.caseMetersHint);

				caseBack = (Button) findViewById(R.id.caseBack);
				caseTimer = (Chronometer) findViewById(R.id.caseTimer);
				toggleZoom = (ToggleButton) findViewById(R.id.toolbarAutozoom);
				toggleVolunteers = (ToggleButton) findViewById(R.id.toolbarVolunteers);

				toggleZoom.setChecked(true);
				toggleVolunteers.setChecked(true);

				toggleZoom.setOnCheckedChangeListener(this);
				toggleVolunteers.setOnCheckedChangeListener(this);

				//buttonHelp = (Button) findViewById(R.id.toolbarHelp);
				buttonArrived = (Button) findViewById(R.id.toolbarArrived);

				//buttonHelp.setOnClickListener(this);
				buttonArrived.setOnClickListener(this);
				caseBack.setOnClickListener(this);
				caseAddress.setOnClickListener(this);

				// Design Marker
				selfMarkerOption = new MarkerOptions();
				selfMarkerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.case_self_marker));
				caseMarkerOption = new MarkerOptions();
				caseMarkerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.case_target_marker));
				newVolunteerMarkerOption = new MarkerOptions();
				newVolunteerMarkerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.case_volunteer_marker));

				volunteerMarkers = new HashMap<String, Marker>();

				/*
				 * New map = ((MapFragment) getFragmentManager() .findFragmentById(R.id.map_fragment)) .getMap();
				 */
				//Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.map_fragment);
				//SupportMapFragment mapFragment = (SupportMapFragment) fragment;
				map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment)).getMap();

				mapd = new GMapDirections(this);
				directions = mapd.refreshDocument(
						APPLICATION.getCurrentUser().getCurrentLocation(),
						new LatLng(APPLICATION.getCaseById(APPLICATION.DISPLAYED_CASE_ID).getLocation_latitude(), APPLICATION.getCaseById(APPLICATION.DISPLAYED_CASE_ID).getLocation_longitude()),
						GMapDirections.MODE_WALKING);

				if (map != null) {
						map.setIndoorEnabled(true);
						map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
						map.getUiSettings().setMyLocationButtonEnabled(false);
						map.getUiSettings().setZoomControlsEnabled(false);
						map.getUiSettings().setAllGesturesEnabled(false);

						mapProjection = map.getProjection();

						updateSelf(APPLICATION.getCurrentUser().getCurrentLocation());
						updateCase();
				} else {
						//Maps not installed!
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
						startActivity(intent);

						finish();
				}
		}

		@Override
		protected void onNewIntent(Intent intent) {
				Base.log("MissionActivity: onNewIntent()");
				super.onNewIntent(intent);

				if (APPLICATION.DISPLAYED_CASE_ID.equals(intent.getExtras().getString("CASEID"))) {
						updateCase();
				} else {
						Base.log("MissionActivity: onNewIntent() - " + APPLICATION.DISPLAYED_CASE_ID + " ... " + intent.getExtras().getString("CASEID"));
				}
		}

		@Override
		protected void onDestroy() {
				Base.log("MissionActivity: onDestroy()");
				if (APPLICATION.missionActivity != null) {
						if (APPLICATION.missionActivity.equals(this)) {
								APPLICATION.missionActivity = null;
						}
                    Window window = getWindow();
                    window.clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
                    window.clearFlags(LayoutParams.FLAG_TURN_SCREEN_ON);
                    window.clearFlags(LayoutParams.FLAG_DISMISS_KEYGUARD);
                    window.clearFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED);
				}
				APPLICATION.changeLocationUpdates(false);
                wl.release();
				super.onDestroy();

		}

		@Override
		public void onUnlock() {
				Base.log("MissionActivity: onUnlock()");
				Animation fadeOut = new AlphaAnimation(1, 0);
				fadeOut.setInterpolator(new AccelerateInterpolator());
				fadeOut.setDuration(500);
				fadeOut.setAnimationListener(new Animation.AnimationListener() {
						public void onAnimationEnd(Animation anim) {
								unlockScreen.setVisibility(View.GONE);

								flipBack.setDisplayedChild(1);
								map.getUiSettings().setAllGesturesEnabled(true);

								APPLICATION.acceptCase(APPLICATION.DISPLAYED_CASE_ID);
								APPLICATION.changeLocationUpdates(true);
						}

						public void onAnimationRepeat(Animation anim) { /*
								 * DO NOTHING
								 */

						}

						public void onAnimationStart(Animation anim) { /*
								 * DO NOTHING
								 */

						}

				});
				unlockScreen.startAnimation(fadeOut);
		}

		/**
		 * Show case on the map
		 */
		private void updateCase() {
				Marker tmpMarker;
				Base.log("MissionActivity: updateCase() " + APPLICATION.DISPLAYED_CASE_ID);

				activeCaseData = APPLICATION.getCaseById(APPLICATION.DISPLAYED_CASE_ID);

				caseAddress.setText(activeCaseData.getAddress());
				caseNote = Toast.makeText(MissionActivity.this, activeCaseData.getNotes(), Toast.LENGTH_LONG);
				caseVolunteers.setText(
						String.valueOf(activeCaseData.getVolunteers().size()));

				caseTimer.setBase(activeCaseData.getCreated() + activeCaseData.getServer_offset());
				caseTimer.start();

				//modify case position
				if (caseMarker != null) {
						animateMarker(caseMarker, activeCaseData.getLocation(), false);
				} else {
						caseMarkerOption.position(activeCaseData.getLocation());
						caseMarker = map.addMarker(caseMarkerOption);
						caseMarker.setSnippet(activeCaseData.getId());
				}

				//modify volunteer positions
				for (SimplifiedVolunteer volunteer : activeCaseData.getVolunteers()) {
						if (volunteer.id.equals(APPLICATION.getCurrentUser().getId())) {
								return;
						}

						if (volunteerMarkers.containsKey(volunteer.id)) {
								animateMarker(volunteerMarkers.get(volunteer.id), volunteer.location, false);
						} else {
								//TODO check if marker gets updated after insertion
								newVolunteerMarkerOption.position(volunteer.location);
								tmpMarker = map.addMarker(newVolunteerMarkerOption);
								tmpMarker.setSnippet(volunteer.id);

								volunteerMarkers.put(volunteer.id, tmpMarker);
						}
				}

				updateZoom();
		}

		public void updateSelf(LatLng toPosition) {
            Base.log("Animate self: " + toPosition.toString());
				if (selfMarker != null) {
						animateMarker(selfMarker, toPosition, false);
				} else {
						selfMarkerOption.position(toPosition);
						selfMarker = map.addMarker(selfMarkerOption);
				}

		}

		public void animateMarker(final Marker marker, final LatLng toPosition,
				final boolean hideMarker) {

            Base.log("Animate marker: " + toPosition.toString());

				runOnUiThread(new Runnable() {

						public void run() {
								final Handler handler = new Handler(Looper.getMainLooper());

								final LatLng startLatLng = marker.getPosition();

								final long start = SystemClock.uptimeMillis();
								final long duration = 500;

								final Interpolator interpolator = new LinearInterpolator();

								handler.post(new Runnable() {
										@Override
										public void run() {
												long elapsed = SystemClock.uptimeMillis() - start;
												float t = interpolator.getInterpolation((float) elapsed
														/ duration);
												double lng = t * toPosition.longitude + (1 - t)
														* startLatLng.longitude;
												double lat = t * toPosition.latitude + (1 - t)
														* startLatLng.latitude;
												marker.setPosition(new LatLng(lat, lng));

												if (t < 1.0) {
														// Post again 16ms later.
														handler.postDelayed(this, 16);
												} else {
														if (hideMarker) {
																marker.setVisible(false);
														} else {
																marker.setVisible(true);
														}
												}
										}

								});
						}

				});

		}

		/*
		 * public void updateMarker() { Base.log("MissionActivity: updateMarker()");
		 * selfMarkerOption.position(APPLICATION.CURRENT_USER.getCurrentLocation()); caseMarkerOption.position(activeCaseData.getLocation());
		 *
		 * map.clear(); map.addMarker(selfMarkerOption); map.addMarker(caseMarkerOption);
		 *
		 * if (toggleVolunteers.isChecked()) { for (Marker volunteer : volunteerMarkers.values()) { volunteer.setPosition(null);
		 * map.addMarker(volunteer); } }
		 *
		 * //zoom in to fit markers updateZoom(); }
		 */
		private void updateZoom() {
				Base.log("MissionActivity: updateZoom()");
				if (toggleZoom.isChecked()) {

						Builder boundsBuilder = new LatLngBounds.Builder();

						boundsBuilder.include(selfMarker.getPosition());
						boundsBuilder.include(caseMarker.getPosition());

						if (toggleVolunteers.isChecked()) {
								for (Marker option : volunteerMarkers.values()) {
										boundsBuilder.include(option.getPosition());
								}
						}

						map.moveCamera(CameraUpdateFactory.newLatLngBounds(
								boundsBuilder.build(),
								this.getResources().getDisplayMetrics().widthPixels,
								this.getResources().getDisplayMetrics().heightPixels,
								100));
				}

				//calculate new distance to case
				updateDistance(activeCaseData);
		}

		public void updateDistance(EmrCaseData caseData) {
				Base.log("MissionActivity: updateDistance()");
				double distance = Shared.calculateDistance(
						caseData.getLocation().latitude,
						caseData.getLocation().longitude,
						APPLICATION.getCurrentUser().getCurrentLocation().latitude,
						APPLICATION.getCurrentUser().getCurrentLocation().longitude);
				if (distance > 10000) {
						caseMeters.setText("10+");
						caseMetersHint.setText(getString(R.string.case_distance_kilometers_full));
				} else if (distance > 1000) {
						distance /= 1000;
						caseMeters.setText(String.format("%.1f", distance));
						caseMetersHint.setText(getString(R.string.case_distance_kilometers_full));

				} else {
						caseMeters.setText(String.format("%d", (int) distance));
						caseMetersHint.setText(getString(R.string.case_distance_meters_full));
						if (distance < 50 && unlockScreen.getVisibility() == View.GONE) {
								//buttelp.setVisibility(View.GONE);
								buttonArrived.setVisibility(View.VISIBLE);
						}
				}
				updateRoute();
		}

		public void updateRoute() {
				if (lastPolyline != null) {
						lastPolyline.remove();
				}

				ArrayList<LatLng> directionPoint = mapd.getDirection(directions);
				PolylineOptions rectLine = new PolylineOptions().width(10).color(0xFF0A8CD2);

				for (int i = 0; i < directionPoint.size(); i++) {
						rectLine.add(directionPoint.get(i));
				}

				lastPolyline = map.addPolyline(rectLine);
		}

		public void closeCase() {
				AlertDialog.Builder builder = new AlertDialog.Builder(MissionActivity.this);
				builder.setIcon(android.R.drawable.ic_dialog_info);
				builder.setTitle(resources.getString(R.string.case_alert_title));
				builder.setMessage(resources.getString(R.string.case_alert_text_shutdown));
				builder.setPositiveButton(resources.getString(R.string.button_positive_ok), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
								finish();
						}

				});

				builder.create().show();
		}

		private void showHelp(boolean show) {
				Base.log("Help Overlay: " + getResources().getResourceEntryName(overlay.getId()));
				if (show) {
						overlay.setVisibility(View.VISIBLE);
				} else {
						overlay.setVisibility(View.GONE);
				}
		}

		public void onClick(View view) {
				int id = view.getId();
				Base.log("Clicked Element: " + getResources().getResourceEntryName(id));

				if (id == R.id.caseAddress) {
						caseNote.show();
				}
				if (id == R.id.caseBack) {
						if (unlockScreen.getVisibility() == View.GONE) {
								Toast.makeText(this, getString(R.string.bubble_backbutton_disabled), Toast.LENGTH_LONG).show();
						} else {
								finish();
						}
				}
				/*if (id == R.id.toolbarHelp) {
						Base.log("Toolbar Help Clicked");
						showHelp(true);
				}*/
				if (id == R.id.toolbarArrived) {
						APPLICATION.arrivedAtCase(APPLICATION.DISPLAYED_CASE_ID);

						buttonArrived.setEnabled(false);
						buttonArrived.setClickable(false);
				}
				if (id == R.id.overlay) {
						Base.log("Help Overlay Clicked");
						showHelp(false);
				}
		}

		public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
				int id = compoundButton.getId();

				if (id == R.id.toolbarAutozoom) {
						updateZoom();
				}
				if (id == R.id.toolbarVolunteers) {
						//updateMarker();
				}
		}

		private LatLng getLatLng(EmrLocation location) {
				return new LatLng(location.getLatitude(), location.getLongitude());
		}

		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
				if (unlockScreen.getVisibility() == View.GONE) {
						if (keyCode == KeyEvent.KEYCODE_BACK) {
								Toast.makeText(this, getString(R.string.bubble_backbutton_disabled), Toast.LENGTH_LONG).show();
								return false;
						} else if (keyCode == KeyEvent.KEYCODE_HOME) {
								Toast.makeText(this, getString(R.string.bubble_homebutton_disabled), Toast.LENGTH_LONG).show();
								return false;
						}
				}
				return super.onKeyDown(keyCode, event);

		}

}
