

export type ViewType = "ControlCenterv2" | "SettingsActivity" | "MiBandPreferencesActivity" | "AppBlacklistActivity" | "DebugActivity" | "DbManagementActivity" | "DiscoveryActivity" | "MiBandPairingActivity" | "ChartsActivity" | "ConfigureAlarms" | "AlarmDetails";

export interface GadgetbridgePlugin {

  /**
   * Open a (native) view.
   *
   * @param  {String} viewType the name of the view (see this.ACTION_START<n>)
   * @param  {Function} [successCallback] the success callback (no arguments)
   * @param  {Function} [errorCallback] the error callback
   */
  openView: (viewType: ViewType, successCallback?: SuccessCallback, errorCallback?: ErrorCallback) => void;


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
  getDeviceInfo: (successCallback: DeviceInfoSuccessCallback, errorCallback?: ErrorCallback) => void;

  /**
   * Check, if paired device is (fully) connected.
   *
   * @param  {Function} [successCallback] the success callback: successCallback(boolean)
   * @param  {Function} [errorCallback] the error callback
   */
  isConnected: (successCallback: IsConnectedSuccessCallback, errorCallback?: ErrorCallback) => void;

  /**
   * Connect to paired device.
   *
   * @param  {number} [timeout] OPTIONAL
   *                           timeout for connecting to device (if omitted, default timeout is used).
   * @param  {Function} [successCallback] the success callback: successCallback()
   * @param  {Function} [errorCallback] the error callback (e.g. due to timeout)
   */
  connect: (timeout?: number | SuccessCallback, successCallback?: SuccessCallback | ErrorCallback, errorCallback?: ErrorCallback) => void;

  /**
   * Add listener for connection-state changes.
   *
   * @param  {Function} [successCallback] the success callback: successCallback({address: String, state: String})
   * @param  {Function} [errorCallback] the error callback
   */
  onConnect: (successCallback?: OnConnectionSuccessCallback, errorCallback?: ErrorCallback) => void;

  /**
   * Remove listener for connection-state changes.
   *
   * @param  {Function} [successCallback] the listener / event handler: successCallback(didRemove: boolean)
   * @param  {Function} [errorCallback] the error callback
   */
  offConnect: (successCallback?: OffConnectionSuccessCallback, errorCallback?: ErrorCallback) => void;

  /**
   * Add listener for button presses (on tracker/device).
   *
   * @param  {Function} [successCallback] the listener / event handler: successCallback()
   * @param  {Function} [errorCallback] the error callback
   */
  onButton: (successCallback?: SuccessCallback, errorCallback?: ErrorCallback) => void;

  /**
   * Remove listener  for button presses (on tracker/device).
   *
   * @param  {Function} [successCallback] the success callback: successCallback(didRemove: boolean)
   * @param  {Function} [errorCallback] the error callback
   */
  offButton: (successCallback?: SuccessCallback, errorCallback?: ErrorCallback) => void;

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
  getBatteryLevel: (timeout?: number | boolean | ChargeSuccessCallback, details?: boolean | ChargeSuccessCallback | ErrorCallback, successCallback?: ChargeSuccessCallback | ErrorCallback, errorCallback?: ErrorCallback) => void;

  /**
   * Show notification on tracker/device (immediately).
   *
   * @param  {string} message the notification message text TODO title, body (, sender)
   * @param  {number} [repeat] number of times, for repeating to show text message on the device (DEFAULT: 3)
   * @param  {number} [delay] delay in milliseconds between repeating the text message on the device (DEFAULT: 10000 ms (10 sec))
   * @param  {Function} [successCallback] the success callback: successCallback(didComplete: boolean)
   * @param  {Function} [errorCallback] the error callback (e.g. if device is not connected)
   */
  fireNotification: (message: string, repeat?: number | NotificationSuccessCallback, delay?: number | NotificationSuccessCallback | ErrorCallback, successCallback?: NotificationSuccessCallback | ErrorCallback, errorCallback?: ErrorCallback) => void;

  /**
   * Cancel notification (repeats) on tracker/device.
   *
   * Has no effect, if currently no notification is active.
   *
   * @param  {Function} [successCallback] the success callback: (didCancel: boolean)
   * @param  {Function} [errorCallback] the error callback (e.g. if device is not connected)
   */
  cancelNotification: (successCallback?: NotificationSuccessCallback, errorCallback?: ErrorCallback) => void;

  /**
   * Start data synchronization with fitness-tracker device.
   *
   * @param  {number} [timeout] OPTIONAL
   *                           timeout for retrieving the batter level (if omitted, default timeout is used).
   * @param  {Function} [successCallback] the success callback: successCallback()
   * @param  {Function} [errorCallback] the error callback (e.g. due to timeout)
   */
  synchronize: (timeout?: number | SuccessCallback, successCallback?: SuccessCallback | ErrorCallback, errorCallback?: ErrorCallback) => void;

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
  retrieveData: (start?: number | SampleDataSuccessCallback, end?: number | SampleDataSuccessCallback | ErrorCallback, successCallback?: SampleDataSuccessCallback | ErrorCallback, errorCallback?: ErrorCallback) => void;

