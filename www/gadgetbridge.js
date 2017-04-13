
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
 * @param  {function} successCallback the success callback (no arguments)
 * @param  {function} errorCallback the error callback
 */
GadgetbridgePlugin.prototype.openView = function(viewType, successCallback, errorCallback) {//TODO use option object instead of arg-list?

     return exec(successCallback, errorCallback, "GadgetbridgePlugin", viewType, []);
};

/**
 * Get batter level for the paired device.
 *
 * @param  {function} successCallback the success callback: successCallback(percent)
 * @param  {function} errorCallback the error callback (e.g. due to timeout)
 * @param  {number} [timeout] OPTIONAL
 *                           timeout for retrieving the batter level (if omitted, default timeout is used).
 */
GadgetbridgePlugin.prototype.getBatteryLevel = function(successCallback, errorCallback, timeout) {//TODO use option object instead of arg-list?

		var args = typeof timeout === 'number'? [timeout] : [];
    return exec(successCallback, errorCallback, "GadgetbridgePlugin", "battery_level", args);
};

//export an instance of GadgetbridgePlugin -> this will be returned by Corodva's require-calls
// (i.e. "singleton pattern")
module.exports = new GadgetbridgePlugin();
