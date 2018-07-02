
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
 * @param  {Function} [successCallback] the listener / event handler: successCallback({address: String, state: String})
 * @param  {Function} [errorCallback] the error callback
 */
GadgetbridgePlugin.prototype.onConnect = function(successCallback, errorCallback) {

	addConnectionChangedListener(successCallback, errorCallback);
};

/**
 * Remove listener for connection-state changes.
 *
 * @param  {Function} [successCallback] the success callback: successCallback(didRemove: boolean)
 * @param  {Function} [errorCallback] the error callback
 */
GadgetbridgePlugin.prototype.offConnect = function(successCallback, errorCallback) {

	removeConnectionChangedListener(successCallback, errorCallback);
};

/**
 * Add listener for button presses (on tracker/device).
 *
 * @param  {Function} [successCallback] the listener / event handler: successCallback()
 * @param  {Function} [errorCallback] the error callback
 */
GadgetbridgePlugin.prototype.onButton = function(successCallback, errorCallback) {

	addButtonListener(successCallback, errorCallback);
};

/**
 * Remove listener  for button presses (on tracker/device).
 *
 * @param  {Function} [successCallback] the success callback: successCallback(didRemove: boolean)
 * @param  {Function} [errorCallback] the error callback
 */
GadgetbridgePlugin.prototype.offButton = function(successCallback, errorCallback) {

	removeButtonListener(successCallback, errorCallback);
};


/**
 * Get batter level for the paired device.
 * 
 * @param  {number} [timeout] OPTIONAL
 *                           timeout for retrieving the batter level (if omitted, default timeout is used).
 * @param  {boolean} [details] OPTIONAL
 *                           include details of battery level in response (threshold, charge/non-charging etc)
 * @param  {Function} [successCallback] the success callback: successCallback(percent: NUMBER, details?: {level: NUMBER, thresholdInPercent: NUMBER, state: STRING})
 * 										NOTE: 2nd parameter in callback is only present, if this function was called with details-argument TRUE
 * @param  {Function} [errorCallback] the error callback (e.g. due to timeout)
 */
GadgetbridgePlugin.prototype.getBatteryLevel = function(timeout, details, successCallback, errorCallback) {//TODO use option object instead of arg-list?
	
	if(typeof timeout === 'function'){
		errorCallback = details;
		successCallback = timeout;
		details = void(0);
		timeout = void(0);
	} if(typeof timeout === 'boolean'){
		errorCallback = successCallback;
		successCallback = details;
		details = timeout;
		timeout = void(0);
	} else if(typeof details === 'function'){
		errorCallback = timeout;
		successCallback = details;
		details = void(0);
		timeout = void(0);
	}
	
	var args = typeof timeout === 'number'? [timeout] : [];
	if(typeof details === 'boolean'){
		args.push(details);
		if(successCallback){
			var successCallbackOrig = successCallback;
			successCallback = function(details){
				successCallbackOrig.call(this, details.level, details);
			};
		}
	}
	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "battery_level", args);
};

/**
 * Show notification on tracker/device (immediately).
 *
 * @param  {string} message the notification message text TODO title, body (, sender)
 * @param  {number} [repeat] number of times, for repeating to show text message on the device (DEFAULT: 3)
 * @param  {number} [delay] delay in milliseconds between repeating the text message on the device (DEFAULT: 10000 ms (10 sec))
 * @param  {Function} [successCallback] the success callback: successCallback(didComplete: boolean)
 * @param  {Function} [errorCallback] the error callback (e.g. if device is not connected)
 */
GadgetbridgePlugin.prototype.fireNotification = function(message, repeat, delay, successCallback, errorCallback) {//TODO use option object instead of arg-list?
	
	
	if(typeof repeat === 'function'){
		errorCallback = delay;
		successCallback = repeat;
		delay = void(0);
		repeat = void(0);
	} else if(typeof delay === 'function'){
		errorCallback = successCallback;
		successCallback = delay;
		delay = void(0);
	}
	
	repeat = typeof repeat === 'number'? repeat : 3;
	delay = typeof delay === 'number'? delay : 10000;
	var args = [message, repeat, delay];
	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "fire_notification", args);
};

/**
 * Cancel notification (repeats) on tracker/device.
 * 
 * Has no effect, if currently no notification is active.
 *
 * @param  {Function} [successCallback] the success callback: successCallback(didCancel: boolean)
 * @param  {Function} [errorCallback] the error callback (e.g. if device is not connected)
 */
GadgetbridgePlugin.prototype.cancelNotification = function(successCallback, errorCallback) {
	
	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "cancel_notification", []);
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
	if(typeof end === 'number'){
		args.push(end);
	}
	
	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "retrieve", args);
};

