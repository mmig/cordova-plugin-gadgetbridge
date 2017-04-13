package de.dfki.iui.mmir.plugins.gadgetbridge;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

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
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPairingActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPreferencesActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
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

    static final String PLUGIN_NAME = GadgetbridgePlugin.class.getSimpleName();

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

                LOG.d(PLUGIN_NAME, "received notification: ACTION_QUIT");

            } else if (GBDevice.ACTION_DEVICE_CHANGED.equals(action)) {

                final GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                LOG.d(PLUGIN_NAME, "received notification: ACTION_DEVICE_CHANGED -> " + device);

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
                LOG.d(PLUGIN_NAME, "received notification: ACTION_DISPLAY_MESSAGE");
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


    }

    /**
     * @param action          the name of the "function" that was called by the JavaScript code
     * @param args            the arguments that were provided the JavaScript invocation (may be empty)
     * @param callbackContext the object for triggering the JavaScript's callback functions
     *                        NOTE take care to trigger callback exactly once (either as success or as error),
     *                        i.e. do not forget to trigger it AND do not call it multiple times
     *                        (see below the example with PluginResult for a way to use the callbackContext multiple times)
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
     * @param target the class name of the targeted view/activity
     * @param callbackContext the callback context for sending the plugin result
     *
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

            if(device == null){

                targetCl = null;

                String msg = "Could not start activity \""+target+"\": ";
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
            String msg = "Requested unknown activity for starting: '"+target+"'";
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

        } catch(Exception exc){
            String msg = "Could not start activity \""+target+"\"";
            LOG.e(PLUGIN_NAME, msg, exc);
            callbackContext.error(msg+": "+exc);//TODO send errorCode / normalize error?
        }

        return true;
    }

    protected void getBatteryLevel(CallbackContext callbackContext, long timeout) {

        GBDevice d = getDevice();
        if (d != null) {

            if (d.isConnected()) {
                callbackContext.success(d.getBatteryLevel());
            } else {

                synchronized (_pendingResultLock) {

                    //register callback, listening to changes in the GBDevice
                    _pendingResults.add(new AsyncDeviceResult(callbackContext, "Battery Level", timeout) {
                        @Override
                        public boolean sendResult(GBDevice device) {
                            int level = device.getBatteryLevel();
                            LOG.d(PLUGIN_NAME, "ASYNC device battery" + level);//DEBUG
                            if (level != -1) {
                                this.callbackContext.success(level);
                                return true;
                            } else {
                                return false;
                            }
                        }
                    });
                }

                GBApplication.deviceService().connect(d);
                GBApplication.deviceService().requestDeviceInfo();

            }
        } else {
            doSendNoDeviceError(callbackContext, "Could not determine battery level: ");
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
                GBApplication.deviceService().requestDeviceInfo();
                this._device = device;
            }

        }
        return this._device;
    }

    protected static void doSendTimeoutError(CallbackContext callbackContext, String message) {
        String msg = message  + "timeout";
        LOG.e(PLUGIN_NAME, msg);
        callbackContext.error(msg);//TODO send errorCode / normalize error
    }

    protected static void doSendNoDeviceError(CallbackContext callbackContext, String message) {
        String msg = message  + "No device available";
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
