/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.guardiansystems.livesapp;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class AdvancedPreferences {
		private static AdvancedPreferences advancedPreferences;
		private Context context;
		private SharedPreferences preferences;
		private SharedPreferences.Editor editor;
		private static Gson GSON = new Gson();
		Type typeOfObject = new TypeToken<Object>() {
		}.getType();

		private AdvancedPreferences(Context context, String namePreferences, int mode) {
				this.context = context;
				if (namePreferences == null || namePreferences.equals("")) {
						namePreferences = "complex_preferences";
				}
				preferences = context.getSharedPreferences(namePreferences, mode);
				editor = preferences.edit();
		}

		public static AdvancedPreferences getAdvancedPreferences(Context context,
				String namePreferences, int mode) {

				if (advancedPreferences == null) {
						advancedPreferences = new AdvancedPreferences(context,
								namePreferences, mode);
				}

				return advancedPreferences;
		}

		public void putObject(String key, Object object) {
				if (key.isEmpty() || key == null) {
						throw new IllegalArgumentException("key is empty or null");
				}

				String obj = GSON.toJson(object);

				if (obj == null || obj.isEmpty()) {
						editor.remove(key);
				} else {
						editor.putString(key, obj);
				}
		}

		public void commit() {
				editor.commit();
		}

		public <T> T getObject(String key, Class<T> a) {

				String gson = preferences.getString(key, null);
				if (gson == null) {
						return null;
				} else {
						try {
								return GSON.fromJson(gson, a);
						} catch (Exception e) {
								throw new IllegalArgumentException("Object storaged with key " + key + " is instanceof other class");
						}
				}
		}

}
