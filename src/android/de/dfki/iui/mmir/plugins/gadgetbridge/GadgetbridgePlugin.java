package de.dfki.iui.mmir.plugins.gadgetbridge;

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import de.dfki.iui.mmir.plugins.gadgetbridge.db.DbTaskManager;
import de.dfki.iui.mmir.plugins.gadgetbridge.utils.ApplySettings;
import de.dfki.iui.mmir.plugins.gadgetbridge.utils.JSONUtils;
import de.dfki.iui.mmir.plugins.gadgetbridge.utils.ResultUtils;
import de.dfki.iui.mmir.plugins.gadgetbridge.utils.SettingsUtil;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.AppBlacklistActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureAlarms;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.DbManagementActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.DebugActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.DiscoveryActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPairingActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPreferencesActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 *
 */
public class GadgetbridgePlugin extends CordovaPlugin implements IDeviceManager {

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

  static final String PREF_ENABLE_EXTENDED_PERMISSIONS = "GB_EXTENDED_PERMISSIONS";
//	static final String PREF_ENABLE_CALL_HANDLER = "GB_EXTENDED_PERMISSIONS_CALL";
//	static final String PREF_ENABLE_SMS_HANDLER = "GB_EXTENDED_PERMISSIONS_SMS";

	static final String PLUGIN_NAME = GadgetbridgePlugin.class.getSimpleName();

  private static String _buttonBroadcastName;

	private DeviceManager _deviceManager;
	private GBDevice _device;

	/* locking object for synchronized/thread-safe access to _pendingResults:
	 * wrap every access to _pendingResults with this locker!
	 */
	private final Object _pendingResultLock = new Object();
	private LinkedList<AsyncDeviceResult> _pendingResults = new LinkedList<AsyncDeviceResult>();

	private Timer _pendingResultTimeoutTimer;
	private Object _pendingResultTimeoutTaskLock = new Object();
	private TimerTask _pendingResultTimeoutTask;
	private static final long RESULT_TIMEOUT = 5000L;//5 sec


	private final Object _notificationRepeatTaskLock = new Object();
	private TimerTask _notificationRepeatTask;
	private static final long NOTIFICATION_REPEAT_DELAY = 10000L;//10 sec
	private int currentNotificationId = 0;

	private final Object _buttonListenerLock = new Object();
	private CallbackContext _buttonListener;

	private ApplySettings _settingsApplier;
	private DbTaskManager _dbTaskManager;

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
												ResultUtils.sendTimeoutError(asyncResult.callbackContext, "Could not " + asyncResult.description);
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
		_dbTaskManager = new DbTaskManager(this.cordova, this);

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

		//TODO allow detailed permissions for call & sms separately
		//NOTE enabling these requires the corresponding permission entries in the Android manifest (e.g. by using the corresponding AAR file which is selected via config.xml preference GB_EXTENDED_PERMISSIONS)
		boolean enableExtPerm = this.preferences.getBoolean(PREF_ENABLE_EXTENDED_PERMISSIONS, false);
		SettingsUtil.enableCallHandler(enableExtPerm);
		SettingsUtil.enableSmsHandler(enableExtPerm);


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

			int start = JSONUtils.getInt(args, 0, 0);//default: starting at UNIX zero -> 1970 ...
			int end = JSONUtils.getInt(args, 1, toTimestamp(new Date()));//default: up to now

			_dbTaskManager.retrieveData(start, end, callbackContext);

		} else if (ACTION_REMOVE_DATA.equals(action)) {

			//FIXME currently this not supported by Gadgebridge:
			//      -> cannot handle deletion of entities with composed-keys (and mi-band-entries key is (device_id, user_id)!)
			//      -> this will, as of now, always return errors

			int start = JSONUtils.getInt(args, 0, 0);//default: starting at UNIX zero -> 1970 ...
			int end = JSONUtils.getInt(args, 1, toTimestamp(new Date()));//default: up to now

      _dbTaskManager.removeData(start, end, false, callbackContext);

		} else if (ACTION_REMOVE_ALL_DATA.equals(action)) {

      _dbTaskManager.removeData(-1, -1, true, callbackContext);

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
			String message = JSONUtils.getString(args, 0, null);

			//(optional) arg: repeating alarms (DEFAULT: 3)
			int repeat = JSONUtils.getInt(args, 1, 3);

      //(optional) arg: delay/interval between repeating the text message (DEFAULT:
      long delay = JSONUtils.getLong(args, 2, NOTIFICATION_REPEAT_DELAY);

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
							name = JSONUtils.getString(list, i, null);
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
					ResultUtils.addToJson(result, e.getKey(), e.getValue());
				}
			} else {
				//TODO use getPrefs() methods directly, instead of using map
				Map<String, ?> settings = SettingsUtil.getAll();
				for(String name : names){
					ResultUtils.addToJson(result, name, settings.get(name));
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

				String name = JSONUtils.getString(args, 0, null);
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
			boolean fullInfo = JSONUtils.getBool(args, 0, false);
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
				ResultUtils.sendNoDeviceError(callbackContext, msg);

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
			ResultUtils.sendNoDeviceError(callbackContext, "Could check connection");
		}

	}

	protected void getDeviceInfo(CallbackContext callbackContext) {
		GBDevice device = getDevice();
		if (device != null) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, ResultUtils.toJson(device, DeviceInfoType.INFO));
			callbackContext.sendPluginResult(result);
		} else {
			ResultUtils.sendNoDeviceError(callbackContext, "Could not get device info");
		}
	}

	protected void addConnectionStateListener(CallbackContext callbackContext, final boolean allStatusChanges) {

		GBDevice _device = getDevice();
		if (_device != null) {


			PluginResult result = new PluginResult(PluginResult.Status.OK, ResultUtils.toJson(_device, DeviceInfoType.CONNECTION_STATE));
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

							PluginResult result = new PluginResult(PluginResult.Status.OK, ResultUtils.toJson(device, DeviceInfoType.CONNECTION_STATE));
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
			ResultUtils.sendNoDeviceError(callbackContext, "Could not get device info");
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
			ResultUtils.sendNoDeviceError(callbackContext, "Could not connect");
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
					callbackContext.success(ResultUtils.toJson(d, DeviceInfoType.BATTERY_INFO));
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
									this.callbackContext.success(ResultUtils.toJson(device, DeviceInfoType.BATTERY_INFO));
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
			ResultUtils.sendNoDeviceError(callbackContext, "Could not determine battery level.");
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

							//DISABLED manually setting notification.id: id is now automatically set/incremented in NotificationSpec constructor
//							if(currentNotificationId + 1 >= Integer.MAX_VALUE){
//								currentNotificationId = 0;
//							}
//							notification.id = ++currentNotificationId;
							currentNotificationId = notification.getId();//MOD retrieve current id
							
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
			ResultUtils.sendNoDeviceError(callbackContext, "Could not fire notification.");
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
			ResultUtils.sendNoDeviceError(callbackContext, "Could not start data synchronization.");
		}

	}

  public GBDevice getDevice() {

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
								ResultUtils.sendTimeoutError(asyncResult.callbackContext, "Could not " + asyncResult.description);
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
