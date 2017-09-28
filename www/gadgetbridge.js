
var exec = require('cordova/exec');

/**
 * the plugin class / constructor
 */
function GadgetbridgePlugin() {
	this.ACTION_START1 = "ControlCenterv2";
	this.ACTION_START2 = "SettingsActivity";
	this.ACTION_START3 = "MiBandPreferencesActivity";
	this.ACTION_START4 = "AppBlacklistActivity";
	this.ACTION_START5 = "DebugActivity";
	this.ACTION_START6 = "DbManagementActivity";
	this.ACTION_START7 = "DiscoveryActivity";
	this.ACTION_START8 = "MiBandPairingActivity";
	this.ACTION_START9 = "ChartsActivity";
	this.ACTION_START10 = "ConfigureAlarms";
	this.ACTION_START11 = "AlarmDetails";
}

/**
 * Open a (native) view.
 *
 * @param  {String} viewType the name of the view (see this.ACTION_START<n>)
 * @param  {Function} [successCallback] the success callback (no arguments)
 * @param  {Function} [errorCallback] the error callback
 */
GadgetbridgePlugin.prototype.openView = function(viewType, successCallback, errorCallback) {//TODO use option object instead of arg-list?

	return exec(successCallback, errorCallback, "GadgetbridgePlugin", viewType, []);
};

/**
 * Check, if paired device is (fully) connected.
 *
 * @param  {Function} [successCallback] the success callback: successCallback(DeviceInfo | null)
 * 										where DeviceInfo: {
 * 											name: String,
 * 											address: String,
 * 											model: String,
 * 											type: String,
 * 											firmware: String
 * 										}
 * @param  {Function} [errorCallback] the error callback
 */
GadgetbridgePlugin.prototype.getDeviceInfo = function(successCallback, errorCallback) {//TODO use option object instead of arg-list?

	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "device_info", []);
};

/**
 * Check, if paired device is (fully) connected.
 *
 * @param  {Function} [successCallback] the success callback: successCallback(boolean)
 * @param  {Function} [errorCallback] the error callback
 */
GadgetbridgePlugin.prototype.isConnected = function(successCallback, errorCallback) {//TODO use option object instead of arg-list?

	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "is_connected", []);
};

/**
 * Connect to paired device.
 *
 * @param  {number} [timeout] OPTIONAL
 *                           timeout for connecting to device (if omitted, default timeout is used).
 * @param  {Function} [successCallback] the success callback: successCallback()
 * @param  {Function} [errorCallback] the error callback (e.g. due to timeout)
 */
GadgetbridgePlugin.prototype.connect = function(timeout, successCallback, errorCallback) {//TODO use option object instead of arg-list?
	
	if(typeof timeout === 'function'){
		errorCallback = successCallback;
		successCallback = timeout;
		timeout = void(0);
	}
	
	var args = typeof timeout === 'number'? [timeout] : [];
	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "connect", args);
};

/**
 * Add listener for connection-state changes.
 *
 * @param  {Function} [successCallback] the success callback: successCallback({address: String, state: String})
 * @param  {Function} [errorCallback] the error callback
 */
GadgetbridgePlugin.prototype.onConnect = function(successCallback, errorCallback) {

	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "on_connect", []);
};

/**
 * Remove listener for connection-state changes.
 *
 * @param  {Function} [successCallback] the success callback: successCallback(didRemove: boolean)
 * @param  {Function} [errorCallback] the error callback
 */
GadgetbridgePlugin.prototype.offConnect = function(successCallback, errorCallback) {

	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "off_connect", []);
};


/**
 * Get batter level for the paired device.
 *
 * @param  {number} [timeout] OPTIONAL
 *                           timeout for retrieving the batter level (if omitted, default timeout is used).
 * @param  {Function} [successCallback] the success callback: successCallback(percent)
 * @param  {Function} [errorCallback] the error callback (e.g. due to timeout)
 */
GadgetbridgePlugin.prototype.getBatteryLevel = function(timeout, successCallback, errorCallback) {//TODO use option object instead of arg-list?
	
	if(typeof timeout === 'function'){
		errorCallback = successCallback;
		successCallback = timeout;
		timeout = void(0);
	}
	
	var args = typeof timeout === 'number'? [timeout] : [];
	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "battery_level", args);
};

/**
 * Show notification on tracker/device (immediately).
 *
 * @param  {string} message the notification message text TODO title, body (, sender)
 * @param  {number} [repeat] number of times, for repeating to show text message on device (DEFAULT: 3)
 * @param  {Function} [successCallback] the success callback: successCallback()
 * @param  {Function} [errorCallback] the error callback (e.g. if device is not connected)
 */
GadgetbridgePlugin.prototype.fireNotification = function(message, repeat, successCallback, errorCallback) {//TODO use option object instead of arg-list?
	
	if(typeof repeat === 'function'){
		errorCallback = successCallback;
		successCallback = repeat;
		repeat = void(0);
	}
	
	repeat = typeof repeat === 'number'? repeat : 3;
	var args = [message, repeat];
	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "fire_notification", args);
};

