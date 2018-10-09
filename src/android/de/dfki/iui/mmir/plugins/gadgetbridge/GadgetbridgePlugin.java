package de.dfki.iui.mmir.plugins.gadgetbridge;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
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
import de.dfki.iui.mmir.plugins.gadgetbridge.GadgetbridgePlugin.DataInsertionTask;
import de.dfki.iui.mmir.plugins.gadgetbridge.GadgetbridgePlugin.DataRemovalTask;
import de.dfki.iui.mmir.plugins.gadgetbridge.GadgetbridgePlugin.DataRetrievalTask;
import de.dfki.iui.mmir.plugins.gadgetbridge.GadgetbridgePlugin.DbTaskType;
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
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPairingActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPreferencesActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
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

	public static final String ACTION_CONNECT = "connect";
	public static final String ACTION_IS_CONNECTED = "is_connected";
	public static final String ACTION_DEVICE_INFO = "device_info";
	public static final String ACTION_BATTERY_LEVEL = "battery_level";
	public static final String ACTION_SYNCHRONIZE_DATA = "sync";
	public static final String ACTION_RETRIEVE_DATA = "retrieve";
	public static final String ACTION_REMOVE_DATA = "remove";
	public static final String ACTION_REMOVE_ALL_DATA = "remove_all";
	public static final String ACTION_ADD_CONNECTION_LISTENER = "on_connect";
	public static final String ACTION_REMOVE_CONNECTION_LISTENER = "off_connect";
	public static final String ACTION_ADD_BUTTON_LISTENER = "on_button";
	public static final String ACTION_REMOVE_BUTTON_LISTENER = "off_button";
	private static final String ACTION_FIRE_NOTIFICATION = "fire_notification";
	private static final String ACTION_CANCEL_NOTIFICATION = "cancel_notification";
	private static final String ACTION_GET_CONFIG = "get_config";
	private static final String ACTION_SET_CONFIG = "set_config";

	private static final String TASK_CONNECTION_STATE_LISTENER = "Connection State Listener";
	private static final String TASK_CONNECTING_DEVICE = "Connecting Device";
	private static final String TASK_BATTERY_LEVEL = "Battery Level";
	private static final String TASK_SYNCHRONIZE_DATA = "Synchronize Data";
	private static final String TASK_RETRIEVING_DATA = "Retrieving Data";
	private static final String TASK_REMOVING_DATA = "Removing Data";
	private static final String TASK_INSERTING_DATA = "Inserting Data";

	static final String PLUGIN_NAME = GadgetbridgePlugin.class.getSimpleName();
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

	private static String _buttonBroadcastName;

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


	private Object _notificationRepeatTaskLock = new Object();
	private TimerTask _notificationRepeatTask;
	private static final long NOTIFICATION_REPEAT_DELAY = 10000l;//10 sec
	private int currentNotificationId = 0;

	private Object _buttonListenerLock = new Object();
	private CallbackContext _buttonListener;

	private ApplySettings _settingsApplier;

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
			final String action = intent.getAction();
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
				//				String message = intent.getStringExtra(GB.DISPLAY_MESSAGE_MESSAGE);
				//				int severity = intent.getIntExtra(GB.DISPLAY_MESSAGE_SEVERITY, GB.INFO);
				//				addMessage(message, severity);
			}
		}
	};

	private final BroadcastReceiver mButtonReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if(_buttonBroadcastName != null && _buttonBroadcastName.equals(action)){
				LOG.d(PLUGIN_NAME, "BR.received notification: PREF_MIBAND_BUTTON_PRESS_BROADCAST");
				synchronized (_buttonListenerLock){
					if(_buttonListener != null){
						PluginResult result = new PluginResult(PluginResult.Status.OK);
						result.setKeepCallback(true);
						_buttonListener.sendPluginResult(result);
					}
				}
			}
		}
	};


	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);

		_settingsApplier = new ApplySettings(cordova);

		_pendingResultTimeoutTimer = new Timer();
		_buttonBroadcastName = SettingsUtil.getDefaultButtonPressValue(this.cordova.getActivity());

		IntentFilter filter = new IntentFilter();
		//		filter.addAction(GBApplication.ACTION_QUIT);
		filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
		//		filter.addAction(GB.ACTION_DISPLAY_MESSAGE);
		LocalBroadcastManager.getInstance(cordova.getActivity()).registerReceiver(mReceiver, filter);

		filter = new IntentFilter();
		filter.addAction(_buttonBroadcastName);
		cordova.getActivity().getApplicationContext().registerReceiver(mButtonReceiver, filter);

