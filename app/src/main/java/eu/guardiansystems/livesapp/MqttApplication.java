package eu.guardiansystems.livesapp;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ListAdapter;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import eu.guardiansystems.livesapp.android.DashboardActivity;
import eu.guardiansystems.livesapp.android.MissionActivity;
import eu.guardiansystems.livesapp.android.adapter.CaseAdapter;
import eu.guardiansystems.livesapp.android.adapter.NewsAdapter;
import eu.guardiansystems.livesapp.android.config.Base;
import eu.guardiansystems.livesapp.android.ui.ApplicationDialog;
import eu.guardiansystems.livesapp.models.Callback;
import eu.guardiansystems.livesapp.models.EmrCaseData;
import eu.guardiansystems.livesapp.models.EmrLocation;
import eu.guardiansystems.livesapp.models.EmrUser;
import eu.guardiansystems.livesapp.service.MqttResponse;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider;

public class MqttApplication extends Application implements OnLocationUpdatedListener, IMqttActionListener, MqttCallback, MqttResponse {
    public CaseAdapter caseAdapter;
    public NewsAdapter newsAdapter;

    class StopTask extends TimerTask {
        @Override
        public void run() {
            stopSound();
        }
    }


    private static final LocationAccuracy ACCURACY_HIGH = LocationAccuracy.HIGH;    //HIGH
    private static final LocationAccuracy ACCURACY_LOW = LocationAccuracy.LOW;      //LOW
    private static final long INTERVAL_HIGH = 15*1000;                              //15sec
    private static final float DISTANCE_HIGH = 25f;                                 //25m
    //testwise:
    //private static final long INTERVAL_HIGH = 1*1000;                               //1s
    //private static final float DISTANCE_HIGH = 1f;                                  //1m

    private static final long INTERVAL_LOW = 5*60*1000;                             //5min
    private static final float DISTANCE_LOW = 1000f;                                //1km

    private LocationParams locationParamsHigh;
    private LocationParams locationParamsLow;

    //Mqtt
    private String host = "tcp://rabbit.livesapp.io:1883";
    private MqttAndroidClient client;
    private MqttConnectOptions options = new MqttConnectOptions();
    private MqttClientPersistence usePersistence = new MemoryPersistence();
    private ConcurrentHashMap<String, MqttResponse> subscriptionCallbacks = new ConcurrentHashMap<>();

    private Queue<Map.Entry<String,MqttResponse>> queuedMqttJobs = new LinkedList<>();
    private boolean resubscribe = false;

    // Singleton: application & mqttclient & user
    private String mqttClientId = null;
    public static MqttApplication APPLICATION = null;

    //		public EmrUser CURRENT_USER;
//		public String CURRENT_DISPATCHER_ID;
//		public String LAST_DISPATCHER_ID;
    private SmartLocation smartLocation;
    private LocationGooglePlayServicesWithFallbackProvider locationProvider;
    public String DISPLAYED_CASE_ID = "blank";

    //public boolean LOGOUT = false;
    //private boolean LOGIN_LOCK = false;
    // Notifications / statusbar
    private Intent mNotificationIntent;
    private Notification mNotification;
    private NotificationManager mNotificationManager;
    private PendingIntent mNotificationPendingIntent;

    // Activity handling
    private Activity mCurrentActivity = null;

    public DashboardActivity dashboardActivity;
    public MissionActivity missionActivity;
    private ApplicationDialog dialogHelper;

    // Shared application data
    private AdvancedPreferences sharedPrefs;
    public ArrayList<EmrCaseData> activeCases = new ArrayList<EmrCaseData>();

    // Audio & alarmangement
    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;
    private int defaultVolume;
    private Uri alarmSound;
    private Uri defaultSound;

