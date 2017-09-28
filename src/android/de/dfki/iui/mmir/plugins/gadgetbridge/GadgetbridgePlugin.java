package de.dfki.iui.mmir.plugins.gadgetbridge;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.AppBlacklistActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureAlarms;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.DbManagementActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.DebugActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.DiscoveryActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPairingActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPreferencesActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 *
 */
public class GadgetbridgePlugin extends CordovaPlugin {

  public static final String ACTION_START_CONTROL_CENTER = "ControlCenterv2";
  public static final String ACTION_START_SETTINGS = "SettingsActivity";
  public static final String ACTION_START_MI_BAND_SETTINGS = "MiBandPreferencesActivity";
  public static final String ACTION_START_BLACKLIST = "AppBlacklistActivity";
  public static final String ACTION_START_DEBUG = "DebugActivity";
  public static final String ACTION_START_DB_MANAGEMENT = "DbManagementActivity";
  public static final String ACTION_START_DISCOVERY = "DiscoveryActivity";
  public static final String ACTION_START_PARING = "MiBandPairingActivity";
  public static final String ACTION_START_ALARMS = "ConfigureAlarms";

  public static final String ACTION_START_CHARTS = "ChartsActivity";
  public static final String ACTION_START_ALARM = "AlarmDetails";

  public static final String ACTION_BATTERY_LEVEL = "battery_level";
  public static final String ACTION_SYNCHRONIZE_DATA = "sync";
  public static final String ACTION_RETRIEVE_DATA = "retrieve";

  static final String PLUGIN_NAME = GadgetbridgePlugin.class.getSimpleName();
  private static final String NAME_SYNC_TASK = "busy_task_fetch_activity_data";

  private DeviceManager _deviceManager;
  private GBDevice _device;

  /* locking object for synchronized/thread-safe access to _pendingResults:
   * wrap every access to _pendingResults with this locker!
   */
  private Object _pendingResultLock = new Object();
  private LinkedList<AsyncDeviceResult> _pendingResults = new LinkedList<AsyncDeviceResult>();

  private Timer _pendingResultTimeoutTimer;
  private Object _pendingResultTimeoutTaskLock = new Object();
  private TimerTask _pendingResultTimeoutTask;
  private static final long RESULT_TIMEOUT = 5000l;//5 sec

  /**
   * Helper class representing PluginResults that are pending, i.e. waiting on
   * on updated device information.
   * <p>
   * Add to <code>_pendingResults</code>:
   * <pre>
   *     synchronized(_pendingResultLock){
   *         _pendingResults.add(new AsyncDeviceResult(theCallbackContext){
   *              @Override
   *              public boolean sendResult(GBDevice device) {
   *
   *                  //check if device has desired information etc. ...
   *
   *              }
   *         });
   *     }
   * </pre>
   */
  private abstract class AsyncDeviceResult {

    protected final long timestamp;
    protected final long timeout;
    protected CallbackContext callbackContext;
    protected final String description;

    public AsyncDeviceResult(CallbackContext callbackContext, String description) {
      this(callbackContext, description, -1l);
    }

    public AsyncDeviceResult(CallbackContext callbackContext, String description, long timeout) {
      this.timestamp = System.currentTimeMillis();
      this.timeout = timeout > 0 ? timeout : RESULT_TIMEOUT;
      this.callbackContext = callbackContext;
      this.description = description;
    }

