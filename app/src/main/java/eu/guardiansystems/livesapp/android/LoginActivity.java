package eu.guardiansystems.livesapp.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.snowdream.android.app.AbstractUpdateListener;
import com.github.snowdream.android.app.DownloadListener;
import com.github.snowdream.android.app.DownloadManager;
import com.github.snowdream.android.app.DownloadTask;
import com.github.snowdream.android.app.UpdateFormat;
import com.github.snowdream.android.app.UpdateInfo;
import com.github.snowdream.android.app.UpdateManager;
import com.github.snowdream.android.app.UpdateOptions;
import com.github.snowdream.android.app.UpdatePeriod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;

import eu.guardiansystems.livesapp.R;
import eu.guardiansystems.livesapp.android.config.Base;
import eu.guardiansystems.livesapp.android.config.SharedPrefs;
import eu.guardiansystems.livesapp.android.ui.ApplicationDialog;
import eu.guardiansystems.livesapp.android.ui.FontHelper;
import eu.guardiansystems.livesapp.models.Callback;
import eu.guardiansystems.livesapp.models.EmrLocation;
import eu.guardiansystems.livesapp.models.EmrUser;
import eu.guardiansystems.livesapp.service.MqttResponse;

import static eu.guardiansystems.livesapp.MqttApplication.APPLICATION;

public class LoginActivity extends Activity implements OnClickListener, MqttResponse {
    private Button buttonLogin;
    private Button buttonRegister;
    private CheckBox checkSave;
    private TextView inputEmail;
    private TextView inputPassword;

    private SharedPrefs sharedPrefs;
    private EmrUser loginUser;

    private ProgressDialog loadingDialog;
    private ApplicationDialog dialog;

    private final String CALLBACK_URL = "/api/login/callback";

