package de.dfki.iui.mmir.plugins.gadgetbridge.utils;


import android.app.Activity;
import android.content.SharedPreferences;

import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SettingsUtil {

	private static final String TAG = "GadgetbridgePluginSettings";

	private static final String TIMESTAMP_NAME_SUFFIX = "TimeMillis";

	public static final String NAME_SYNC_TASK = "busy_task_fetch_activity_data";
	public static final String NAME_BUTTON_BROADCAST = "mi2_prefs_button_press_broadcast_default_value";


	public static final String NAME_ENABLE_CALL_HANDLER = "enableCallHandler";
	public static final String NAME_ENABLE_SMS_HANDLER = "enableSmsHandler";


	private static String _SYNC_TASK_NAME;
	private static String _DEF_BUTTON_PRESS_VAL;

	public static Map<String, ?> getAll(){
		return GBApplication.getPrefs().getPreferences().getAll();
	}

	public static Prefs getPrefs() {
		return GBApplication.getPrefs();
	}

	public static boolean setPref(String name, Object value){
		SharedPreferences.Editor edit = SettingsUtil.getPrefs().getPreferences().edit();
		if(setPref(edit, name, value))
			return edit.commit();

		return false;
	}

	public static Set<String> setPref(JSONObject settings, ApplySettings applier){

		HashSet<String> results = new HashSet<String>();
		SharedPreferences.Editor edit = SettingsUtil.getPrefs().getPreferences().edit();
		String name;
		Object val;
		Iterator<String> it = settings.keys();
		while(it.hasNext()){

			name = it.next();

			try {

				val = settings.get(name);
				if(setPref(edit, name, val)){
					applier.applySettings(name, val);
					results.add(name);
				}

			} catch (JSONException e) {
				LOG.e(TAG, String.format("could not set %s", name), e);
			}
		}

		if(results.size() > 0){

			if(edit.commit()){
				return results;
			} else {
				results.clear();
			}

		}

		return results;
	}

	/////////////////////////////// static helpers //////////////////////////////////////////


	public static void enableCallHandler(boolean enable){
		setPref(NAME_ENABLE_CALL_HANDLER, (Boolean) enable);
	}

	public static void enableSmsHandler(boolean enable){
		setPref(NAME_ENABLE_SMS_HANDLER, (Boolean) enable);
	}

	public static String getSyncTaskName(Activity activity) {

		if(_SYNC_TASK_NAME != null){
			return _SYNC_TASK_NAME;/////////EARLY EXIT //////////////
		}

		int strId = activity.getResources().getIdentifier(NAME_SYNC_TASK, "string", activity.getApplicationInfo().packageName);
		if (strId == 0) {
			LOG.e(TAG, "could not retrieve name for sync task, invalid ID " + NAME_SYNC_TASK + "?");
		}

		_SYNC_TASK_NAME = activity.getString(strId);
		return _SYNC_TASK_NAME;
	}

	public static  String getDefaultButtonPressValue(Activity activity) {

		if(_DEF_BUTTON_PRESS_VAL != null){
			return _DEF_BUTTON_PRESS_VAL;///////////// EARLY EXIT ///////////
		}

		int strId = activity.getResources().getIdentifier(NAME_BUTTON_BROADCAST, "string", activity.getApplicationInfo().packageName);
		if (strId == 0) {
			LOG.e(TAG, "could not retrieve name for button broadcast, invalid ID " + NAME_BUTTON_BROADCAST + "?");
		}

		_DEF_BUTTON_PRESS_VAL = activity.getString(strId);
		return _DEF_BUTTON_PRESS_VAL;
	}

	/////////////////////////////////// private helpers ////////////////////////////////

	private static boolean setPref(SharedPreferences.Editor edit, String name, Object value){

		boolean success = true;
		if(value instanceof JSONArray){

			JSONArray list = (JSONArray) value;
			HashSet<String> set = new HashSet<String>();
			for(int i=list.length()-1; i >= 0; --i){
				set.add(list.opt(i).toString());
			}
			edit.putStringSet(name, set);

		} else if(value instanceof Boolean){
			edit.putBoolean(name, (Boolean) value);
		} else if(value instanceof String){
			edit.putString(name, (String) value);
		} else
			//      if(value instanceof Long){
			//      edit.putLong(name, (Long) value);
			//    } else if(value instanceof Integer){
			//      edit.putInt(name, (Integer) value);
			//    } else if(value instanceof Float){
			//      edit.putFloat(name, (Float) value);
			//    }
			if(value != null){
				//NOTE currently, number values, are stored as strings ... except for timestamps
				//TODO use fixed mapping setting-name -> value-type?
				if(value instanceof Long){//name.endsWith(TIMESTAMP_NAME_SUFFIX)){
					//store timestamps as LONG
					edit.putLong(name, (Long) value);
				} else if(JSONObject.NULL.equals(value)){
					//special case: NULL-value removes the setting
					edit.remove(name);
				} else {
					//default: convert to String
					edit.putString(name, value.toString());
				}
			}else {
				success = false;
			}

		return success;
	}
}