//		//TODO add a callable init-method -> check there, if it is run the first time
//		Prefs prefs = GBApplication.getPrefs();
//		if (prefs.getBoolean("firstrun", true)) {
//		prefs.getPreferences().edit().putBoolean("firstrun", false).apply();
//		Intent enableIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
//		startActivity(enableIntent);

//		}

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


		if (ACTION_IS_CONNECTED.equals(action)) {

			this.isDeviceConnected(callbackContext);

		} else if (ACTION_BATTERY_LEVEL.equals(action)) {

			long timeout;
			boolean includeDetails = false;

			try {

				if (args.length() > 0) {
					timeout = args.getInt(0);

					if(args.length() > 1){
						includeDetails = args.getBoolean(1);
					}

				} else {
					timeout = RESULT_TIMEOUT;
				}


			} catch (JSONException e) {

				String errorMessage = "Invalid argument for timeout (using default instead): " + e;
				LOG.e(PLUGIN_NAME, errorMessage, e);
				timeout = RESULT_TIMEOUT;
			}

			this.getBatteryLevel(callbackContext, timeout, includeDetails);

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

			int start = getInt(args, 0, 0);//default: starting at UNIX zero -> 1970 ...
			int end = getInt(args, 1, toTimestamp(new Date()));//default: up to now

			this.retrieveData(start, end, callbackContext);

		} else if (ACTION_REMOVE_DATA.equals(action)) {

			//FIXME currently this not supported by Gadgebridge:
			//      -> cannot handle deletion of entities with composed-keys (and mi-band-entries key is (device_id, user_id)!)
			//      -> this will, as of now, always return errors

			int start = getInt(args, 0, 0);//default: starting at UNIX zero -> 1970 ...
			int end = getInt(args, 1, toTimestamp(new Date()));//default: up to now

			this.removeData(start, end, false, callbackContext);

		} else if (ACTION_REMOVE_ALL_DATA.equals(action)) {

			this.removeData(-1, -1, true, callbackContext);

		} else if (ACTION_DEVICE_INFO.equals(action)) {

			this.getDeviceInfo(callbackContext);

		} else if (ACTION_CONNECT.equals(action)) {

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

			this.connectDevice(callbackContext, timeout);

		} else if (ACTION_FIRE_NOTIFICATION.equals(action)) {

			//arg: message text /TODO: title, body (, sender?)
			String message = getString(args, 0, null);

			//(optional) arg: repeating alarms (DEFAULT: 3)
			int repeat = getInt(args, 1, 3);

      //(optional) arg: delay/interval between repeating the text message (DEFAULT:
      long delay = getLong(args, 2, NOTIFICATION_REPEAT_DELAY);

			this.fireNotification(callbackContext, message, repeat, delay);

		} else if (ACTION_CANCEL_NOTIFICATION.equals(action)) {

			boolean canceled = false;
			synchronized (_notificationRepeatTaskLock){

				GBApplication.deviceService().onDeleteNotification(currentNotificationId);

				if(_notificationRepeatTask != null){
					canceled = _notificationRepeatTask.cancel();
				}
			}

			callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, canceled));

		} else if (ACTION_GET_CONFIG.equals(action)) {

			LinkedList<String> names = null;
			if(args.length() > 0){
				Object obj = null;
				try {

					obj = args.get(0);
					names = new LinkedList<String>();

					if(obj instanceof String){
						names.add((String) obj);
					} else {
						JSONArray list = (JSONArray) obj;
						String name;
						for(int i=0, size = list.length(); i < size; ++i){
							name = getString(list, i, null);
							if(name != null){
								names.add(name);
							}
						}
					}

				} catch (JSONException e) {
					LOG.e(PLUGIN_NAME, "get_config: could not evaluate arguments", e);
				}
			}

			JSONObject result = new JSONObject();
			if(names == null || names.size() < 1){
				for(Map.Entry<String, ?> e : SettingsUtil.getAll().entrySet()){
					this.addToJson(result, e.getKey(), e.getValue());
				}
			} else {
				//TODO use getPrefs() methods directly, instead of using map
				Map<String, ?> settings = SettingsUtil.getAll();
				for(String name : names){
					this.addToJson(result, name, settings.get(name));
				}
			}

			callbackContext.success(result);

		} else if (ACTION_SET_CONFIG.equals(action)) {


			if(args.length() < 2) {

				if(args.length() > 0){
					try {

						Set<String> results = SettingsUtil.setPref(args.getJSONObject(0), _settingsApplier);
						callbackContext.success(new JSONArray(results));

					} catch (JSONException e) {
						String msg = "set_config: could not evulate first argument as JSON object";
						LOG.e(PLUGIN_NAME, msg, e);
						callbackContext.error(msg);
					}
				} else {
					callbackContext.error("set_config: require 2 arguments, only encountered " + args.length());
				}

			} else {

				String name = getString(args, 0, null);
				if (name == null || name.length() < 1) {
					callbackContext.error("set_config: invalid setting ID " + name);
				} else {

					try {
						Object val = args.get(1);
						if(SettingsUtil.setPref(name, val)){
							callbackContext.success(name);
						} else {
							callbackContext.error(String.format("failed to set %s to %s",name, val));
						}
					} catch (JSONException e) {
						String msg = "Could not extract settings value at argument index 1";
						LOG.e(PLUGIN_NAME, msg, e);
						callbackContext.error(msg);
					}
				}

			}

		} else if (ACTION_ADD_CONNECTION_LISTENER.equals(action)) {

			//(optional) arg: return full information on device upon connection changes (instead of just the state)
			boolean fullInfo = getBool(args, 0, false);
			this.addConnectionStateListener(callbackContext, fullInfo);

		} else if (ACTION_REMOVE_CONNECTION_LISTENER.equals(action)) {

			this.removeConnectionStateListener(callbackContext);

		} else if (ACTION_ADD_BUTTON_LISTENER.equals(action)) {

			synchronized (_buttonListenerLock){

				this.releaseButtonListener();
				_buttonListener = callbackContext;
			}

		} else if (ACTION_REMOVE_BUTTON_LISTENER.equals(action)) {

			boolean removed = false;
			synchronized (_buttonListenerLock){

				if(_buttonListener != null) {
					this.releaseButtonListener();
					_buttonListener = null;
					removed = true;
				}
			}

			callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, removed));

		} else {


			if (!start(action, callbackContext)) {
				isValidAction = false;
			}
		}


		return isValidAction;

	}

	private void releaseButtonListener() {
		if(_buttonListener != null){
			PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
			_buttonListener.sendPluginResult(result);
		}
	}

	private String getString(JSONArray args, int i, String defaultValue) {
		if(args.length() > i){
			try{
				return args.getString(i);
			} catch(Exception e){
				LOG.e(PLUGIN_NAME, "Failed to extract String argument at "+i+": "+e.getLocalizedMessage(), e);
			}
		}
		return defaultValue;
	}

	private boolean isString(JSONArray args, int i) {
		if(args.length() > i){
			try{
				Object obj = args.getJSONObject(i);
				return obj instanceof String;
			} catch(Exception e){
				LOG.e(PLUGIN_NAME, String.format("Could not access to extract String argument at %d as String: %s ", i, e.getLocalizedMessage()), e);
			}
		}
		return false;
	}

	/**
	 * HELPER extract argument as boolean from index i.
	 *
	 * @param args the arguments
	 * @param i the index in args
	 * @param defaultValue a default value, in case the there is no boolean value in args at i
	 * @return defaultValue if there was a problem, otherwise the boolean-argument value from i
	 */
	private boolean getBool(JSONArray args, int i, boolean defaultValue) {
		if(args.length() > i){
			try{
				return args.getBoolean(i);
			} catch(Exception e){
				LOG.e(PLUGIN_NAME, "Failed to extract boolean argument at "+i+": "+e.getLocalizedMessage(), e);
			}
		}
		return defaultValue;
	}

	/**
	 * HELPER extract argument as int from index i.
	 *
	 * @param args the arguments
	 * @param i the index in args
	 * @param defaultValue a default value, in case the there is no int value in args at i
	 * @return defaultValue if there was a problem, otherwise the int-argument value from i
	 */
	private int getInt(JSONArray args, int i, int defaultValue) {
		if(args.length() > i){
			try{
				return args.getInt(i);
			} catch(Exception e){
				LOG.e(PLUGIN_NAME, "Failed to extract int argument at "+i+": "+e.getLocalizedMessage(), e);
			}
		}
		return defaultValue;
	}

  /**
   * HELPER extract argument as long from index i.
   *
   * @param args the arguments
   * @param i the index in args
   * @param defaultValue a default value, in case the there is no long value in args at i
   * @return defaultValue if there was a problem, otherwise the long-argument value from i
   */
  private long getLong(JSONArray args, int i, long defaultValue) {
    if(args.length() > i){
      try{
        return args.getLong(i);
      } catch(Exception e){
        LOG.e(PLUGIN_NAME, "Failed to extract long argument at "+i+": "+e.getLocalizedMessage(), e);
      }
    }
    return defaultValue;
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
//		else if (target.equals(ACTION_START_ALARM)) {
//		targetCl = AlarmDetails.class;

//		//avoidSendAlarmsToDevice = true;
//		intent.putExtra("alarm", alarm);
//		intent.putExtra(GBDevice.EXTRA_DEVICE, getDevice());
//		//startActivityForResult(startIntent, REQ_CONFIGURE_ALARM);


//		}
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

	protected void isDeviceConnected(CallbackContext callbackContext) {

		GBDevice d = getDevice();
		if (d != null) {

			boolean connected = GBDevice.State.INITIALIZED.equals(d.getState());
			PluginResult result = new PluginResult(PluginResult.Status.OK, connected);
			callbackContext.sendPluginResult(result);

		} else {
			doSendNoDeviceError(callbackContext, "Could check connection");
		}

	}

	protected void getDeviceInfo(CallbackContext callbackContext) {
		GBDevice device = getDevice();
		if (device != null) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, toJson(device, DeviceInfoType.INFO));
			callbackContext.sendPluginResult(result);
		} else {
			doSendNoDeviceError(callbackContext, "Could not get device info");
		}
	}

	protected void addConnectionStateListener(CallbackContext callbackContext, final boolean allStatusChanges) {

		GBDevice _device = getDevice();
		if (_device != null) {


			PluginResult result = new PluginResult(PluginResult.Status.OK, toJson(_device, DeviceInfoType.CONNECTION_STATE));
			result.setKeepCallback(true);
			callbackContext.sendPluginResult(result);

			final long timeout = Long.MAX_VALUE;
			final GBDevice.State _state = _device.getState();
			synchronized (_pendingResultLock) {

				//TODO re-use AsyncDeviceResult, if multiple listeners are registered:
				//      * make extended AsyncDeviceResult class that maintains a list of CallbackContext
				//      * check, if _pendingResults has an entry for TASK_CONNECTION_STATE_LISTENER
				//      * add callbackContext to its list of callbacks

				//register callback, listening to changes in the GBDevice
				_pendingResults.add(new AsyncDeviceResult(callbackContext, TASK_CONNECTION_STATE_LISTENER, timeout) {

					private GBDevice.State prevState = _state;

					@Override
					public boolean sendResult(GBDevice device) {

						final GBDevice.State state = device.getState();

						if (!this.prevState.equals(state)) {

							if(!allStatusChanges && (!GBDevice.State.NOT_CONNECTED.equals(state) && !GBDevice.State.INITIALIZED.equals(state))){
								//if not all connection-status changes should be reported: ignore all changes other than not-connected & initialized (i.e. "fully-connected")
								return false;//<- FALSE, so that this "listener" is kept in _pendingResults list
							}

							LOG.d(PLUGIN_NAME, "ASYNC connection state changed: " + device.getState().toString());//DEBUG

							PluginResult result = new PluginResult(PluginResult.Status.OK, toJson(device, DeviceInfoType.CONNECTION_STATE));
							result.setKeepCallback(true);
							callbackContext.sendPluginResult(result);

							this.prevState = state;
						}

						//do not remove this "listener" from _pendingResults:
						return false;
					}
				});
			}

		} else {
			doSendNoDeviceError(callbackContext, "Could not get device info");
		}

	}

	protected void removeConnectionStateListener(CallbackContext callbackContext) {

		boolean removed = false;
		synchronized (_pendingResultLock) {

			Iterator<AsyncDeviceResult> it = _pendingResults.iterator();
			AsyncDeviceResult asyncResult;
			while (it.hasNext()) {
				asyncResult = it.next();
				if(TASK_CONNECTION_STATE_LISTENER.equals(asyncResult.description)){
					it.remove();
					removed = true;
					break;
				}
			}
		}

		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, removed));
	}

	protected void connectDevice(CallbackContext callbackContext, long timeout) {

		GBDevice d = getDevice();
		if (d != null) {

			if (d.isBusy()) {
				//TODO should this be queued instead of triggering an error here?
				callbackContext.error("device busy: '" + d.getBusyTask() + "'");
				return;///////////////////// EARLY EXIT /////////////////////
			}

			if (d.isInitialized()) {
				callbackContext.success();
			} else {

				synchronized (_pendingResultLock) {

					//register callback, listening to changes in the GBDevice
					_pendingResults.add(new AsyncDeviceResult(callbackContext, TASK_CONNECTING_DEVICE, timeout) {
						@Override
						public boolean sendResult(GBDevice device) {
							LOG.d(PLUGIN_NAME, "ASYNC connecting: " + device.getStateString());//DEBUG
							boolean fullyConnected = device.isInitialized();
							if (fullyConnected) {
								this.callbackContext.success();
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
			doSendNoDeviceError(callbackContext, "Could not connect");
		}

	}

	protected void getBatteryLevel(CallbackContext callbackContext, long timeout, final boolean details) {

		GBDevice d = getDevice();
		if (d != null) {

			if (d.isBusy()) {
				//TODO should this be queued instead of triggering an error here?
				callbackContext.error("device busy: '" + d.getBusyTask() + "'");
				return;///////////////////// EARLY EXIT /////////////////////
			}

			if (d.isConnected()) {
				if(!details){
					callbackContext.success(d.getBatteryLevel());
				} else {
					callbackContext.success(toJson(d, DeviceInfoType.BATTERY_INFO));
				}
			} else {

				synchronized (_pendingResultLock) {

					//register callback, listening to changes in the GBDevice
					_pendingResults.add(new AsyncDeviceResult(callbackContext, TASK_BATTERY_LEVEL, timeout) {
						@Override
						public boolean sendResult(GBDevice device) {
							int level = device.getBatteryLevel();
							LOG.d(PLUGIN_NAME, "ASYNC device battery: " + level);//DEBUG
							if (level != -1) {
								if(!details){
									this.callbackContext.success(level);
								} else {
									this.callbackContext.success(toJson(device, DeviceInfoType.BATTERY_INFO));
								}
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

	protected void fireNotification(CallbackContext callbackContext, String message, int repeat, long delay) {

		GBDevice d = getDevice();
		if (d != null) {

			if (d.isBusy()) {
				//TODO should this be queued instead of triggering an error here?
				callbackContext.error("device busy: '" + d.getBusyTask() + "'");
				return;///////////////////// EARLY EXIT /////////////////////
			}

			if (d.isConnected()) {

				//NotificationType.GENERIC_EMAIL | GENERIC_SMS:
				// sender + subject + body
				//NotificationType.GENERIC_ALARM_CLOCK:
				// title | subject
				//NotificationType.GENERIC_NAVIGATION:
				// title | body
				//else:
				// body
				//
				// custom: NotificationType.GENERIC_TEXT_ONLY_MESSAGE:
				//   title | subject

				final CallbackContext cb = callbackContext;
				final String msg = message;
				final int loops = repeat;
        final long interval = delay;


				synchronized (_notificationRepeatTaskLock){

					if(_notificationRepeatTask != null){
						_notificationRepeatTask.cancel();
					}

					_notificationRepeatTask = new TimerTask() {

						private int count = 0;
						@Override
						public void run() {

							NotificationSpec notification = new NotificationSpec();

							if(currentNotificationId + 1 >= Integer.MAX_VALUE){
								currentNotificationId = 0;
							}

							notification.id = ++currentNotificationId;
							notification.title = msg;
							//notification.subject = msg;
							notification.type = count == 0? NotificationType.GENERIC_ALARM_CLOCK : NotificationType.GENERIC_TEXT_ONLY_MESSAGE;

							GBApplication.deviceService().onNotification(notification);

							LOG.d(PLUGIN_NAME, String.format("fireNotification: repeating notification (%s / %d): '%s' ", count+1, loops, msg));

							if(++count >= loops){
								this.cancel();
							}
						}

						@Override
						public boolean cancel(){
              super.cancel();

							LOG.d(PLUGIN_NAME, String.format("CANCEL fireNotification: repeated notification (%s / %d): '%s' ", count, loops, msg));

							boolean completed = count >= loops;
							cb.sendPluginResult(new PluginResult(PluginResult.Status.OK, completed));
							_notificationRepeatTask = null;

              return !completed;
						}
					};
				}

				//FIXME
				_pendingResultTimeoutTimer.schedule(_notificationRepeatTask, 0l, interval);

			} else {
				callbackContext.error("Could not fire notification: Not connected.");
			}
		} else {
			doSendNoDeviceError(callbackContext, "Could not fire notification.");
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

//			GBApplication.deviceService().onFetchActivityData();
            GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_ACTIVITY);
			final String taskDesc = d.getBusyTask();
			synchronized (_pendingResultLock) {

				//if there is a busy-task set -> start listing to when the busy-task is reset again
				//				if(taskDesc != null) {

				final boolean checkConnecting = isConnecting;

				//register callback, listening to changes in the GBDevice & its busy-task
				_pendingResults.add(new AsyncDeviceResult(callbackContext, TASK_SYNCHRONIZE_DATA, timeout) {

					private boolean _isCheckConnecting = checkConnecting;//if need to wait for connection first
					private boolean _isSettingTaskName = false;
					private boolean _isSyncStarted = false;
					private String _taskName = taskDesc;//<- NULL, if waiting for connection (will be set after connecting)

					@Override
					public boolean sendResult(GBDevice device) {

						String syncTaskName = SettingsUtil.getSyncTaskName(cordova.getActivity());

						if (_isCheckConnecting || _taskName == null) {
							if (device.isInitialized()) {
								LOG.d(PLUGIN_NAME, "ASYNC synchronize data: connected, starting to sync... ");//DEBUG
								_isCheckConnecting = false;

								if (_taskName == null) {

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
			}

		} else {
			doSendNoDeviceError(callbackContext, "Could not start data synchronization.");
		}

	}

	protected enum DbTaskType {RETRIEVE, REMOVE, INSERT}
	protected LinkedList<DBAccess> dbTasks = new LinkedList<DBAccess>();
	protected void executeNextDbTask(DbTaskType ignoreActiveTask){
		synchronized (dbTasks) {
			if (dbTasks.size() > 0 && !isDbTaskActive(ignoreActiveTask)) {
				LOG.d(PLUGIN_NAME, "executing next DBAccess task...");
				dbTasks.removeFirst().execute();
			} else {
				LOG.d(PLUGIN_NAME, "no next DBAccess task to execute");
			}
		}
	}
	protected boolean queueIfDbTaskActive(DBAccess task){
		synchronized (dbTasks) {
			if (isDbTaskActive(null)) {
				LOG.d(PLUGIN_NAME, "queuing DBAccess task");
				dbTasks.addLast(task);
				return true;
			}
		}
		LOG.d(PLUGIN_NAME, "no need to queue DBAccess task");
		return false;
	}
	protected boolean isDbTaskActive(DbTaskType ignoreActiveTask){
		if (
				   (!DbTaskType.REMOVE.equals(ignoreActiveTask)   && removeTask  != null && removeTask.getStatus()  != AsyncTask.Status.FINISHED)
				|| (!DbTaskType.RETRIEVE.equals(ignoreActiveTask) && refreshTask != null && refreshTask.getStatus() != AsyncTask.Status.FINISHED)
				|| (!DbTaskType.INSERT.equals(ignoreActiveTask)   && insertTask  != null && insertTask.getStatus()  != AsyncTask.Status.FINISHED)
		) {

			return true;
		}
		return false;
	}

	protected AsyncTask refreshTask;

	protected void retrieveData(int start, int end, CallbackContext callbackContext) {
		GBDevice device = getDevice();
		if (device != null) {

			DataRetrievalTask task = new DataRetrievalTask(TASK_RETRIEVING_DATA, start, end, cordova.getActivity(), callbackContext);
			if(!queueIfDbTaskActive(task)){
				refreshTask = task.execute();
			}

		} else {
			doSendNoDeviceError(callbackContext, "Could not retrieve data");
		}
	}

	protected class DataRetrievalTask extends DBAccess {

		private CallbackContext callbackContext;
		private List<? extends ActivitySample> samples;

		private int start;
		private int end;

		public DataRetrievalTask(String task, int startTimestamp, int endTimestamp, Context context, CallbackContext callbackContext) {
			super(task, context);
			this.start = startTimestamp;
			this.end = endTimestamp;
			this.callbackContext = callbackContext;
		}

		@Override
		protected void doInBackground(DBHandler db) {
			GBDevice device = getDevice();
			if (device != null) {
				DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
				SampleProvider<? extends ActivitySample> provider = coordinator.getSampleProvider(device, db.getDaoSession());

				samples = provider.getAllActivitySamples(start, end);

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

				executeNextDbTask(DbTaskType.RETRIEVE);

			} else {
				LOG.i(PLUGIN_NAME, "Not retrieving data because Cordova activity is not available anymore");
			}
		}
	}

	protected AsyncTask removeTask;

	protected void removeData(int start, int end, boolean isRemoveAll, CallbackContext callbackContext) {
		GBDevice device = getDevice();
		if (device != null) {

			DataRemovalTask task = new DataRemovalTask(TASK_REMOVING_DATA, start, end, isRemoveAll, cordova.getActivity(), callbackContext);
			if(!queueIfDbTaskActive(task)){
				removeTask = task.execute();
			}

		} else {
			doSendNoDeviceError(callbackContext, "Could not remove data");
		}
	}

	protected class DataRemovalTask extends DBAccess {

		private CallbackContext callbackContext;

		private int start;
		private int end;
		private boolean removeAll;

		private List<ActivitySample> removedSamples;
		private List<String> errors;
		private Exception lastError;

		public DataRemovalTask(String task, int startTimestamp, int endTimestamp, boolean isRemoveAll, Context context, CallbackContext callbackContext) {
			super(task, context);
			this.start = startTimestamp;
			this.end = endTimestamp;
			this.removeAll = isRemoveAll;
			this.callbackContext = callbackContext;
			this.removedSamples = new LinkedList<ActivitySample>();
		}

		@Override
		protected void doInBackground(DBHandler db) {
			GBDevice device = getDevice();
			if (device != null) {
				DaoSession session = db.getDaoSession();

				if(removeAll){

					try {

						session.deleteAll(MiBandActivitySample.class);

					} catch (Exception e) {

						String msg = "ERROR removing all samples from Db";
						LOG.e(PLUGIN_NAME, msg, e);

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
								LOG.e(PLUGIN_NAME, msg, e);

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
				doSendNoDeviceError(callbackContext, "Could not access data.");
			}
		}

		@Override
		protected void onPostExecute(Object o) {
			super.onPostExecute(o);
			Activity activity = cordova.getActivity();
			if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {

				boolean hasErrors = errors == null;
				if(removedSamples.size() > 0 || hasErrors){


					JSONObject result = new JSONObject();
					addToJson(result, "message", hasErrors? "errors occured" : "success");

					if(errors != null){
						JSONArray l = new JSONArray();
						for(String s : errors){
							l.put(s);
						}
						addToJson(result, "errors", l);
					}

					if(!removeAll){
						JSONArray rmList = toJson(removedSamples);
						addToJson(result, "removed", rmList);
					}

					callbackContext.success(result);

				} else {

					String msg = lastError != null? " (last error) " + lastError.getMessage() : "error(s) while removing data.";
					callbackContext.error("Could not remove any data: "+msg);
				}

				executeNextDbTask(DbTaskType.REMOVE);

			} else {
				LOG.i(PLUGIN_NAME, "Not retrieving data because Cordova activity is not available anymore");
			}
		}
	}

	protected AsyncTask insertTask;

	protected void insertData(JSONArray args, CallbackContext callbackContext) {
		GBDevice device = getDevice();
		if (device != null) {

			DataInsertionTask task = new DataInsertionTask(TASK_INSERTING_DATA, args, cordova.getActivity(), callbackContext);
			if(!queueIfDbTaskActive(task)){
				insertTask = task.execute();
			}

		} else {
			doSendNoDeviceError(callbackContext, "Could not retrieve data");
		}
	}

	protected class DataInsertionTask extends DBAccess {

		private CallbackContext callbackContext;

		private JSONArray data;
		private boolean success;

		public DataInsertionTask(String task, JSONArray args, Context context, CallbackContext callbackContext) {
			super(task, context);
			this.success = false;
			this.data = args;
			this.callbackContext = callbackContext;
		}

		@Override
		protected void doInBackground(DBHandler db) {
			GBDevice gbDevice = getDevice();
			if (gbDevice != null) {

				try {

					//insertion adapted from: nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.operations.FetchActivityOperation
					DBHandler handler = GBApplication.acquireDB();

					DaoSession session = handler.getDaoSession();
					SampleProvider<MiBandActivitySample> sampleProvider = new MiBandSampleProvider(getDevice(), session);
					Device device = DBHelper.getDevice(gbDevice, session);
					User user = DBHelper.getUser(session);

					int size = this.data.length();
					MiBandActivitySample[] samples = new MiBandActivitySample[size];

					for (int i=0; i < size; ++i) {

						JSONObject data = this.data.getJSONObject(i);
						MiBandActivitySample sample = sampleProvider.createActivitySample();

						sample.setDevice(device);
						sample.setUser(user);
						sample.setTimestamp(data.getInt(FIELD_TIMESTAMP));
						sample.setProvider(sampleProvider);

						sample.setRawKind(MiBandSampleProvider.TYPE_ACTIVITY);

						if(data.has(FIELD_RAW_INTENSITY)){
							sample.setRawIntensity(data.getInt(FIELD_RAW_INTENSITY));
						}

						if(data.has(FIELD_ACTIVITY) || data.has(FIELD_LIGHT_SLEEP) || data.has(FIELD_DEEP_SLEEP)){

							double value;//<- FIXME TEST remove after test
							if(data.has(FIELD_ACTIVITY)){
								value = data.getDouble(FIELD_ACTIVITY);
								sample.setRawKind(MiBandSampleProvider.TYPE_ACTIVITY);
							} else if(data.has(FIELD_LIGHT_SLEEP)){
								value = data.getDouble(FIELD_LIGHT_SLEEP);
								sample.setRawKind(MiBandSampleProvider.TYPE_LIGHT_SLEEP);
							} else {//if(data.has(FIELD_DEEP_SLEEP)){
								value = data.getDouble(FIELD_DEEP_SLEEP);
								sample.setRawKind(MiBandSampleProvider.TYPE_DEEP_SLEEP);
							}

							//added extra field FIELD_RAW_INTENSITY instead of "reverse engineering" the value
							//              //HACK need to reverse AbstractMiBandSampleProvider.normalizeIntensity() which is used in getIntensity(): do mult by 180
							//              sample.setRawIntensity((int)(value * 180.0d));
							//FIXME TEST:
							int converted = (int)(value * 180.0d);
							int raw = sample.getRawIntensity();
							LOG.i(PLUGIN_NAME, String.format("TEST: intensity to raw: %s (raw: %d | converted: %d)", converted == raw, raw, converted));
						}

						if(data.has(FIELD_NOT_WORN)){
							sample.setRawKind(MiBandSampleProvider.TYPE_NONWEAR);
						}

						if(data.has(FIELD_STEPS)){
							sample.setSteps(data.getInt(FIELD_STEPS));
						}

						if(data.has(FIELD_HEART_RATE)){
							sample.setHeartRate(data.getInt(FIELD_HEART_RATE));
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
					LOG.e(PLUGIN_NAME, "Could not insert data", ex);
					cancel(true);
					callbackContext.error("Could not insert data.");
				}


			} else {
				cancel(true);
				doSendNoDeviceError(callbackContext, "Could not insert data.");
			}
		}

		@Override
		protected void onPostExecute(Object o) {
			super.onPostExecute(o);
			Activity activity = cordova.getActivity();
			if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
				if (this.success) {
					callbackContext.success();
				} else {
					callbackContext.error("errors occured while adding samples");//TODO should this more detailed?
				}
				executeNextDbTask(DbTaskType.INSERT);
			} else {
				LOG.i(PLUGIN_NAME, "Not retrieving data because Cordova activity is not available anymore");
			}
		}

	}

	private JSONObject addToJson(JSONObject obj, String name, Object value) {
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
			LOG.e(PLUGIN_NAME, String.format("Could not add JSON field %s with value %s", name, value), e);
		}
		return obj;
	}

	private enum DeviceInfoType {INFO, CONNECTION_STATE, BATTERY_INFO}

	private JSONObject toJson(GBDevice deviceInfo, DeviceInfoType type) {
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
			LOG.e(PLUGIN_NAME, "Failed to create JSONObject for sample", e);
		}
		return o;
	}


	private GBDevice getDevice() {

		if (this._deviceManager == null) {
			this._deviceManager = ((GBApplication) GBApplication.getContext()).getDeviceManager();
		}

		if (this._deviceManager.getSelectedDevice() == null) {

			List<GBDevice> deviceList = this._deviceManager.getDevices();
			if (deviceList.size() > 0) {
				GBDevice device = deviceList.get(0);//GET FIRST ONE ... TODO how should multiple devices be handled?
				//        GBApplication.deviceService().connect(device);
				this._device = device;
			}

		}
		return this._device;
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

		//			public static final int TYPE_NOT_MEASURED = -1;
		//			public static final int TYPE_UNKNOWN = 0;
		//			public static final int TYPE_ACTIVITY = 1;
		//			public static final int TYPE_LIGHT_SLEEP = 2;
		//			public static final int TYPE_DEEP_SLEEP = 4;
		//			public static final int TYPE_NOT_WORN = 8;
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
			o.put(FIELD_ACTIVITY, (double) sample.getIntensity());
			break;
		case 2://TYPE_LIGHT_SLEEP
			o.put(FIELD_LIGHT_SLEEP, sample.getIntensity());
			break;
		case 4://TYPE_LIGHT_DEEP
			o.put(FIELD_DEEP_SLEEP, sample.getIntensity());
			break;
		case 8://TYPE_NOT_WORN
			o.put(FIELD_NOT_WORN, true);
			break;
		}

		o.put(FIELD_RAW_INTENSITY, sample.getRawIntensity());

		o.put(FIELD_TIMESTAMP, timestamp);

		int steps = sample.getSteps();
		if (steps > 0) {
			o.put(FIELD_STEPS, steps);
		}

		int heartRate = sample.getHeartRate();
		if (heartRate > 0 && heartRate < 255) {
			o.put(FIELD_HEART_RATE, heartRate);
		}

		return o;
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



	///////////////////////////////////// APP STATE BEHAVIOR /////////////////////////////////////////////////////////

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

		LocalBroadcastManager.getInstance(cordova.getActivity()).unregisterReceiver(mReceiver);
		this.cordova.getActivity().getApplicationContext().unregisterReceiver(mButtonReceiver);

		super.onDestroy();
	}
}