/**
 * Start data synchronization with fitness-tracker device.
 * 
 * @param  {number} [timeout] OPTIONAL
 *                           timeout for retrieving the batter level (if omitted, default timeout is used).
 * @param  {Function} [successCallback] the success callback: successCallback()
 * @param  {Function} [errorCallback] the error callback (e.g. due to timeout)
 */
GadgetbridgePlugin.prototype.synchronize = function(timeout, successCallback, errorCallback) {//TODO use option object instead of arg-list?

	if(typeof timeout === 'function'){
		errorCallback = successCallback;
		successCallback = timeout;
		timeout = void(0);
	}
	
	var args = typeof timeout === 'number'? [timeout] : [];
	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "sync", args);
};

/**
 * Get activity data.
 *
 * @param  {number} [start] OPTIONAL
 *                           start timestamp (10 digits) for first data-sample to receive
 *                           DEFAULT: 0
 * @param  {number} [end] OPTIONAL
 *                           end timestamp (10 digits) for last data-sample to receive
 *                           DEFAULT: now
 * @param  {Function} [successCallback] the success callback: successCallback(data)
 * @param  {Function} [errorCallback] the error callback (e.g. no device paired)
 */
GadgetbridgePlugin.prototype.retrieveData = function(start, end, successCallback, errorCallback) {//TODO use option object instead of arg-list?
	
	if(typeof start === 'function'){
		errorCallback = end;
		successCallback = start;
		end = void(0);
		start = void(0);
	}
	
	if(typeof end === 'function'){
		errorCallback = successCallback;
		successCallback = timeout;
		end = start;
		start = void(0);
	}
	
	var args = typeof start === 'number'? [start] : [];
	if(typeof stop === 'number'){
		args.push(end);
	}
	
	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "retrieve", args);
};

/**
 * Remove activity data from data base.
 *
 * @param  {number} [start] OPTIONAL
 *                           start timestamp (10 digits) for first data-sample to remove
 *                           DEFAULT: 0
 * @param  {number} [end] OPTIONAL
 *                           end timestamp (10 digits) for last data-sample to remove
 *                           DEFAULT: now
 * @param  {Function} [successCallback] the success callback: successCallback()
 * @param  {Function} [errorCallback] the error callback (e.g. no device paired)
 */
GadgetbridgePlugin.prototype.removeData = function(start, end, successCallback, errorCallback) {//TODO use option object instead of arg-list?

	if(typeof start === 'function'){
		errorCallback = end;
		successCallback = start;
		end = void(0);
		start = void(0);
	}
	
	if(typeof end === 'function'){
		errorCallback = successCallback;
		successCallback = timeout;
		end = start;
		start = void(0);
	}
	
	var args = typeof start === 'number'? [start] : [];
	if(typeof stop === 'number'){
		args.push(end);
	}
	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "remove", args);
};

/**
 * Get configuration setting(s).
 *
 * @param  {string|Array<string>} [settingsName] 
 * 									the name(s) of the setting(s) to get.
 * 									If omitted, all settings are returned
 * @param  {Function} [successCallback] the success callback: successCallback({[id: string]: any})
 * @param  {Function} [errorCallback] the error callback
 */
GadgetbridgePlugin.prototype.getConfig = function(settingsName, successCallback, errorCallback) {//TODO use option object instead of arg-list?
	
	if(typeof settingsName === 'function'){
		errorCallback = successCallback;
		successCallback = settingsName;
		settingsName = void(0);
	}
	
	var args = settingsName? [settingsName] : [];

	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "get_config", args);
};

/**
 * Set one or multiple configuration values.
 *
 * @param  {string|{[id:string]: any} name 	
 * 					if string: the name/ID of the setting/configuration field (NOTE: must also supply value argument)
 * 					if object: the settings (name & value) that should be applied
 * @param  {string|number|boolean|Array<any>|null} [value]
 * 					the configuration value.
 * 					Using <code>null</code> will remove the setting, i.e. reset to default value.
 * @param  {Function} [successCallback] the success callback with the successfully set preference(s) as argument: 
 * 											if arguments name & value: successCallback(string) NOTE: if not successful, the error-callback is invoked
 * 											if object argument: successCallback(Array<string>)
 * @param  {Function} [errorCallback] the error callback
 */
GadgetbridgePlugin.prototype.setConfig = function(name, value, successCallback, errorCallback) {//TODO use option object instead of arg-list?
	
	var args = [name];
	
	if(typeof value === 'function'){
		errorCallback = successCallback;
		successCallback = value;
		value = void(0);
	}
	
	if(typeof value !== 'undefined'){
		args.push(value);
	}
	
	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "set_config", args);
};

//export an instance of GadgetbridgePlugin -> this will be returned by Corodva's require-calls
//(i.e. "singleton pattern")
module.exports = new GadgetbridgePlugin();