    /**
     * Implementations should first check (efficiently!) if <code>device</code> contains
     * the needed information and if not, immediately return <code>false</code>.
     * <p>
     * If the <code>device</code> contains the information, a PluginResult can be sent
     * via <code>this.callbackContext</code>.
     * <p>
     * If the method returns <code>false</code>, the AsnycDeviceResult
     * was <strong>not</strong> consumed and should stay active
     * (e.g. no PluginResult was sent or callbackContext was kept open).
     *
     * @param device the device that was updated
     * @return true, if this AsyncDeviceResult was consumed
     */
    abstract public boolean sendResult(GBDevice device);
  }

  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (GBApplication.ACTION_QUIT.equals(action)) {

        LOG.d(PLUGIN_NAME, "BR.received notification: ACTION_QUIT");

      } else if (GBDevice.ACTION_DEVICE_CHANGED.equals(action)) {

        final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (LOG.isLoggable(LOG.DEBUG))
          LOG.d(PLUGIN_NAME, "BR.received notification: ACTION_DEVICE_CHANGED [task " + device.getBusyTask() + "| state " + device.getState() + "] -> " + device);

        if (device != null) {

          _device = device;

          cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {

              synchronized (_pendingResultLock) {
                if (_pendingResults.size() > 0) {

                  stopCheckPendingTimeout();

                  Iterator<AsyncDeviceResult> it = _pendingResults.iterator();
                  AsyncDeviceResult asyncResult;
                  long now = System.currentTimeMillis();
                  while (it.hasNext()) {
                    boolean timedOut = false;
                    asyncResult = it.next();
                    if (asyncResult.sendResult(device) || (timedOut = now - asyncResult.timestamp > asyncResult.timeout)) {
                      if (timedOut) {
                        doSendTimeoutError(asyncResult.callbackContext, "Could not " + asyncResult.description);
                      }
                      it.remove();
                    }
                  }

                  startCheckPendingTimeout();
                }

              }//END: synchronized()

            }//END: run(){...

          });//END: execute(...

        }
      } else if (GB.ACTION_DISPLAY_MESSAGE.equals(action)) {
        LOG.d(PLUGIN_NAME, "BR.received notification: ACTION_DISPLAY_MESSAGE");
//                String message = intent.getStringExtra(GB.DISPLAY_MESSAGE_MESSAGE);
//                int severity = intent.getIntExtra(GB.DISPLAY_MESSAGE_SEVERITY, GB.INFO);
//                addMessage(message, severity);
      }
    }
  };


  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    _pendingResultTimeoutTimer = new Timer();

    IntentFilter filter = new IntentFilter();
    filter.addAction(GBApplication.ACTION_QUIT);
    filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
    filter.addAction(GB.ACTION_DISPLAY_MESSAGE);
    LocalBroadcastManager.getInstance(cordova.getActivity()).registerReceiver(mReceiver, filter);

