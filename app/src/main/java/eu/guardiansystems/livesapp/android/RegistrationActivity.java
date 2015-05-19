package eu.guardiansystems.livesapp.android;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.EditText;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;

import static eu.guardiansystems.livesapp.MqttApplication.APPLICATION;

import eu.guardiansystems.livesapp.R;
import eu.guardiansystems.livesapp.android.config.Base;
import eu.guardiansystems.livesapp.android.config.Validator;
import eu.guardiansystems.livesapp.models.Callback;
import eu.guardiansystems.livesapp.models.EmrLocation;
import eu.guardiansystems.livesapp.models.EmrUser;
import eu.guardiansystems.livesapp.service.MqttResponse;

public class RegistrationActivity extends Activity implements OnFocusChangeListener, MqttResponse {

		private EditText emailForm;
		private EditText firstNameForm;
		private EditText lastNameForm;
		private EditText passwordForm;
		private EditText passcheckForm;
		private Button submitButton;

		private Resources resources;
		private Builder messageDialog;
		private ProgressDialog progressDialog;

		//private RestReceiver restReceiver;
		private Validator validate;

		private final String CALLBACK_URL = "/api/register/callback";

		/**
		 * First creation of the screen when activity starts. Devices with hardware keyboard will call onCreate if keyboard is opened/closed
		 */
		@Override
		public void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);
				setContentView(R.layout.screen_registration);

				emailForm = (EditText) findViewById(R.id.regEmail);
				firstNameForm = (EditText) findViewById(R.id.regFirstName);
				lastNameForm = (EditText) findViewById(R.id.regLastName);
				passwordForm = (EditText) findViewById(R.id.regPass);
				passcheckForm = (EditText) findViewById(R.id.regPassValid);

				// init validator & restreceiver
				validate = new Validator(this);
		//restReceiver = new RestReceiver(this);

				// get stringressources & prebuild message
				resources = getResources();
				messageDialog = new Builder(this);
				progressDialog = new ProgressDialog(this);
				progressDialog.setMessage(resources.getString(R.string.registration_loading));

				// set focuslisteners for validation checks
				emailForm.setOnFocusChangeListener(this);
				firstNameForm.setOnFocusChangeListener(this);
				lastNameForm.setOnFocusChangeListener(this);
				passwordForm.setOnFocusChangeListener(this);
				passcheckForm.setOnFocusChangeListener(this);

				submitButton = (Button) findViewById(R.id.buttonRegister);
				submitButton.setOnClickListener(new View.OnClickListener() {
						public void onClick(View v) {
								progressDialog.show();

								EmrUser registerUser = new EmrUser(emailForm.getText().toString(), passwordForm.getText().toString());
								registerUser.setName(firstNameForm.getText().toString() + " " + lastNameForm.getText().toString());

                                TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
                                registerUser.setMobileNumber(tm.getLine1Number());

                                EmrLocation tmp = APPLICATION.getLastLocation();
                                registerUser.setCurrentLocation(tmp.getLatitude(), tmp.getLongitude());

                                //TODO determine based on region / geofencing
                                String tmp_dispatcher_solution = "aedbf693c6d454aca200f76b97d3895a";
                                registerUser.setActive_dispatcher(tmp_dispatcher_solution);

                                ArrayList<String> nearby = new ArrayList<String>();
                                nearby.add(tmp_dispatcher_solution);

                                registerUser.setDispatcher(nearby);

								APPLICATION.registration(registerUser);
						}

				});

				APPLICATION.subscribeMQTT(CALLBACK_URL, this);
		}

		@Override
		public void onBackPressed() {
				Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
				startActivity(intent);
				finish();
		}

		@Override
		public void onDestroy() {
				Base.log("onDestroy (LoginActivity)");
				validate = null;
				progressDialog = null;
				messageDialog = null;
				APPLICATION.unsubscribeMQTT(CALLBACK_URL, this);
				super.onDestroy();
		}

		private void showMessageDialog(String title, String text, String buttonText, DialogInterface.OnClickListener listener) {
				messageDialog.setTitle(title);
				messageDialog.setMessage(text);
				messageDialog.setPositiveButton(buttonText, listener);
				messageDialog.show();
		}

		public void onFocusChange(View view, boolean hasFocus) {
				// TODO: do complete validation on each focus
				if (!hasFocus) {
						submitButton.setEnabled(true);
						EditText target = (EditText) view;
						int id = target.getId();
						if (id == R.id.regEmail) {
								if (!validate.checkEmail(target.getText().toString())) {
										target.setError(validate.wrongEmail());
										submitButton.setEnabled(false);
								} else {
										target.setError(null);
								}
						}
						if (id == R.id.regFirstName) {
								if (!validate.checkFirstName(target.getText().toString())) {
										target.setError(validate.wrongFirstName());
										submitButton.setEnabled(false);
								} else {
										target.setError(null);
								}
						}
						if (id == R.id.regLastName) {
								if (!validate.checkLastName(target.getText().toString())) {
										target.setError(validate.wrongLastName());
										submitButton.setEnabled(false);
								} else {
										target.setError(null);
								}
						}
						if (id == R.id.regPass) {
								if (!validate.checkPassword(target.getText().toString())) {
										target.setError(validate.wrongPassword());
										submitButton.setEnabled(false);
								} else {
										target.setError(null);
								}
						}
						if (id == R.id.regPassValid) {
								if (!validate.samePasswords(target.getText().toString(),
										passwordForm.getText().toString())) {
										target.setError(validate.wrongPasswordCheck());
										submitButton.setEnabled(false);
								} else {
										target.setError(null);
								}
						}
				}
		}

    @Override
    public void onSuccess(String topic) {
        if(topic.equals(CALLBACK_URL)) {
            submitButton.setEnabled(true);
        }
    }

    @Override
    public void onResponse(String topic, MqttMessage message) {
        String response = new String(message.getPayload());

        if (topic.equals(CALLBACK_URL)) {
            progressDialog.dismiss();

            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

            //Just to be sure
            Callback callback = null;
            try {
                callback = gson.fromJson(response, Callback.class);
            } catch (Exception e) {
                Base.log(e.getMessage());
            }

            if (callback.error != null) {
                DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }

                };
                showMessageDialog(resources.getString(R.string.registration_title),
                        resources.getString(R.string.registration_exists), resources.getString(R.string.button_positive_ok), clickListener);

            } else if (callback.user != null) {
                DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        onBackPressed();
                    }

                };
                showMessageDialog(resources.getString(R.string.registration_title), resources.getString(
                        R.string.registration_success), resources.getString(R.string.button_positive_ok), clickListener);
            }
        }
    }

    @Override
    public void onError(String message) {
        Base.log(message);
    }
}
