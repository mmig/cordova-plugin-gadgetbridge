package de.dfki.iui.mmir.plugins.gadgetbridge.db;

import java.util.LinkedList;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import de.dfki.iui.mmir.plugins.gadgetbridge.utils.ResultUtils;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

public class RemovalTask extends DBAccess {

  private static final String TAG_NAME = "GBDataRemoval";

  private DbTaskManager taskManager;

  private CallbackContext callbackContext;

  private int start;
  private int end;
  private boolean removeAll;

  private List<ActivitySample> removedSamples;
  private List<String> errors;
  private Exception lastError;

  public RemovalTask(DbTaskManager taskManager, String task, int startTimestamp, int endTimestamp, boolean isRemoveAll, Context context, CallbackContext callbackContext) {
    super(task, context);
    this.taskManager = taskManager;
    this.start = startTimestamp;
    this.end = endTimestamp;
    this.removeAll = isRemoveAll;
    this.callbackContext = callbackContext;
    this.removedSamples = new LinkedList<ActivitySample>();
  }

  @Override
  protected void doInBackground(DBHandler db) {
    GBDevice device = taskManager.getDevice();
    if (device != null) {
      DaoSession session = db.getDaoSession();

      if(removeAll){

        try {

          session.deleteAll(MiBandActivitySample.class);

        } catch (Exception e) {

          String msg = "ERROR removing all samples from Db";
          LOG.e(TAG_NAME, msg, e);

          if(errors == null){
            errors = new LinkedList<String>();
          }
          errors.add(msg +  " " + e);
          lastError = e;
        }

      } else {

        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
        SampleProvider<? extends ActivitySample> provider = coordinator.getSampleProvider(device, session);

        List<? extends ActivitySample> samples = provider.getAllActivitySamples(start, end);

        if(samples != null) {
          for (ActivitySample s : samples) {
            try {

              ((MiBandActivitySample) s).delete();
              removedSamples.add(s);

            } catch (Exception e) {

              String msg = "ERROR while removing sample from Db " + s;
              LOG.e(TAG_NAME, msg, e);

              if(errors == null){
                errors = new LinkedList<String>();
              }
              errors.add(msg +  " " + e);
              lastError = e;
            }
          }
        }
      }

    } else {
      cancel(true);
      ResultUtils.sendNoDeviceError(callbackContext, "Could not access data.");
    }
  }

  @Override
  protected void onPostExecute(Object o) {
    super.onPostExecute(o);
    Activity activity = taskManager.getCordova().getActivity();
    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {

      boolean hasErrors = errors == null;
      if(removedSamples.size() > 0 || hasErrors){


        JSONObject result = new JSONObject();
        ResultUtils.addToJson(result, "message", hasErrors? "errors occured" : "success");

        if(errors != null){
          JSONArray l = new JSONArray();
          for(String s : errors){
            l.put(s);
          }
          ResultUtils.addToJson(result, "errors", l);
        }

        if(!removeAll){
          JSONArray rmList = ResultUtils.toJson(removedSamples);
          ResultUtils.addToJson(result, "removed", rmList);
        }

        callbackContext.success(result);

      } else {

        String msg = lastError != null? " (last error) " + lastError.getMessage() : "error(s) while removing data.";
        callbackContext.error("Could not remove any data: "+msg);
      }

      taskManager.executeNextDbTask(DbTaskType.REMOVE);

    } else {
      LOG.i(TAG_NAME, "Not retrieving data because Cordova activity is not available anymore");
    }
  }
}
