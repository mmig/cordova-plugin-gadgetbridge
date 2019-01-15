package de.dfki.iui.mmir.plugins.gadgetbridge.db;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import de.dfki.iui.mmir.plugins.gadgetbridge.utils.ResultUtils;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class InsertionTask extends DBAccess {

  private static final String TAG_NAME = "GBDataRetrieval";

  private DbTaskManager taskManager;

  private CallbackContext callbackContext;

  private JSONArray data;
  private boolean success;

  public InsertionTask(DbTaskManager taskManager, String task, JSONArray args, Context context, CallbackContext callbackContext) {
    super(task, context);
    this.taskManager = taskManager;
    this.success = false;
    this.data = args;
    this.callbackContext = callbackContext;
  }

  @Override
  protected void doInBackground(DBHandler db) {
    GBDevice gbDevice = taskManager.getDevice();
    if (gbDevice != null) {

      try {

        //insertion adapted from: nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.operations.FetchActivityOperation
        DBHandler handler = GBApplication.acquireDB();

        DaoSession session = handler.getDaoSession();
        SampleProvider<MiBandActivitySample> sampleProvider = new MiBandSampleProvider(taskManager.getDevice(), session);
        Device device = DBHelper.getDevice(gbDevice, session);
        User user = DBHelper.getUser(session);

        int size = this.data.length();
        MiBandActivitySample[] samples = new MiBandActivitySample[size];

        for (int i=0; i < size; ++i) {

          JSONObject data = this.data.getJSONObject(i);
          MiBandActivitySample sample = sampleProvider.createActivitySample();

          sample.setDevice(device);
          sample.setUser(user);
          sample.setTimestamp(data.getInt(ResultUtils.FIELD_TIMESTAMP));
          sample.setProvider(sampleProvider);

          sample.setRawKind(MiBandSampleProvider.TYPE_ACTIVITY);

          if(data.has(ResultUtils.FIELD_RAW_INTENSITY)){
            sample.setRawIntensity(data.getInt(ResultUtils.FIELD_RAW_INTENSITY));
          }

          if(data.has(ResultUtils.FIELD_ACTIVITY) || data.has(ResultUtils.FIELD_LIGHT_SLEEP) || data.has(ResultUtils.FIELD_DEEP_SLEEP)){

//            double value;//<- FIXM TEST remove after test
            if(data.has(ResultUtils.FIELD_ACTIVITY)){
//              value = data.getDouble(ResultUtils.FIELD_ACTIVITY);//TEST
              sample.setRawKind(MiBandSampleProvider.TYPE_ACTIVITY);
            } else if(data.has(ResultUtils.FIELD_LIGHT_SLEEP)){
//              value = data.getDouble(ResultUtils.FIELD_LIGHT_SLEEP);//TEST
              sample.setRawKind(MiBandSampleProvider.TYPE_LIGHT_SLEEP);
            } else {//if(data.has(FIELD_DEEP_SLEEP)){
//              value = data.getDouble(ResultUtils.FIELD_DEEP_SLEEP);//TEST
              sample.setRawKind(MiBandSampleProvider.TYPE_DEEP_SLEEP);
            }

//            //added extra field FIELD_RAW_INTENSITY instead of "reverse engineering" the value (see code above)
//            //              //HACK need to reverse AbstractMiBandSampleProvider.normalizeIntensity() which is used in getIntensity(): do mult by 180
//            //              sample.setRawIntensity((int)(value * 180.0d));
//            //FIXM TEST:
//            int converted = (int)(value * 180.0d);
//            int raw = sample.getRawIntensity();
//            LOG.i(TAG_NAME, String.format("TEST: intensity to raw: %s (raw: %d | converted: %d)", converted == raw, raw, converted));
          }

          if(data.has(ResultUtils.FIELD_NOT_WORN)){
            sample.setRawKind(MiBandSampleProvider.TYPE_NONWEAR);
          }

          if(data.has(ResultUtils.FIELD_STEPS)){
            sample.setSteps(data.getInt(ResultUtils.FIELD_STEPS));
          }

          if(data.has(ResultUtils.FIELD_HEART_RATE)){
            sample.setHeartRate(data.getInt(ResultUtils.FIELD_HEART_RATE));
          }

          samples[i] = sample;

          //            if (LOG.isDebugEnabled()) {
          ////                        LOG.debug("sample: " + sample);
          //            }
        }

        sampleProvider.addGBActivitySamples(samples);
        this.success = true;

      } catch (Exception ex) {
        //          GB.toast(getContext(), "Error saving activity samples", Toast.LENGTH_LONG, GB.ERROR);
        LOG.e(TAG_NAME, "Could not insert data", ex);
        cancel(true);
        callbackContext.error("Could not insert data.");
      }


    } else {
      cancel(true);
      ResultUtils.sendNoDeviceError(callbackContext, "Could not insert data.");
    }
  }

  @Override
  protected void onPostExecute(Object o) {
    super.onPostExecute(o);
    Activity activity = taskManager.getCordova().getActivity();
    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
      if (this.success) {
        callbackContext.success();
      } else {
        callbackContext.error("errors occured while adding samples");//TODO should this more detailed?
      }
      taskManager.executeNextDbTask(DbTaskType.INSERT);
    } else {
      LOG.i(TAG_NAME, "Not retrieving data because Cordova activity is not available anymore");
    }
  }

}