  /**
   * @deprecated FIXME currently not supported by Gadgebridge: cannot handle deletion of entities with composed-keys (and mi-band-entries key is (device_id, user_id)!)
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
  removeData: (start?: number | RemoveDataSuccessCallback, end?: number | RemoveDataSuccessCallback | ErrorCallback, RemoveDataSuccessCallback?: SuccessCallback | ErrorCallback, errorCallback?: ErrorCallback) => void;

  /**
   * Remove activity data from data base.
   *
   * @param  {Function} [successCallback] the success callback: successCallback()
   * @param  {Function} [errorCallback] the error callback (e.g. no device paired)
   */
  removeAllData: (successCallback?: RemoveDataSuccessCallback, errorCallback?: ErrorCallback) => void;

  /**
   * Get configuration setting(s).
   *
   * @param  {string|Array<string>} [settingsName]
   * 									the name(s) of the setting(s) to get.
   * 									If omitted, all settings are returned
   * @param  {Function} [successCallback] the success callback: successCallback({[id: string]: any})
   * @param  {Function} [errorCallback] the error callback
   */
  getConfig: (settingsName?: string | Array<string> | GetConfigSuccessCallback, successCallback?: GetConfigSuccessCallback | ErrorCallback, errorCallback?: ErrorCallback) => void;

  /**
   * Set one or multiple configuration values.
   *
   * Custom settings (i.e. in addition to standard Gadgetbridge settings):
   *  * "disableSyncToast" (boolean): hides/does not show Toast message when synchronizing data from device
   *
   *
   * @param  {string|{[id:string]: SettingsValue|null} name
   * 					if string: the name/ID of the setting/configuration field (NOTE: must also supply value argument)
   * 					if object: the settings (name & value) that should be applied
   * @param  {SettingsValue|null} [value]
   * 					the configuration value.
   * 					Using <code>null</code> will remove the setting, i.e. reset to default value.
   * @param  {Function} [successCallback] the success callback with the successfully set preference(s) as argument:
   * 											if arguments name & value: successCallback(string) NOTE: if not successful, the error-callback is invoked
   * 											if object argument: successCallback(Array<string>)
   * @param  {Function} [errorCallback] the error callback
   */
  setConfig: (name: string | {[id:string]: SettingsValue|null}, value?: SettingsValue|null|SetConfigSuccessCallback, successCallback?: SetConfigSuccessCallback|ErrorCallback, errorCallback?: ErrorCallback) => void;
}

export type ErrorCallback = (error)=>void;
export type SuccessCallback = ()=>void;
export type SampleDataSuccessCallback = (data: Array<DataSample>)=>void;
export type RemoveDataSuccessCallback = (result: {message: string, errors?: Array<string>, removed?: Array<DataSample>})=>void;
export type ChargeSuccessCallback = (chargePercent: number, details?: BatteryDetails)=>void;
export type DeviceInfoSuccessCallback = (info: DeviceInfo | null)=>void;
export type OffConnectionSuccessCallback = (didRemove: boolean)=>void;
export type OnConnectionSuccessCallback = (connectionState: DeviceConnectionState)=>void;
export type IsConnectedSuccessCallback = (connected: boolean)=>void;
export type NotificationSuccessCallback = (didComplete: boolean)=>void;
export type GetConfigSuccessCallback = (settings: {[id: string]: any})=>void;
export type SetConfigSuccessCallback = (applied: string | Array<string>)=>void;

export interface DeviceInfo {
  name: string;
  address: string;
  model: string;
  type: string;
  firmware: String
  state: ConnectionState;
}

export interface DeviceConnectionState {
  address: string;
  state: ConnectionState;
}

export interface BatteryDetails {
  level: number;
  threshold: number;
  state: BatteryState;
}

export type ConnectionState = "NOT_CONNECTED" |
  "WAITING_FOR_RECONNECT" |
  "CONNECTING" |
  "CONNECTED" |
  "INITIALIZING" |
  "AUTHENTICATION_REQUIRED" |
  "AUTHENTICATING" |
  "INITIALIZED";

export type BatteryState = "UNKNOWN" | "BATTERY_NORMAL" | "BATTERY_LOW" | "BATTERY_CHARGING" | "BATTERY_CHARGING_FULL" | "BATTERY_NOT_CHARGING_FULL";

export type SettingsValue = string | number | boolean | Array<string | number | boolean>;

export interface DataSample {

  activity?: number;//double
  /**light sleep*/
  sleep1?: number;//float
  /**deep sleep*/
  sleep2?: number;//float
  notWorn?: boolean;

  steps?: number;//integer
  heartRate?: number;// [0,255]

  raw: number;//integer
  timestamp: number;//10-digit / UNIX timestamp
}
