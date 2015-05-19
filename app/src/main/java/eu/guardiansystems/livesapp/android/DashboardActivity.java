package eu.guardiansystems.livesapp.android;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import static eu.guardiansystems.livesapp.MqttApplication.APPLICATION;
import eu.guardiansystems.livesapp.R;
import eu.guardiansystems.livesapp.android.adapter.CaseAdapter;
import eu.guardiansystems.livesapp.android.adapter.NewsAdapter;
import eu.guardiansystems.livesapp.android.adapter.objects.ParcelableNewsObject;
import eu.guardiansystems.livesapp.android.config.Base;
import eu.guardiansystems.livesapp.android.config.SharedPrefs;
import eu.guardiansystems.livesapp.android.ui.ApplicationDialog;
import eu.guardiansystems.livesapp.android.ui.FontHelper;
import eu.guardiansystems.livesapp.models.EmrCaseData;
import eu.guardiansystems.livesapp.models.EmrLocation;
import eu.guardiansystems.livesapp.models.Shared;
import eu.guardiansystems.livesapp.service.RestService;
import eu.guardiansystems.livesapp.service.UpdateAppService;

public class DashboardActivity extends Activity implements OnClickListener, OnItemClickListener, OnCheckedChangeListener,
		OnScrollListener {

        private Context mContext;

        public static final int VIEW_NEWS = 0;
		public static final int VIEW_CASES = 1;

		private String version = "";
		private String newVersion;
		private String newUrl;

		private long lastTimestamp;
		private boolean downloading;
		private ProgressBar refreshNewsProgressBar;
		private Button refreshNewsButton;
		private TextView emptyNewsText;
		private ToggleButton viewToggleCases;
		private ToggleButton viewToggleStream;
		private ViewSwitcher viewSwitcher;
		private Button buttonSettings;
		private Button buttonLogout;
		private ToggleButton statusBox;
		private Animation animInFirst;
		private Animation animOutFirst;
		private Animation animInSecond;
		private Animation animOutSecond;
		private ImageView logoTitle;
		private ListView caseList;
		private ListView newsList;

		private EmrLocation location;
		private ApplicationDialog dialogHelper;
		private SharedPrefs sharedPrefs;

		private Dialog volumeDialog;
		private TextView volumeDialogText;
		private SeekBar volumeDialogSeek;
		private Button volumeDialogOk;

		private Dialog lengthDialog;
		private TextView lengthDialogText;
		private SeekBar lengthDialogSeek;
		private Button lengthDialogOk;

		ResultReceiver restResultReceiver = new ResultReceiver(new Handler()) {
				@Override
				protected void onReceiveResult(int resultCode, Bundle resultData) {
						switch (resultCode) {
								case RestService.STATUS_RUNNING:
										refreshNewsProgressBar.setVisibility(View.VISIBLE);
										downloading = true;
										break;
								case RestService.STATUS_ERROR:
										refreshNewsProgressBar.setVisibility(View.GONE);
										String error = resultData.getString(RestService.ERROR_MESSAGE);
										Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
										downloading = false;
										break;
								case RestService.STATUS_FINISHED:
										refreshNewsProgressBar.setVisibility(View.GONE);
										String result
												= resultData.containsKey(RestService.ACTIVITY_STREAM_CALLBACK)
														? resultData.getString(RestService.ACTIVITY_STREAM_CALLBACK)
														: resultData.getString(RestService.UPDATE_CALLBACK);

										Base.log("MSG: " + result);

										if (result != null) {
												try {
														JsonObject json = (JsonObject) new JsonParser().parse(result);

														if (json.has("version")) {
																newVersion = json.get("version").getAsString();
																newUrl = json.get("url").getAsString();
                                                            //TODO updater
														} else if (json.has("items")) {
																Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
																ParcelableNewsObject news[] = gson.fromJson(json.get("items"), ParcelableNewsObject[].class);
																if (news.length != 0) {
																		lastTimestamp = json.get("last_timestamp").getAsLong();

																		for (ParcelableNewsObject newsObject : news) {
																				APPLICATION.newsAdapter.add(newsObject);
																		}
																}
														} else {
																Toast.makeText(getApplicationContext(), "No News...", Toast.LENGTH_LONG).show();
														}
												} catch (Exception e) {
														Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
												}
										} else {
												Toast.makeText(getApplicationContext(), getString(R.string.bubble_result_null), Toast.LENGTH_LONG).show();
										}
										downloading = false;
										break;
						}
				}

		};

		private final DialogInterface.OnClickListener logoutDialogListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
						switch (id) {
								case DialogInterface.BUTTON_POSITIVE:
										dialog.dismiss();
										APPLICATION.logout();

										//go to login
										Intent intent = new Intent(mContext, LoginActivity.class);
										startActivity(intent);

										break;
								case DialogInterface.BUTTON_NEGATIVE:
										dialog.dismiss();
										break;
						}
				}

		};

		private final DialogInterface.OnClickListener visitHomepageDialogListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
						switch (id) {
								case DialogInterface.BUTTON_POSITIVE:
										dialog.dismiss();
										Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://livesapp.net"));
										startActivity(intent);
										break;
								case DialogInterface.BUTTON_NEGATIVE:
										dialog.dismiss();
										break;
						}
				}

		};

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				super.onCreateContextMenu(menu, v, menuInfo);
				MenuInflater inflater = getMenuInflater();
				switch (v.getId()) {
						case R.id.toolbarSettings:
								inflater.inflate(R.menu.menu_dashboard, menu);
								return;
						default:
								inflater.inflate(R.menu.menu_submenu, menu);
				}
		}

		@Override
		public boolean onPrepareOptionsMenu(Menu menu) {
				int option = sharedPrefs.getSoundOption();

				switch (option) {
						case 0:
								menu.findItem(R.id.menu_soundforce_type_alarm).setChecked(true);
								break;
						case 1:
								menu.findItem(R.id.menu_soundforce_type_vibrate).setChecked(true);
								break;
						case 2:
								menu.findItem(R.id.menu_soundforce_type_native).setChecked(true);
								break;
				}
				return super.onPrepareOptionsMenu(menu); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public boolean onContextItemSelected(MenuItem item) {
				AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
				switch (item.getItemId()) {
						case R.id.menu_soundforce_type:
								new Handler().postDelayed(new Runnable() {
										@Override
										public void run() {
												openContextMenu(findViewById(R.id.hidden));
										}

								}, 0);
								return true;
						case R.id.menu_soundforce_volume:
								volumeDialog.show();
								return true;
						case R.id.menu_soundforce_length:
								lengthDialog.show();
								return true;
						case R.id.menu_soundforce_type_alarm:
								sharedPrefs.setSoundOption(0);
								item.setChecked(true);
								return true;
						case R.id.menu_soundforce_type_vibrate:
								sharedPrefs.setSoundOption(1);
								item.setChecked(true);
								return true;
						case R.id.menu_soundforce_type_native:
								sharedPrefs.setSoundOption(2);
								item.setChecked(true);
								return true;
						default:
								return super.onContextItemSelected(item);
				}
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);

                mContext = this;

				if (APPLICATION.dashboardActivity != null) {
						APPLICATION.dashboardActivity.finish();
						return;
				}
				APPLICATION.setCurrentActivity(this, true);

				Base.log("~~~~~ DASHBOARD ONCREATE ~~~~~");
				setContentView(R.layout.screen_dashboard);

				/*
				 * try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch
				 * (PackageManager.NameNotFoundException ex) { Logger.getLogger(DashboardActivity.class.getName()).log(Level.SEVERE, null, ex); }
				 */
				// globally add 'droid sans' font again on samsung devices
				FontHelper fHelper = new FontHelper(this);
				fHelper.applyCustomFont((RelativeLayout) findViewById(R.id.dashboardRoot));

				sharedPrefs = new SharedPrefs(getApplicationContext());

				viewSwitcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);

				viewToggleCases = (ToggleButton) findViewById(R.id.toolbarCases);
				viewToggleCases.setOnClickListener(this);

				viewToggleStream = (ToggleButton) findViewById(R.id.toolbarStream);
				viewToggleStream.setOnClickListener(this);

				buttonSettings = (Button) findViewById(R.id.toolbarSettings);
				registerForContextMenu(buttonSettings);
				buttonSettings.setOnClickListener(this);

				buttonLogout = (Button) findViewById(R.id.toolbarLogout);
				buttonLogout.setOnClickListener(this);

				statusBox = (ToggleButton) findViewById(R.id.statusBox);
				//statusBox.setOnClickListener(this);
				statusBox.setOnCheckedChangeListener(this);
                statusBox.setChecked(APPLICATION.getCurrentUser().isActive());
				//statusBox.setChecked((APPLICATION.getCurrentUser().getLevel() >= 2 || APPLICATION.getCurrentUser().getLevel() == -1));

				logoTitle = (ImageView) findViewById(R.id.logoTitle);
				logoTitle.setOnClickListener(this);

				//animInFirst = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.slide_out_right);
				//animOutFirst = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.slide_in_left);
				animInFirst = viewSwitcher.getInAnimation();
				animOutFirst = viewSwitcher.getOutAnimation();
				animInSecond = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
				animOutSecond = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);

				dialogHelper = new ApplicationDialog(this);
				//APPLICATION.setDialogHelper(dialogHelper);

				caseList = (ListView) findViewById(R.id.caseList);
				caseList.setEmptyView(findViewById(R.id.emptyCaseView));
				caseList.setAdapter(APPLICATION.caseAdapter);
				caseList.setDuplicateParentStateEnabled(true);
				caseList.setOnItemClickListener(this);

				newsList = (ListView) findViewById(R.id.newsList);
				newsList.setEmptyView(findViewById(R.id.emptyNewsView));
				newsList.setAdapter(APPLICATION.newsAdapter);
				newsList.setDuplicateParentStateEnabled(true);
				newsList.setOnItemClickListener(this);
				newsList.setOnScrollListener(this);

				registerForContextMenu(findViewById(R.id.hidden));

				emptyNewsText = (TextView) findViewById(R.id.emptyNewsTextView);
				refreshNewsProgressBar = (ProgressBar) findViewById(R.id.refreshNewsProgressBar);
				refreshNewsButton = (Button) findViewById(R.id.refreshNewsButton);

				refreshNewsButton.setOnClickListener(new OnClickListener() {
						public void onClick(View arg0) {
								setActivityStreamStatus(getString(R.string.bubble_waiting_activity_stream), false);
								readActivityStream(0);
						}

				});

				setActivityStreamStatus(getString(R.string.bubble_waiting_activity_stream), false);
				readActivityStream(0);
				/*
				 *
				 * checkUpdates();
				 */
				volumeDialog = new Dialog(this, android.R.style.Theme_Light_NoTitleBar /*
				 * Theme_Holo_Light_Dialog_NoActionBar_MinWidth
				 */);
				lengthDialog = new Dialog(this, android.R.style.Theme_Light_NoTitleBar/*
				 * Theme_Holo_Light_Dialog_NoActionBar_MinWidth
				 */);

				volumeDialog.setContentView(R.layout.dialog_volume);
				lengthDialog.setContentView(R.layout.dialog_length);

				volumeDialogText = (TextView) volumeDialog.findViewById(R.id.vol_txt);
				volumeDialogSeek = (SeekBar) volumeDialog.findViewById(R.id.vol_seek);
				volumeDialogOk = (Button) volumeDialog.findViewById(R.id.vol_ok);
				lengthDialogText = (TextView) lengthDialog.findViewById(R.id.len_txt);
				lengthDialogSeek = (SeekBar) lengthDialog.findViewById(R.id.len_seek);
				lengthDialogOk = (Button) lengthDialog.findViewById(R.id.len_ok);

				volumeDialogOk.setOnClickListener(this);
				lengthDialogOk.setOnClickListener(this);

				volumeDialogSeek.setProgress(sharedPrefs.getVolume());
				volumeDialogText.setText(sharedPrefs.getVolume().toString() + "%");
				lengthDialogSeek.setProgress(sharedPrefs.getSoundLength());
				lengthDialogText.setText(sharedPrefs.getSoundLength().toString() + "s");

				volumeDialogSeek.setKeyProgressIncrement(5);
				lengthDialogSeek.setKeyProgressIncrement(1);

				volumeDialogSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
								volumeDialogText.setText(Integer.toString(progress) + "%");
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
								//TODO play sound + length
						}

				});

				lengthDialogSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

						@Override
						public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
								lengthDialogText.setText(Integer.toString(progress) + "s");
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
								//TODO play sound + length
						}

				});

				//enable notification
				APPLICATION.updateNotification(true, false);
		}

		@Override
		protected void onResume() {
				super.onResume();
				caseList.postInvalidate();
				newsList.postInvalidate();
		}

		@Override
		public boolean onCreateOptionsMenu(Menu menu) {
				getMenuInflater().inflate(R.menu.menu_dashboard, menu);
				return true;
		}

		@Override
		public void onBackPressed() {
				dialogHelper.messageDialog(R.string.dialog_logout_title, R.string.dialog_logout_message, ApplicationDialog.MESSAGE_CHOICE, logoutDialogListener).show();
		}

		private void visitHomepage() {
				dialogHelper.messageDialog(R.string.dialog_visit_title, R.string.dialog_visit_message, ApplicationDialog.MESSAGE_CHOICE, visitHomepageDialogListener).show();
		}

		public void toggleView(int viewChild) {
				int before = viewSwitcher.getDisplayedChild();
				boolean skip = false;

				if (before < viewChild) {
						viewSwitcher.setInAnimation(animInFirst);
						viewSwitcher.setOutAnimation(animOutFirst);
				} else if (before > viewChild) {
						viewSwitcher.setInAnimation(animInSecond);
						viewSwitcher.setOutAnimation(animOutSecond);
				} else {
						skip = true;
				}

				if (!skip) {
						viewSwitcher.setDisplayedChild(viewChild);
						viewToggleCases.setChecked(!viewToggleCases.isChecked());
						viewToggleStream.setChecked(!viewToggleStream.isChecked());
				}
		}

		public void onClick(View view) {
				int id = view.getId();
				Base.log("Clicked Element: " + getResources().getResourceEntryName(id));

				if (id == R.id.logo) {
						visitHomepage();
				}
				if (id == R.id.logoTitle) {
						visitHomepage();
				}
				if (id == R.id.toolbarCases) {
						//undo toggle
						((ToggleButton) view).setChecked(!((ToggleButton) view).isChecked());
						toggleView(VIEW_CASES);
				}
				if (id == R.id.toolbarStream) {
						//undo toggle
						((ToggleButton) view).setChecked(!((ToggleButton) view).isChecked());
						toggleView(VIEW_NEWS);
				}
				if (id == R.id.toolbarLogout) {
						onBackPressed();
				}
				if (id == R.id.toolbarSettings) {
                    Toast.makeText(this, "Disabled!", Toast.LENGTH_LONG).show();
						//openContextMenu(view);
				}
				if (id == R.id.statusBox) {
						//APPLICATION.submitLastLocation();
						APPLICATION.changeLocationUpdates(true);
						//Toast.makeText(this, getString(R.string.bubble_status_level_prefix) + APPLICATION.getCurrentUser().getLevel(), Toast.LENGTH_LONG).show();
				}
				if (id == R.id.len_ok) {
						sharedPrefs.setSoundLength(lengthDialogSeek.getProgress());
						lengthDialog.dismiss();
				}
				if (id == R.id.vol_ok) {
						sharedPrefs.setVolume(volumeDialogSeek.getProgress());
						volumeDialog.dismiss();
				}
		}

		public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				Base.log("DASHBOARD AdapterView onItemClick()");
				if (parent.getAdapter().equals(APPLICATION.newsAdapter)) {
						Toast.makeText(this, APPLICATION.newsAdapter.getItem(pos).getId(), Toast.LENGTH_LONG).show();
						//TODO
						/*
						 * Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(newsAdapter.getItem(pos).getId())); startActivity(viewIntent);
						 */
				} else if (parent.getAdapter().equals(APPLICATION.caseAdapter)) {
						APPLICATION.startMissionActivity(APPLICATION.caseAdapter.getItem(pos));
				}
		}

		/*
		 * Reset default checked status by user level @see
		 * android.widget.CompoundButton.OnCheckedChangeListener#onCheckedChanged(android.widget.CompoundButton, boolean)
		 */
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            APPLICATION.activate(isChecked);
		}

		/*
		 * Refresh button implementation TODO: translation ready + implement
		 */
		private void setActivityStreamStatus(String msg, boolean allowRefresh) {
				emptyNewsText.setText(msg);
				refreshNewsButton.setVisibility(allowRefresh ? View.VISIBLE : View.GONE);
				refreshNewsProgressBar.setVisibility(allowRefresh ? View.GONE : View.VISIBLE);
		}

		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				/*
				 * TODO: calculate itemnumber to preload list in advance
				 */
		}

		/*
		 * Naive implementation of next activity stream page on list end @see
		 * android.widget.AbsListView.OnScrollListener#onScrollStateChanged(android.widget.AbsListView, int)
		 */
		public void onScrollStateChanged(AbsListView view, int scrollState) {
				if ((newsList.getLastVisiblePosition() + 1) == newsList.getCount()) {

						//reach last element... download next page
						if (!downloading) {
								readActivityStream(lastTimestamp);
						}
				}
		}

		private void readActivityStream(long timestamp) {
				Intent serviceIntent = new Intent(getApplicationContext(), RestService.class);
				serviceIntent.putExtra(RestService.LAST_TIMESTAMP, timestamp);
				serviceIntent.putExtra(Shared.RECEIVER, restResultReceiver);
				serviceIntent.putExtra(Shared.COMMAND, RestService.ACTIVITY_STREAM_COMMAND);
				startService(serviceIntent);
		}

		private void checkUpdates() {
				Intent serviceIntent = new Intent(getApplicationContext(), RestService.class);
				serviceIntent.putExtra(Shared.RECEIVER, restResultReceiver);
				serviceIntent.putExtra(Shared.COMMAND, RestService.UPDATE_COMMAND);
				startService(serviceIntent);
		}

}