//        //TODO add a callable init-method -> check there, if it is run the first time
//        Prefs prefs = GBApplication.getPrefs();
//        if (prefs.getBoolean("firstrun", true)) {
//          prefs.getPreferences().edit().putBoolean("firstrun", false).apply();
//          Intent enableIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
//          startActivity(enableIntent);
//
//        }

    GBApplication.deviceService().start();
    GBApplication.deviceService().requestDeviceInfo();
  }

  /**
   * @param action          the name of the "function" that was called by the JavaScript code
   * @param args            the arguments that were provided the JavaScript invocation (may be empty)
   * @param callbackContext the object for triggering the JavaScript's callback functions
   */
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

    boolean isValidAction = true;

    try {//FIXME DEBUG

      LOG.i(PLUGIN_NAME + "_DEBUG", String.format("action '%s' with arguments: %s)", action, args.toString(2)));

    } catch (Exception e) {
    }

    if (ACTION_BATTERY_LEVEL.equals(action)) {

      long timeout;

      try {

        if (args.length() > 0) {
          timeout = args.getInt(0);
        } else {
          timeout = RESULT_TIMEOUT;
        }


      } catch (JSONException e) {

        String errorMessage = "Invalid argument for timeout (using default instead): " + e;
        LOG.e(PLUGIN_NAME, errorMessage, e);
        timeout = RESULT_TIMEOUT;
      }

      this.getBatteryLevel(callbackContext, timeout);

    } else if (ACTION_SYNCHRONIZE_DATA.equals(action)) {

      long timeout;

      try {

        if (args.length() > 0) {
          timeout = args.getInt(0);
        } else {
          timeout = 12 * RESULT_TIMEOUT;//data transfer could take long -> increased default timeout
        }


      } catch (JSONException e) {

        String errorMessage = "Invalid argument for timeout (using default instead): " + e;
        LOG.e(PLUGIN_NAME, errorMessage, e);
        timeout = 12 * RESULT_TIMEOUT;//data transfer could take long -> increased default timeout
      }

      this.synchronizeData(callbackContext, timeout);

    } else if (ACTION_RETRIEVE_DATA.equals(action)) {

      this.retrieveData(callbackContext);

    } else {


      if (!start(action, callbackContext)) {
        isValidAction = false;
      }
    }

    return isValidAction;

  }

  @Override
  public void onNewIntent(Intent intent) {

    LOG.d(PLUGIN_NAME, "onNewIntent: " + intent);//DEBUG

    super.onNewIntent(intent);
  }

  /**
   * Handels invocation of views/activities in the Gadgetbridge app.
   *
   * @param target          the class name of the targeted view/activity
   * @param callbackContext the callback context for sending the plugin result
   * @return TRUE if there is a valid action for <code>target</code>
   */
  protected boolean start(final String target, final CallbackContext callbackContext) {

    Class targetCl;
    GBDevice device = null;
    Alarm alarm = null;

    if (target.equals(ACTION_START_CONTROL_CENTER)) {
      targetCl = ControlCenterv2.class;

    } else if (target.equals(ACTION_START_SETTINGS)) {
      targetCl = SettingsActivity.class;

    } else if (target.equals(ACTION_START_MI_BAND_SETTINGS)) {
      targetCl = MiBandPreferencesActivity.class;

    } else if (target.equals(ACTION_START_BLACKLIST)) {
      targetCl = AppBlacklistActivity.class;

    } else if (target.equals(ACTION_START_DEBUG)) {
      targetCl = DebugActivity.class;

    } else if (target.equals(ACTION_START_DB_MANAGEMENT)) {
      targetCl = DbManagementActivity.class;

    } else if (target.equals(ACTION_START_DISCOVERY)) {
      targetCl = DiscoveryActivity.class;

    } else if (target.equals(ACTION_START_PARING)) {
      targetCl = MiBandPairingActivity.class;

    } else if (target.equals(ACTION_START_ALARMS)) {
      targetCl = ConfigureAlarms.class;

    } else if (target.equals(ACTION_START_CHARTS)) {
      targetCl = ChartsActivity.class;
      device = getDevice();

      if (device == null) {

        targetCl = null;

        String msg = "Could not start activity \"" + target + "\": ";
        doSendNoDeviceError(callbackContext, msg);

        return true;////////////// EARLY EXIT ////////////////////////
      }

    }
//        else if (target.equals(ACTION_START_ALARM)) {
//            targetCl = AlarmDetails.class;
//
//            //avoidSendAlarmsToDevice = true;
//            intent.putExtra("alarm", alarm);
//            intent.putExtra(GBDevice.EXTRA_DEVICE, getDevice());
//            //startActivityForResult(startIntent, REQ_CONFIGURE_ALARM);
//
//
//        }
    else {
      targetCl = null;
    }

    if (targetCl == null) {
      String msg = "Requested unknown activity for starting: '" + target + "'";
      LOG.e(PLUGIN_NAME, msg);
      callbackContext.error(msg);//TODO send errorCode / normalize error?
      return false;////////////// EARLY EXIT ////////////////////////
    }

    try {

      Intent intent = new Intent(this.cordova.getActivity(), targetCl);
      if (device != null) {
        intent.putExtra(GBDevice.EXTRA_DEVICE, device);
      }
      if (alarm != null) {
        intent.putExtra("alarm", alarm);
      }

      this.cordova.getActivity().startActivity(intent);
      callbackContext.success();

    } catch (Exception exc) {
      String msg = "Could not start activity \"" + target + "\"";
      LOG.e(PLUGIN_NAME, msg, exc);
      callbackContext.error(msg + ": " + exc);//TODO send errorCode / normalize error?
    }

    return true;
  }

  protected void getBatteryLevel(CallbackContext callbackContext, long timeout) {

    GBDevice d = getDevice();
    if (d != null) {

      if (d.isBusy()) {
        //TODO should this be queued instead of triggering an error here?
        callbackContext.error("device busy: '" + d.getBusyTask() + "'");
        return;///////////////////// EARLY EXIT /////////////////////
      }

      if (d.isConnected()) {
        callbackContext.success(d.getBatteryLevel());
      } else {

        synchronized (_pendingResultLock) {

          //register callback, listening to changes in the GBDevice
          _pendingResults.add(new AsyncDeviceResult(callbackContext, "Battery Level", timeout) {
            @Override
            public boolean sendResult(GBDevice device) {
              int level = device.getBatteryLevel();
              LOG.d(PLUGIN_NAME, "ASYNC device battery: " + level);//DEBUG
              if (level != -1) {
                this.callbackContext.success(level);
                return true;
              } else {
                return false;
              }
            }
          });
        }

        if (GBDevice.State.NOT_CONNECTED.equals(d.getState())) {
          GBApplication.deviceService().connect(d);
        }
        GBApplication.deviceService().requestDeviceInfo();

      }
    } else {
      doSendNoDeviceError(callbackContext, "Could not determine battery level.");
    }

  }

  protected void synchronizeData(CallbackContext callbackContext, long timeout) {

    GBDevice d = getDevice();
    if (d != null) {

      if (d.isBusy()) {
        //TODO should this be queued instead of triggering an error here?
        callbackContext.error("device busy: '" + d.getBusyTask() + "'");
        return;///////////////////// EARLY EXIT /////////////////////
      }

      boolean isConnecting = !d.isInitialized();
      if (GBDevice.State.NOT_CONNECTED.equals(d.getState())) {
        //any other state of NOT_CONNECTED means, that the device is in the process of connecting (or is connected)
        GBApplication.deviceService().connect(d);
      }

      GBApplication.deviceService().onFetchActivityData();
      final String taskDesc = d.getBusyTask();
      synchronized (_pendingResultLock) {

        //if there is a busy-task set -> start listing to when the busy-task is reset again
//            if(taskDesc != null) {

        final boolean checkConnecting = isConnecting;

        //register callback, listening to changes in the GBDevice & its busy-task
        _pendingResults.add(new AsyncDeviceResult(callbackContext, "Synchronize Data", timeout) {

          private boolean _isCheckConnecting = checkConnecting;//if need to wait for connection first
          private boolean _isSettingTaskName = false;
          private boolean _isSyncStarted = false;
          private String _taskName = taskDesc;//<- NULL, if waiting for connection (will be set after connecting)

          @Override
          public boolean sendResult(GBDevice device) {

            String syncTaskName = getSyncTaskName();

            if (_isCheckConnecting || _taskName == null) {
              if (device.isInitialized()) {
                LOG.d(PLUGIN_NAME, "ASYNC synchronize data: connected, starting to sync... ");//DEBUG
                _isCheckConnecting = false;

                if (_taskName == null) {
//                          _taskName = device.getBusyTask();
//                          if(_taskName == null){
//                            //set marker, that we should try to retrieve task-name in next update
//                            _isSettingTaskName = true;
//                          }
                  String taskName = device.getBusyTask();
                  if (syncTaskName.length() > 0) {
                    if (syncTaskName.equals(taskName)) {
                      _taskName = taskName;
                      _isSyncStarted = true;
                    }
                  } else if (taskName != null) {
                    _isSyncStarted = true;
                    //in case we do not know the name for  the sync-task:
                    // use heuristic that the first non-null name is the sync-task name
                    if (_taskName == null) {
                      //actually try to use the task-name from next cycle:
                      _isSettingTaskName = false;
                    }
                    _taskName = taskName;
                  }
                  LOG.d(PLUGIN_NAME, "ASYNC synchronize data: set task to '" + _taskName + "' (1)... ");//DEBUG
                } else
                  LOG.d(PLUGIN_NAME, "ASYNC synchronize data: continuing with task '" + _taskName + "'... ");//DEBUG
              } else
                LOG.d(PLUGIN_NAME, "ASYNC synchronize data: connecting... ");//DEBUG

              return false;//<- continue listening (for end of sync-task)

            } else {

              String taskName = device.getBusyTask();
              if (_isSettingTaskName && taskName != null) {
                _taskName = taskName;
                LOG.d(PLUGIN_NAME, "ASYNC synchronize data: set task to '" + _taskName + "' (2)... ");//DEBUG
              }

              if (_taskName == null) {
                LOG.e(PLUGIN_NAME, "ASYNC synchronize data: invalid sync state: should have busy-task set, but is NULL ", device);//DEBUG
                this.callbackContext.error("invalid sync state: should have busy-task set, but is NULL");//FIXME normalize error
                return true;//<- remove this from pending task list
              }

              LOG.d(PLUGIN_NAME, "ASYNC synchronize data: " + device.getBusyTask());//DEBUG
              if (!_taskName.equals(device.getBusyTask())) {
                this.callbackContext.success();//TODO send timestamp of last activity-data sample?
                return true;
              } else {
                return false;
              }
            }

          }
        });

//            }
      }

//          //if there was no busy-task set: send result immediately
//          if(taskDesc == null){
//            callbackContext.success();
//          }

    } else {
      doSendNoDeviceError(callbackContext, "Could not start data synchronization.");
    }

  }