    private final DialogInterface.OnClickListener exitApplicationDialogListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            switch (id) {
                case DialogInterface.BUTTON_POSITIVE:
                    dialog.dismiss();
                    finish();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    dialog.dismiss();
                    break;
            }
        }

    };

    private final DialogInterface.OnClickListener loginFailedDialogListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            switch (id) {
                case DialogInterface.BUTTON_NEUTRAL:
                    dialog.dismiss();
                    break;
            }
        }

    };


    private UpdateManager updateManager;
    private UpdateOptions updateOptions;

    private MaterialDialog.Builder updateDialogBuilder;
    private MaterialDialog updateDialog;

    private MaterialDialog.Builder downloadDialogBuilder;
    private MaterialDialog downloadDialog;

    private AbstractUpdateListener updateListener = new AbstractUpdateListener() {

        @Override
        public void onShowNoUpdateUI() {
            //DO NOTHING
        }

        @Override
        public void onShowUpdateUI(final UpdateInfo updateInfo) {
            final String downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            final String apkFile = downloadDir + File.separator + "livesapp.apk";

            //delete all temporary
            File tmp = new File(apkFile);
            tmp.delete();

            updateDialog = updateDialogBuilder.callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    DownloadManager manager = new DownloadManager(LoginActivity.this);
                    final DownloadTask downloadTask = new DownloadTask(LoginActivity.this);

                    downloadTask.setUrl(updateInfo.getApkUrl());
                    downloadTask.setPath(apkFile);

                    manager.start(downloadTask, new DownloadListener<Integer, DownloadTask>() {
                        @Override
                        public void onProgressUpdate(Integer... values) {
                            super.onProgressUpdate(values);
                            onShowUpdateProgressUI(updateInfo, downloadTask, values[0]);
                        }

                        @Override
                        public void onSuccess(DownloadTask downloadTask) {
                            super.onSuccess(downloadTask);
                            if (downloadTask != null && !TextUtils.isEmpty(downloadTask.getPath())) {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setDataAndType(Uri.parse("file://" + downloadTask.getPath()), "application/vnd.android.package-archive");
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                            }
                        }
                    });

                    dialog.dismiss();
                    downloadDialog = downloadDialogBuilder.show();
                }

                @Override
                public void onNegative(MaterialDialog dialog) {
                    informCancel(updateInfo);
                    dialog.dismiss();
                }
            }).show();
        }

        @Override
        public void onShowUpdateProgressUI(UpdateInfo updateInfo, DownloadTask downloadTask, int i) {
            if (i != 100) {
                downloadDialog.setProgress(i);
            } else {
                downloadDialog.dismiss();
            }
        }

        @Override
        public void ExitApp() { /* Do nothing */ }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.screen_login);

        //start dashboard because still logged in
        if (APPLICATION.getCurrentUser() != null) {
            Base.log("Current user = " + APPLICATION.getCurrentUser().getEmail());
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
            finish();
        } else {
            Base.log("Current user = null???");
        }

        // Globally add 'droid sans' font since different fonts break layout...
        FontHelper fHelper = new FontHelper(this);
        fHelper.applyCustomFont((RelativeLayout) findViewById(R.id.loginRoot));

        // Hold instance of logged in user form data
        sharedPrefs = new SharedPrefs(getApplicationContext());

        inputEmail = (TextView) findViewById(R.id.formEmail);
        inputPassword = (TextView) findViewById(R.id.formPassword);
        checkSave = (CheckBox) findViewById(R.id.formCheckSave);
        buttonLogin = (Button) findViewById(R.id.buttonLogin);
        buttonRegister = (Button) findViewById(R.id.buttonRegister);

        // Autofill form
        checkSave.setChecked(sharedPrefs.getAutoFill());
        inputEmail.setText(sharedPrefs.getEmail());

        buttonLogin.setOnClickListener(this);
        buttonRegister.setOnClickListener(this);

        dialog = new ApplicationDialog(this);
        loadingDialog = dialog.progressDialog(R.string.login_loading_title, R.string.login_loading_message);

        APPLICATION.setCurrentActivity(this, false);
        APPLICATION.subscribeMQTT(CALLBACK_URL, this);

        updateManager = new UpdateManager(this);

        updateOptions = new UpdateOptions.Builder(this)
                .checkUrl("http://lab.guardiansystems.eu/application/beta-releases/raw/master/update.xml")
                .updateFormat(UpdateFormat.XML)
                .updatePeriod(new UpdatePeriod(UpdatePeriod.EACH_TIME))
                .checkPackageName(false)
                .build();

        updateDialogBuilder = new MaterialDialog.Builder(this)
                .title(getString(R.string.new_update_available_title))
                .content(Html.fromHtml(getString(R.string.new_update_available)))
                .positiveText(R.string.new_update_available_yes)
                .negativeText(R.string.new_update_available_no);

        downloadDialogBuilder = new MaterialDialog.Builder(this)
                .title(R.string.new_update_available_progress_title)
                .content(R.string.new_update_available_progress)
                .progress(false, 100);

        APPLICATION.startLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();

        APPLICATION.connectMQTT();
        updateManager.check(this, updateOptions, updateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        APPLICATION.disconnectMQTT();
    }

    @Override
    public void onBackPressed() {
        dialog.messageDialog(R.string.dialog_quit_title, R.string.dialog_quit_message, ApplicationDialog.MESSAGE_CHOICE, exitApplicationDialogListener).show();
    }

    public void onClick(View view) {
        if (view.getId() == R.id.buttonLogin) {
            if (inputEmail.getText().toString().trim().equals("")) {
                return;
            }
            loadingDialog.show();

						/*
                         * Submit login data and wait for response
						 */
            loginUser = new EmrUser(inputEmail.getText().toString(), inputPassword.getText().toString());
            APPLICATION.login(loginUser);
        } else if (view.getId() == R.id.buttonRegister) {
            Intent intent = new Intent(getApplicationContext(), RegistrationActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onSuccess(String topic) {
        if (topic.equals(CALLBACK_URL)) {
            buttonLogin.setEnabled(true);
        }
    }

    @Override
    public void onResponse(String topic, MqttMessage message) {
        String response = new String(message.getPayload());

        if (topic.equals(CALLBACK_URL)) {
            loadingDialog.dismiss();
            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

            //Just to be sure
            Callback callback = null;
            try {
                callback = gson.fromJson(response, Callback.class);
            } catch (Exception e) {
                Base.log(e.getMessage());
            }

            if (callback != null) {
                if (callback.user != null) {
                    if (checkSave.isChecked()) {
                        sharedPrefs.setEmail(callback.user.getEmail());
                    }
                    sharedPrefs.setAutoFill(checkSave.isChecked());

                    //set dispatcher
                    //TODO first item sorted
                    //String nearby_dispatcher = callback.user.getDispatcher().get(0);
                    APPLICATION.setDispatcherId(callback.user.getActive_dispatcher());

                    EmrLocation lastKnownLocation = APPLICATION.getLastLocation();
                    //set user
                    EmrUser loggedIn = new EmrUser(callback.user.getEmail(), inputPassword.getText().toString());
                    loggedIn.setLevel(callback.user.getLevel());
                    loggedIn.setActive_dispatcher(callback.user.getActive_dispatcher());
                    if (loggedIn.getCurrentLocation().latitude == 0.0 || loggedIn.getCurrentLocation().longitude == 0.0) {
                        loggedIn.setCurrentLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    }

                    Base.log(response);
                    APPLICATION.setCurrentUser(loggedIn);
                    APPLICATION.subscribeCases();
                    buttonLogin.setEnabled(false);

                    //start dashboard
                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    startActivity(intent);
                    finish();
                } else if (callback.error != null) {
                    dialog.messageDialog(R.string.login_error, R.string.login_invalid_message, ApplicationDialog.MESSAGE_NEUTRAL, loginFailedDialogListener).show();
                }
            }

        }
    }

    @Override
    public void onError(String message) {
        Base.log(message);
    }
}