/**
 * @deprecated FIXME currently not supported by Gadgebridge: cannot handle deletion of entities with composed-keys (and mi-band-entries key is (device_id, user_id)!)
 * 
 * 
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
	if(typeof end === 'number'){
		args.push(end);
	}
	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "remove", args);
};

/**
 * Remove activity data from data base.
 * 
 * @param  {Function} [successCallback] the success callback: successCallback()
 * @param  {Function} [errorCallback] the error callback (e.g. no device paired)
 */
GadgetbridgePlugin.prototype.removeAllData = function(successCallback, errorCallback) {//TODO use option object instead of arg-list?

	return exec(successCallback, errorCallback, "GadgetbridgePlugin", "remove_all", []);
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

/* ****************************************************************
 * HELPERS use only 1 native listener for connection-changes ->
 *         do manage multiple listeners via list on JS side.
 * 
 */


var _connectionListeners = [];
var _connectionChangeHandlerFunc = function onconnection(state){//HELPER fire "connection changed" on all listeners
	_connectionErrorFunc = null;//<- reset error, if there was one previously
	for(var i=0, size = _connectionListeners.length; i < size; ++i){
		_connectionListeners[i](state);
	}
};
var _connectionErrorFunc = null;//HELPER error callback: will be set, if there was an error registering the native callback

function addConnectionChangedListener(listener, errorCallback){
	
	if(_connectionListeners.length < 1){
		
		exec(_connectionChangeHandlerFunc, function onButtonError(err){
			
			_doRemoveListener(listener, _connectionListeners);//revert adding the listener to the list
			_connectionErrorFunc = function(errCb){//set error-function callback to output this error
				errCb && errCb(err);
			};
			_connectionErrorFunc(errorCallback);
			
		}, "GadgetbridgePlugin", "on_connect", []);
		
	} else if(_connectionErrorFunc){
		setTimeout(function(){_connectionErrorFunc(errorCallback);}, 10);
		return;
	}
	
	_connectionListeners.push(listener);
}

function removeConnectionChangedListener(listener, errorCallback){

	var removed = _doRemoveConnectionListener(listener, _connectionListeners);
	
	if(removed && _connectionListeners.length < 1){
		exec(function(didRemove){
			if(!didRemove){
				console.warn('no onconnectionchanged listener to remove');
			}
			listener(removed);
		}, errorCallback, "GadgetbridgePlugin", "off_connect", []);
	} else {
		setTimeout(function(){listener(removed);}, 10);
	}
}


/* *****************************************************************
 * HELPERS since natively only 1 button-clicked-listener can be set:
 *         must manage listener list on JS side.
 * 
 */

var _buttonListener = [];
var _buttonHandlerFunc = function onbutton(){//HELPER fire "button click" on all listeners
	_buttonErrorFunc = null;//<- reset error, if there was one previously
	for(var i=0, size = _buttonListener.length; i < size; ++i){
		_buttonListener[i]();
	}
};
var _buttonErrorFunc = null;//HELPER error callback: will be set, if there was an error registering the native callback

function addButtonListener(listener, errorCallback){
	
	if(_buttonListener.length < 1){
		
		exec(_buttonHandlerFunc, function onButtonError(err){
			
			_doRemoveListener(listener, _buttonListener);//revert adding the listener to the list
			_buttonErrorFunc = function(errCb){//set error-function callback to output this error
				errCb && errCb(err);
			};
			_buttonErrorFunc(errorCallback);
			
		}, "GadgetbridgePlugin", "on_button", []);
		
	} else if(_buttonErrorFunc){
		setTimeout(function(){_buttonErrorFunc(errorCallback);}, 10);
		return;
	}
	
	_buttonListener.push(listener);
}

function removeButtonListener(listener, errorCallback){

	var removed = _doRemoveListener(listener, _buttonListener);
	
	if(removed && _buttonListener.length < 1){
		exec(function(didRemove){
			if(!didRemove){
				console.warn('no onbutton listener to remove');
			}
			listener(removed);
		}, errorCallback, "GadgetbridgePlugin", "off_button", []);
	} else {
		setTimeout(function(){listener(removed);}, 10);
	}
}


/////////////////////////////////////////// utility functions

var _doRemoveListener = function(listener, list){//HELPER remove listener from list
	var l;
	for(var i = list.length; i >= 0; --i){
		l = list[i];
		if(l === listener){
			list.splice(i,1);
			return true;
		}
	}
	return false;
}

//export an instance of GadgetbridgePlugin -> this will be returned by Corodva's require-calls
//(i.e. "singleton pattern")
module.exports = new GadgetbridgePlugin();
