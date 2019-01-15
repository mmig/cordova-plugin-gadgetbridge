package de.dfki.iui.mmir.plugins.gadgetbridge.db;

import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.LOG;
import org.json.JSONArray;

import android.app.Activity;
import android.content.Context;
import de.dfki.iui.mmir.plugins.gadgetbridge.utils.ResultUtils;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

public class RetrievalTask extends DBAccess {

  private static final String TAG_NAME = "GBDataRetrieval";

  private DbTaskManager taskManager;
  private CallbackContext callbackContext;
  private List<? extends ActivitySample> samples;

  private int start;
  private int end;

  public RetrievalTask(DbTaskManager taskManager, String task, int startTimestamp, int endTimestamp, Context context, CallbackContext callbackContext) {
    super(task, context);
    this.taskManager = taskManager;
    this.start = startTimestamp;
    this.end = endTimestamp;
    this.callbackContext = callbackContext;
  }

  @Override
  protected void doInBackground(DBHandler db) {
    GBDevice device = this.taskManager.getDevice();
    if (device != null) {
      DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
      SampleProvider<? extends ActivitySample> provider = coordinator.getSampleProvider(device, db.getDaoSession());

      samples = provider.getAllActivitySamples(start, end);

    } else {
      cancel(true);
      ResultUtils.sendNoDeviceError(callbackContext, "Could not retrieve data.");
    }
  }

  @Override
  protected void onPostExecute(Object o) {
    super.onPostExecute(o);
    Activity activity = taskManager.getCordova().getActivity();
    if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
      if (samples != null) {
        JSONArray result = ResultUtils.toJson(samples);
        callbackContext.success(result);
      } else {
        callbackContext.error("no samples");//TODO should this really be an error, or should no-samples be a valid response?
      }

      taskManager.executeNextDbTask(DbTaskType.RETRIEVE);

    } else {
      LOG.i(TAG_NAME, "Not retrieving data because Cordova activity is not available anymore");
    }
  }
}