//    private void getData(final CallbackContext callbackContext, long timeout){
//      cordova.getThreadPool().execute(new Runnable() {
//        public void run() {
//          try {
//            GBDevice device = getDevice();
//            DBHandler db = GBApplication.acquireDB();
//            DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
//            SampleProvider<? extends ActivitySample> provider = coordinator.getSampleProvider(device, db.getDaoSession());
//            provider.getAllActivitySamples(tsFrom, tsTo);
//
//            callbackContext.success();
//          } catch (Exception e) {
//            LOG.e(PLUGIN_NAME, "failed to get data", e);
//          }
//        }
//      });
//    }

  protected AsyncTask refreshTask;

  protected void retrieveData(CallbackContext callbackContext) {
    GBDevice device = getDevice();
    if (device != null) {
//        mChartDirty = false;
//        updateDateInfo(getStartDate(), getEndDate());
      if (refreshTask != null && refreshTask.getStatus() != AsyncTask.Status.FINISHED) {
        refreshTask.cancel(true);//TODO should this be really canceled?
      }
      refreshTask = new DataRetrievalTask("Retrieving Data", cordova.getActivity(), callbackContext).execute();
    }
  }

  protected class DataRetrievalTask extends DBAccess {

    private CallbackContext callbackContext;
    private List<? extends ActivitySample> samples;

    public DataRetrievalTask(String task, Context context, CallbackContext callbackContext) {
      super(task, context);
      this.callbackContext = callbackContext;
    }

    @Override
    protected void doInBackground(DBHandler db) {
      GBDevice device = getDevice();
      if (device != null) {
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
        SampleProvider<? extends ActivitySample> provider = coordinator.getSampleProvider(device, db.getDaoSession());
        samples = provider.getAllActivitySamples(0, toTimestamp(new Date()));//FIXME this retrieves all available sample up to now!!!

      } else {
        cancel(true);
        doSendNoDeviceError(callbackContext, "Could not retrieve data.");
      }
    }

    @Override
    protected void onPostExecute(Object o) {
      super.onPostExecute(o);
      Activity activity = cordova.getActivity();
      if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
        if (samples != null) {
          JSONArray result = toJson(samples);
          callbackContext.success(result);
        } else {
          callbackContext.error("no samples");//TODO should this really be an error, or should no-samples be a valid response?
        }
      } else {
        LOG.i(PLUGIN_NAME, "Not retrieving data because activity is not available anymore");
      }
    }

    private JSONArray toJson(List<? extends ActivitySample> list) {
      JSONArray l = new JSONArray();
      for (ActivitySample s : list) {
        try {
          JSONObject data = toJson(s);
          l.put(data);
        } catch (JSONException e) {
          LOG.e(PLUGIN_NAME, "Failed to create JSONObject for sample", e);
        }
      }
      return l;
    }

    private JSONObject toJson(ActivitySample sample) throws JSONException {
      JSONObject o = new JSONObject();

//      public static final int TYPE_NOT_MEASURED = -1;
//      public static final int TYPE_UNKNOWN = 0;
//      public static final int TYPE_ACTIVITY = 1;
//      public static final int TYPE_LIGHT_SLEEP = 2;
//      public static final int TYPE_DEEP_SLEEP = 4;
//      public static final int TYPE_NOT_WORN = 8;
      int kind = sample.getKind();
      int timestamp = sample.getTimestamp();
      switch (kind) {
        case -1://TYPE_NOT_MEASURED
          LOG.w(PLUGIN_NAME, "sample NOT_MEASURED at " + timestamp);
          return null;////////////// EARLY EXIT ////////////////
        case 0://TYPE_UNKNOWN
          LOG.w(PLUGIN_NAME, "sample has UNKNOWN_TYPE at " + timestamp);
          break;
        case 1://TYPE_ACTIVITY
          o.put("activity", (double) sample.getIntensity());
          break;
        case 2://TYPE_LIGHT_SLEEP
          o.put("sleep1", sample.getIntensity());
          break;
        case 4://TYPE_LIGHT_DEEP
          o.put("sleep2", sample.getIntensity());
          break;
        case 8://TYPE_NOT_WORN
          o.put("notWorn", true);
          break;
      }

      o.put("timestamp", timestamp);

      int steps = sample.getSteps();
      if (steps > 0) {
        o.put("steps", steps);
      }

      int heartRate = sample.getHeartRate();
      if (heartRate > 0 && heartRate < 255) {
        o.put("heartRate", heartRate);
      }

      return o;
    }
  }


  private GBDevice getDevice() {

    if (this._deviceManager == null) {
      this._deviceManager = ((GBApplication) GBApplication.getContext()).getDeviceManager();
    }

    if (this._deviceManager.getSelectedDevice() == null) {

      List<GBDevice> deviceList = this._deviceManager.getDevices();
      if (deviceList.size() > 0) {
        GBDevice device = deviceList.get(0);//GET FIRST ONE ... TODO how should multiple devices be handled?
        GBApplication.deviceService().connect(device);
        this._device = device;
      }

    }
    return this._device;
  }

  protected static void doSendTimeoutError(CallbackContext callbackContext, String message) {
    String msg = message + " timeout";
    LOG.e(PLUGIN_NAME, msg);
    callbackContext.error(msg);//TODO send errorCode / normalize error
  }

  protected static void doSendNoDeviceError(CallbackContext callbackContext, String message) {
    String msg = message + " No device available";
    LOG.e(PLUGIN_NAME, msg);
    callbackContext.error(msg);//TODO send errorCode / normalize error
  }

  protected void startCheckPendingTimeout() {

    synchronized (_pendingResultTimeoutTaskLock) {
      if (_pendingResultTimeoutTask == null) {
        _pendingResultTimeoutTask = createCheckPendingTimeoutTask();
        _pendingResultTimeoutTimer.schedule(_pendingResultTimeoutTask, RESULT_TIMEOUT, RESULT_TIMEOUT);
      }
    }
  }

  protected void stopCheckPendingTimeout() {

    synchronized (_pendingResultTimeoutTaskLock) {
      if (_pendingResultTimeoutTask != null) {
        _pendingResultTimeoutTask.cancel();
        _pendingResultTimeoutTask = null;
      }
    }

    _pendingResultTimeoutTimer.purge();
  }

  protected TimerTask createCheckPendingTimeoutTask() {

    return new TimerTask() {
      @Override
      public void run() {

        synchronized (_pendingResultLock) {

          if (_pendingResults.size() > 0) {
            Iterator<AsyncDeviceResult> it = _pendingResults.iterator();
            AsyncDeviceResult asyncResult;
            long now = System.currentTimeMillis();
            while (it.hasNext()) {
              asyncResult = it.next();
              if (now - asyncResult.timestamp > asyncResult.timeout) {
                doSendTimeoutError(asyncResult.callbackContext, "Could not " + asyncResult.description);
                it.remove();
              }
            }
          }

        }//END: synchronized()

      }//END: run(){...

    };//END: new TimerTask(){...
  }


  ///////////////////////////////// HELPER
  private int toTimestamp(Date date) {
    return (int) ((date.getTime() / 1000));
  }

  private String getSyncTaskName() {
    Activity activity = this.cordova.getActivity();
    int strId = activity.getResources().getIdentifier(NAME_SYNC_TASK, "string", activity.getApplicationInfo().packageName);
    if (strId == 0) {
      LOG.e(PLUGIN_NAME, "could not retrieve name for sync task, invalid ID " + NAME_SYNC_TASK + "?");
    }
    return activity.getString(strId);
  }


//  /////////////////////////////////// APP STATE BEHAVIOR /////////////////////////////////////////////////////////

  @Override
  public void onResume(boolean multitasking) {
    startCheckPendingTimeout();
    super.onResume(multitasking);
  }

  @Override
  public void onPause(boolean multitasking) {
    stopCheckPendingTimeout();
    super.onPause(multitasking);
  }

  @Override
  public void onDestroy() {
    stopCheckPendingTimeout();
    _pendingResultTimeoutTimer.cancel();
    super.onDestroy();
  }
}
