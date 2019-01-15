package de.dfki.iui.mmir.plugins.gadgetbridge.utils;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.dfki.iui.mmir.plugins.gadgetbridge.DeviceInfoType;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class ResultUtils {

  private static final String TAG_NAME = "GBPluginUtils";

  /** field type: double */
  public static final String FIELD_ACTIVITY = "activity";
  /** field type: float */
  public static final String FIELD_LIGHT_SLEEP = "sleep1";
  /** field type: float */
  public static final String FIELD_DEEP_SLEEP = "sleep2";
  /** field type: boolean */
  public static final String FIELD_NOT_WORN = "notWorn";
  /** field type: int (10 digits) */
  public static final String FIELD_TIMESTAMP = "timestamp";
  /** field type: int */
  public static final String FIELD_STEPS = "steps";
  /** field type: int [1,254] */
  public static final String FIELD_HEART_RATE = "heartRate";
  /** field type: int */
  public static final String FIELD_RAW_INTENSITY = "raw";
  public static final String INFO_FIELD_ADDRESS = "address";
  public static final String INFO_FIELD_NAME = "name";
  public static final String INFO_FIELD_MODEL = "model";
  public static final String INFO_FIELD_TYPE = "type";
  public static final String INFO_FIELD_FIRMWARE = "firmware";
  public static final String INFO_FIELD_STATE = "state";
  public static final String INFO_FIELD_BAT_LEVEL = "level";
  public static final String INFO_FIELD_BAT_THRESHOLD = "threshold";
  public static final String INFO_FIELD_BAT_STATE = "state";

  public static JSONObject addToJson(JSONObject obj, String name, Object value) {
    try {

      if(value instanceof Collection){

        //TODO support array-type recursively?
        JSONArray list = new JSONArray();
        Iterator it = ((Collection)value).iterator();
        while(it.hasNext()){
          list.put(it.next());
        }
        obj.putOpt(name, list);

      } else {
        obj.putOpt(name, value);
      }

    } catch (JSONException e) {
      LOG.e(TAG_NAME, String.format("Could not add JSON field %s with value %s", name, value), e);
    }
    return obj;
  }

  public static JSONArray toJson(List<? extends ActivitySample> list) {
    JSONArray l = new JSONArray();
    for (ActivitySample s : list) {
      try {
        JSONObject data = toJson(s);
        l.put(data);
      } catch (JSONException e) {
        LOG.e(TAG_NAME, "Failed to create JSONObject for sample", e);
      }
    }
    return l;
  }


  public static JSONObject toJson(GBDevice deviceInfo, DeviceInfoType type) {
    JSONObject o = new JSONObject();
    try {

      //the "ID":
      o.putOpt(INFO_FIELD_ADDRESS, deviceInfo.getAddress());

      if(DeviceInfoType.INFO.equals(type)){

        //full device info
        o.putOpt(INFO_FIELD_NAME, deviceInfo.getName());
        o.putOpt(INFO_FIELD_MODEL, deviceInfo.getModel());
        o.putOpt(INFO_FIELD_TYPE, deviceInfo.getType().toString());
        o.putOpt(INFO_FIELD_FIRMWARE, deviceInfo.getFirmwareVersion());
        o.putOpt(INFO_FIELD_STATE, deviceInfo.getState().toString());

      } else if(DeviceInfoType.CONNECTION_STATE.equals(type)){

        //device connection state
        o.putOpt(INFO_FIELD_STATE, deviceInfo.getState().toString());

      } else if(DeviceInfoType.BATTERY_INFO.equals(type)){

        //device battery state
        o.putOpt(INFO_FIELD_BAT_LEVEL, deviceInfo.getBatteryLevel());
        o.putOpt(INFO_FIELD_BAT_THRESHOLD, deviceInfo.getBatteryThresholdPercent());
        o.putOpt(INFO_FIELD_BAT_STATE, deviceInfo.getBatteryState().toString());
      }

    } catch (JSONException e) {
      LOG.e(TAG_NAME, "Failed to create JSONObject for sample", e);
    }
    return o;
  }

  public static JSONObject toJson(ActivitySample sample) throws JSONException {
    JSONObject o = new JSONObject();

    //			public static final int TYPE_NOT_MEASURED = -1;
    //			public static final int TYPE_UNKNOWN = 0;
    //			public static final int TYPE_ACTIVITY = 1;
    //			public static final int TYPE_LIGHT_SLEEP = 2;
    //			public static final int TYPE_DEEP_SLEEP = 4;
    //			public static final int TYPE_NOT_WORN = 8;
    int kind = sample.getKind();
    int timestamp = sample.getTimestamp();
    int steps = sample.getSteps();
    int heartRate = sample.getHeartRate();
    switch (kind) {
    case -1://TYPE_NOT_MEASURED
      LOG.i(TAG_NAME, "sample NOT_MEASURED at " + timestamp);
      return null;////////////// EARLY EXIT ////////////////
    case 0://TYPE_UNKNOWN (may have step or heartrate data)
      if(LOG.isLoggable(LOG.DEBUG) && steps == 0 && heartRate == 0){
        LOG.w(TAG_NAME, "sample has UNKNOWN_TYPE at " + timestamp);
      }
      break;
    case 1://TYPE_ACTIVITY
      o.put(FIELD_ACTIVITY, (double) sample.getIntensity());
      break;
    case 2://TYPE_LIGHT_SLEEP
      o.put(FIELD_LIGHT_SLEEP, sample.getIntensity());
      break;
    case 4://TYPE_DEEP_SLEEP
      o.put(FIELD_DEEP_SLEEP, sample.getIntensity());
      break;
    case 8://TYPE_NOT_WORN
      o.put(FIELD_NOT_WORN, true);
      break;
    }

    o.put(FIELD_RAW_INTENSITY, sample.getRawIntensity());

    o.put(FIELD_TIMESTAMP, timestamp);

    if (steps > 0) {
      o.put(FIELD_STEPS, steps);
    }

    if (heartRate > 0 && heartRate < 255) {
      o.put(FIELD_HEART_RATE, heartRate);
    }

    return o;
  }

  public static void sendTimeoutError(CallbackContext callbackContext, String message) {
		String msg = message + " timeout";
		LOG.e(TAG_NAME, msg);
		callbackContext.error(msg);//TODO send errorCode / normalize error
	}

  public static void sendNoDeviceError(CallbackContext callbackContext, String message) {
		String msg = message + " No device available";
		LOG.e(TAG_NAME, msg);
		callbackContext.error(msg);//TODO send errorCode / normalize error
	}
}