    private final DialogInterface.OnClickListener okDialogListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            switch (id) {
                case DialogInterface.BUTTON_POSITIVE:
                    dialog.dismiss();
                    break;
            }
        }
    };

    private final DialogInterface.OnClickListener downloadDialogListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            switch (id) {
                case DialogInterface.BUTTON_POSITIVE:
                    dialog.dismiss();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    dialog.dismiss();
                    break;
            }
        }

    };

    private EmrLocation newLocation(Location location) {
        EmrLocation newLocation = new EmrLocation(location.getLatitude(), location.getLongitude());
        newLocation.setAltitude(location.getAltitude());
        newLocation.setTimestamp(location.getTime());
        newLocation.setProvider(location.getProvider());
        return newLocation;
    }

    @Override
    public void onLocationUpdated(Location location) {
        Base.log("Location changed -> updating!");
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

    public void onSuccess(IMqttToken imt) {
        Base.log("MqttApplication: onSuccess");

        if(imt.getClient().isConnected()){
            if(getCurrentUser() != null){
                subscribeCases();
            }

            delayedSubscribe();


            if(resubscribe && subscriptionCallbacks != null){
                for(ConcurrentHashMap.Entry<String, MqttResponse> entry : subscriptionCallbacks.entrySet()) {
                    String topic = entry.getKey();
                    MqttResponse listener = entry.getValue();

                    subscribeMQTT(topic, listener);
                }
                resubscribe = false;
            }
        }
    }

    @Override
    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
        Base.log("MqttApplication: onFailure - " + throwable.getMessage());
    }

    public void delayedSubscribe(){
        for(int i = 0; i < queuedMqttJobs.size(); i++){
            Map.Entry subvalue = queuedMqttJobs.poll();
            String todo = (String) subvalue.getKey();
            MqttResponse listener = (MqttResponse) subvalue.getValue();
            String job = todo.split(":")[0];
            String topic = todo.split(":")[1];
            if(job.equals("sub")){
                subscribeMQTT(topic, listener);
            }else{
                unsubscribeMQTT(topic, listener);
            }
        }
    }

    @Override
    public void onSuccess(String topic) {
        Base.log("Subscribed to: " + topic);
    }

    public void addCaseToAdapter(EmrCaseData caseData) {
        caseAdapter.add(caseData);
        if(dashboardActivity != null){
            dashboardActivity.toggleView(dashboardActivity.VIEW_CASES);
        }

    }

    public void removeRunningCase(String caseId) {
        final String timedOutCase = caseId;
        if(dashboardActivity != null) {
            dashboardActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if (timedOutCase.equals(APPLICATION.DISPLAYED_CASE_ID)) {
                        if (missionActivity != null) {
                            missionActivity.closeCase();
                        }
                    }
                    caseAdapter.removeCaseById(timedOutCase);
                }

            });
        }
    }

    public void updateCaseInAdapter(EmrCaseData caseData) {
        caseAdapter.update(caseData);
    }

    @Override
    public void onResponse(String topic, MqttMessage payload) {
        Base.log("handleMessage() MqttApplication: topic=" + topic + ", message=" + new String(payload.getPayload()));
        String[] subTopics = topic.split(Pattern.quote("/"));
        String base = subTopics[1];
        String command = subTopics[2];

        String payloadString = new String(payload.getPayload());

        if (base.equals("api")) {
            //We don't need to handle API requests
            return;
        } else if (base.equals("client")) {
            command = subTopics[4];
        }

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock
                = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "caseDataUpdate");
        wakeLock.acquire();

        if (command.equals("updateCase") || command.equals("startCase")) {
            EmrCaseData receivedCaseData = null;
            try {
                Callback callback = gson.fromJson(payloadString, Callback.class);
                receivedCaseData = callback.casedata;
                receivedCaseData.setServer_offset(SystemClock.elapsedRealtime() - callback.time);

                // check if this case is new to the android client
                if (!hasCase(receivedCaseData.getId())) {
                    playSound(defaultVolume, 3, false, false);
                    addCaseToAdapter(receivedCaseData);
                } else {
                    updateCaseInAdapter(receivedCaseData);
                }
            } catch (Exception e) {
                Base.log(e.getMessage());
            }

            if (receivedCaseData != null) {
                startMissionActivity(receivedCaseData);
            }
        } else if (command.equals("closeCase")) {
            Callback callback = gson.fromJson(payloadString, Callback.class);

            if (callback.error == null) {
                EmrCaseData receivedCaseData = callback.casedata;

                if (hasCase(receivedCaseData.getId())) {
                    removeRunningCase(receivedCaseData.getId());
                }
            }
        }

        wakeLock.release();
    }

    @Override
    public void onError(String message) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage payload) throws Exception {
        if(subscriptionCallbacks.containsKey(topic)){
            subscriptionCallbacks.get(topic).onResponse(topic, payload);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    @Override
    public void connectionLost(Throwable throwable) {
        Base.log("MQTT Connection Lost");
        resubscribe = true;
        connectMQTT();
    }

    public void setSound(boolean setDefault) {
        try {
            mMediaPlayer.setDataSource(this, setDefault ? defaultSound : alarmSound);
        } catch (IOException ex) {
            Base.log("Setting sound not possible");
        }
    }

    public void playSound(int volume, int length, boolean setDefault, boolean vibrate) {
        int flag = AudioManager.RINGER_MODE_NORMAL;

        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, flag);
        try {
            if (!mMediaPlayer.isPlaying()) {
                setSound(setDefault);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();

                Timer timer = new Timer("timer", true);
                timer.schedule(new StopTask(), length * 1000);
            }

        } catch (IOException e) {
            Base.log("Playing sound not possible");
        }

    }

    public void stopSound() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, defaultVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    public void releasePlayer() {
        mMediaPlayer.release();
    }

    public Activity getCurrentActivity() {
        return mCurrentActivity;
    }

    public void setCurrentActivity(Activity activity, boolean notification) {
        mCurrentActivity = activity;

        if (activity instanceof MissionActivity) {
            missionActivity = (MissionActivity) activity;
        } else if (activity instanceof DashboardActivity) {
            dashboardActivity = (DashboardActivity) activity;
        }

        updateNotification(notification, false);
    }

    public boolean isGoogleMapsInstalled() {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0);
            return info.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void setCurrentUser(EmrUser currentUser) {
        sharedPrefs.putObject("currentUser", currentUser);
        sharedPrefs.commit();
    }

    public EmrUser getCurrentUser() {
        return sharedPrefs.getObject("currentUser", EmrUser.class);
    }

    public void setLastLocation(EmrLocation currentLocation) {
        sharedPrefs.putObject("lastLocation", currentLocation);
        sharedPrefs.commit();
    }

    public EmrLocation getLastLocation() {
        return sharedPrefs.getObject("lastLocation", EmrLocation.class);
    }

    public void setDispatcherId(String id) {
        sharedPrefs.putObject("dispatcherId", id);
        sharedPrefs.commit();
    }

    public String getDispatcherId() {
        return sharedPrefs.getObject("dispatcherId", String.class);
    }

    public void setLastUserId(String id) {
        sharedPrefs.putObject("lastUserId", id);
        sharedPrefs.commit();
    }

    public String getLastUserId() {
        return sharedPrefs.getObject("lastUserId", String.class);
    }

    public void setLastDispatcherId(String id) {
        sharedPrefs.putObject("lastDispatcherId", id);
        sharedPrefs.commit();
    }

    public String getLastDispatcherId() {
        return sharedPrefs.getObject("lastDispatcherId", String.class);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        stopLocationUpdates();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Base.log("***************** ONCREATE APPLICATION ********************");
        APPLICATION = this;

        mNotificationIntent = new Intent(this, MqttApplication.class);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        options.setPassword("jackrabbit_9_emurgency.io".toCharArray());
        options.setUserName("bucksbunny_6_emurgency.io");
        options.setKeepAliveInterval(49);
        options.setMaxInflight(MqttConnectOptions.MAX_INFLIGHT_DEFAULT);
        options.setCleanSession(true);

        connectMQTT();

        locationParamsHigh = new LocationParams.Builder()
                .setAccuracy(ACCURACY_HIGH).setInterval(INTERVAL_HIGH).setDistance(DISTANCE_HIGH).build();
        locationParamsLow = new LocationParams.Builder()
                .setAccuracy(ACCURACY_LOW).setInterval(INTERVAL_LOW).setDistance(DISTANCE_LOW).build();

        sharedPrefs = AdvancedPreferences.getAdvancedPreferences(this, "livesapp.io", MODE_PRIVATE);

        caseAdapter = new CaseAdapter(getApplicationContext());
        newsAdapter = new NewsAdapter(getApplicationContext());

        mMediaPlayer = new MediaPlayer();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        alarmSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alert);
        defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); //TODO default Notification sound!
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        defaultVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);

        TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        smartLocation = new SmartLocation.Builder(this).logging(true).build();
        locationProvider = new LocationGooglePlayServicesWithFallbackProvider(this);

        Location lastLocation = smartLocation.location().getLastLocation();

        if (lastLocation != null && lastLocation.getLatitude() != 0.0 && lastLocation.getLongitude() != 0.0) {
            EmrLocation location = new EmrLocation();
            location.setLatitude(lastLocation.getLatitude());
            location.setLongitude(lastLocation.getLongitude());
            location.setTimestamp(lastLocation.getTime());

            setLastLocation(location);
        }

        startLocationUpdates();
    }

    public void startLocationUpdates() {
        smartLocation.location().config(locationParamsLow).provider(locationProvider)
                .start(this);
    }

    public void stopLocationUpdates() {
        smartLocation.location().provider(locationProvider)
                .stop();
    }

    public void changeLocationUpdates(boolean high) {
        smartLocation.location().stop();
        smartLocation.location().config((high ? locationParamsHigh : locationParamsLow)).provider(locationProvider).start(this);
    }

    // MQTT spec does not allow client ids longer than 23 chars
    public String getAndroidClientId() {
        if (mqttClientId == null) {
            String android_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (android_id.length() > 12) {
                android_id = android_id.substring(0, 12);
            }
            mqttClientId = android_id;
        }
        Base.log("getAndroidClientId(): " + mqttClientId);
        return mqttClientId;
    }

    public void startMissionActivity(EmrCaseData caseData) {
        Base.log("*** startMissionActivity() ***");
        Intent missionIntent = new Intent();
        missionIntent.setClass(getApplicationContext(), MissionActivity.class);
        missionIntent.putExtra("CASEID", caseData.getId());
        missionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(missionIntent);
    }

    public void updateNotification(boolean connected, boolean logout) {

        if (mCurrentActivity != null) {
            try {
                mNotificationPendingIntent = PendingIntent.getActivity(mCurrentActivity, 0, mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                mNotification = new NotificationCompat.Builder(this)
                        .setContentTitle(getString(R.string.full_app_name))
                        .setContentIntent(mNotificationPendingIntent)
                        .setContentText(getString(connected ? R.string.notification_available : R.string.notification_unavailable))
                        .setSmallIcon(connected ? R.drawable.icon_notification_color : R.drawable.icon_notification_grey)
                        .build();
            } catch (Exception e) {
                Base.log(e.getMessage());
            }
            mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

            if (!logout) {
                mNotificationManager.notify(Base.TAG, 255, mNotification);
            } else {
                mNotificationManager.cancel(Base.TAG, 255);
            }
        }
    }

    public void addCase(EmrCaseData caseData) {
        if (!hasCase(caseData.getId())) {
            activeCases.add(null);
        }
    }

    public boolean hasCase(String caseId) {
        return (getCaseById(caseId) != null);
    }

    public EmrCaseData getCaseById(String caseId) {
        for (EmrCaseData oneCase : activeCases) {
            if (oneCase.getId().equals(caseId)) {
                return oneCase;
            }
        }
        return null;
    }

    public void subscribeCases() {
        String lastUserId = getCurrentUser().getId();
        String lastDispatcherId = getDispatcherId();

        setLastUserId(lastUserId);
        setLastDispatcherId(lastDispatcherId);

        String[] topics = {
                "/client/" + lastDispatcherId + "/" + lastUserId + "/acceptCase/callback",
                "/client/" + lastDispatcherId + "/" + lastUserId + "/startCase/callback",
                "/client/" + lastDispatcherId + "/" + lastUserId + "/updateCase/callback",
                "/client/" + lastDispatcherId + "/" + lastUserId + "/closeCase/callback"
        };

        for(int i = 0; i < topics.length; i++ ){
            subscribeMQTT(topics[i], this);
        }
    }

    public void unsubscribeCases(){
        String lastUserId = getLastUserId();
        String lastDispatcherId = getLastDispatcherId();

        String[] topics = {
                "/client/" + lastDispatcherId + "/" + lastUserId + "/acceptCase/callback",
                "/client/" + lastDispatcherId + "/" + lastUserId + "/startCase/callback",
                "/client/" + lastDispatcherId + "/" + lastUserId + "/updateCase/callback",
                "/client/" + lastDispatcherId + "/" + lastUserId + "/closeCase/callback"
        };

        for(int i = 0; i < topics.length; i++ ){
            unsubscribeMQTT(topics[i], this);
        }

        setLastUserId(null);
        setLastDispatcherId(null);
    }

    // client only listens to server/{id}/# so we tell the server our {id} in the loginRequest
    public void login(EmrUser loginUser) {
        String topic = "/api/login";
        if(client != null) {
            try {
                client.publish(topic, loginUser.toJson().getBytes(), 1, true);
            } catch (MqttException e) {
                if (subscriptionCallbacks.containsKey(topic)) {
                    subscriptionCallbacks.get(topic).onError(e.getMessage());
                }
            }
        }
    }

    public void registration(EmrUser registerUser) {
        String topic = "/api/register";
        if(client != null) {
            try {
                client.publish(topic, registerUser.toJson().getBytes(), 1, true);
            } catch (MqttException e) {
                if (subscriptionCallbacks.containsKey(topic)) {
                    subscriptionCallbacks.get(topic).onError(e.getMessage());
                }
            }
        }
    }

    public void activate(boolean active) {
        String topic = "/client/" + getDispatcherId() + "/" + getCurrentUser().getId() + "/active";
        if(client != null) {
            try {
                client.publish(topic, new JSONObject("{ active: " + active + " }").toString().getBytes(), 1, true);
            } catch (MqttException | JSONException e) {
                if (subscriptionCallbacks.containsKey(topic)) {
                    subscriptionCallbacks.get(topic).onError(e.getMessage());
                }
            }
        }
    }

    public void logout() {
        String topic = "/client/" + getDispatcherId() + "/" + getCurrentUser().getId() + "/logout";

            try {
                if(client != null) {
                    client.publish(topic, getCurrentUser().toJson().getBytes(), 1, true);

                    APPLICATION.dashboardActivity.finish();
                    APPLICATION.dashboardActivity = null;

                    APPLICATION.unsubscribeCases();

                    APPLICATION.caseAdapter.clear();
                    APPLICATION.newsAdapter.clear();
                }
            } catch (MqttException | NullPointerException e ) {
                if (subscriptionCallbacks.containsKey(topic)) {
                    subscriptionCallbacks.get(topic).onError(e.getMessage());
                }
            }

            //remove active dispatcher
            setDispatcherId(null);

            //remove user
            setCurrentUser(null);

            //disable notifications
            updateNotification(false, true);

            //disable location updates
            stopLocationUpdates();

    }


    public void acceptCase(String caseId) {
        String topic = "/client/" + getDispatcherId() + "/" + getCurrentUser().getId() + "/acceptCase";
        if(client != null) {
            try {
                client.publish(topic, getCaseById(caseId).toJson().getBytes(), 1, true);
            } catch (MqttException e) {
                if (subscriptionCallbacks.containsKey(topic)) {
                    subscriptionCallbacks.get(topic).onError(e.getMessage());
                }
            }
        }
    }

    public void updateLocation(EmrLocation location) {
        String topic = "/client/" + getDispatcherId() + "/" + getCurrentUser().getId() + "/updateLocation";
        if(client != null){
            try {
                client.publish(topic, location.toJson().getBytes(), 1, true);
                setLastLocation(location);
            } catch (MqttException e) {
                if(subscriptionCallbacks.containsKey(topic)){
                    subscriptionCallbacks.get(topic).onError(e.getMessage());
                }
            }
        }
    }

    public void arrivedAtCase(String caseId) {
        String topic = "/client/" + getDispatcherId() + "/" + getCurrentUser().getId() + "/arrivedAtCase";
        if(client != null) {
            try {
                client.publish(topic, getCaseById(caseId).toJson().getBytes(), 1, true);
            } catch (MqttException e) {
                if (subscriptionCallbacks.containsKey(topic)) {
                    subscriptionCallbacks.get(topic).onError(e.getMessage());
                }
            }
        }
    }

    public void connectMQTT() {
        if (client == null) {
            client = new MqttAndroidClient(this, host, "andi_" + getAndroidClientId(), usePersistence);
            client.setCallback(this);
        }

        try {
            client.connect(options, null, this);
        } catch (MqttException e) {
            Base.log("\tCONNECTION ERROR: " + e.getMessage());
        }

        updateNotification(false, true);
    }

    public void disconnectMQTT() {
        if (client != null) {
            try {
                client.disconnect();
                client = null;
            } catch (MqttException e) {
                Log.e("MQTT", "\tDISCONNECT ERROR: " + e.getMessage());
            }
            mNotificationManager.cancelAll();
        }
    }

    public void subscribeMQTT(String topic, MqttResponse responselistener) {
        if(subscriptionCallbacks.containsKey(topic)){
            subscriptionCallbacks.remove(topic);
        }

        if(client != null && client.isConnected()){
            try {
                client.subscribe(topic, 1);
            } catch (MqttException e) {
                responselistener.onError(e.getMessage());
            }
            subscriptionCallbacks.put(topic, responselistener);
            responselistener.onSuccess(topic);
            delayedSubscribe();
        }else{
            queuedMqttJobs.offer(new AbstractMap.SimpleEntry<>("sub:"+topic, responselistener));
            responselistener.onError("Disconnected MQTT Broker");
        }
    }

    public void unsubscribeMQTT(String topic, MqttResponse responselistener){
        if(client != null) {
            try {
                client.unsubscribe(topic);
            } catch (MqttException e) {
                responselistener.onError(e.getMessage());
            }

            if(subscriptionCallbacks.containsKey(topic)){
                subscriptionCallbacks.remove(topic);
                responselistener.onSuccess(topic);
                delayedSubscribe();
            }else{
                responselistener.onError("Not subscribed to MQTT Broker");
            }
        }else{
            queuedMqttJobs.offer(new AbstractMap.SimpleEntry<>("unsub:"+topic, responselistener));
            responselistener.onError("Disconnected MQTT Broker");
        }
    }

}
