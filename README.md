# cordova-plugin-gadgetbridge
----

Cordova plugin for using [Gadgetbridge][1] as a library,
for accessing Pebble, Mi Band etc. devices on Android devices.

# Limitations

Supported platforms:  
 * `android`

Due to background services, _Gadgetbridge_ can be installed only once on an android-device:
 * either the _Gadgetbrige_ app itself
 * or exactly one app that uses this cordova-plugin


# Included Resources

Uses a [slightly modified][2] version of [Gadgetbridge][1] that is compiled
[as-library][3] instead of as an `application`, see  
`res/android/libs/gadgetbridge.aar`

The compiled `gadgetbridge.aar` is based on Gadgetbridge version 0.32.1
(versionCode 144, git hash 78e7c0145311b89365db3a0e80c7555976971b6f).


# Installation

```
cordova plugin add https://github.com/mmig/cordova-plugin-gadgetbridge
cordova plugin add https://github.com/mmig/cordova-plugin-gadgetbridge --variable GB_EXTENDED_PERMISSIONS=[false | true]
```

## Plugin Configuration

Some of the permission of `Gadgetbridge` are classified as _dangerous_, namely the SMS and Call Log permissions.
Apps that use these permissions e.g. need to meet some additional criteria in order to be allowed in the Google PlayStore,
see [Use of SMS or Call Log permission groups][5].

For this reason, the plugin __by default disables__ these permissions; as a result, 
the app does not listen to SMS and phone calls and the paired device(s) will not be notified 
with the corresponding information, e.g. of an incoming SMS or phone call.

For enabling the permissions and the corresponding functionality, install the plugin with argument `--variable GB_EXTENDED_PERMISSIONS=true`.

Or set the corresponding preference in `config.xml`:
```xml
<preference name="GB_EXTENDED_PERMISSIONS" value="true" />
``` 

**NOTE** the command-line argument is persisted in `config.xml` as `<variable name="GB_EXTENDED_PERMISSIONS" value="{THE VALUE}" />`,
     but changing the `variable` in `config.xml` after the plugin was installed, will have no effect.  
     For changing the setting after the plugin was installed, the corresponding preference tag can be used, e.g. the following would enable the extended permissions:	  
```xml
<preference name="GB_EXTENDED_PERMISSIONS" value="true" />
<plugin name="cordova-plugin-gadgetbridge" spec="git+https://github.com/mmig/cordova-plugin-gadgetbridge">
  <variable name="GB_EXTENDED_PERMISSIONS" value="false" />
</plugin>
``` 


# Usage

For details see [API docs][4].

**NOTE** that currently, the plugin is designed to work with *one* paired device.   

**NOTE** current implementation focuses on supporting the Mi-Band 2 device.

```javascript

//get plugin instance:
var GadgetbridgePlugin = window.cordova.plugins.gadgetbridgePlugin;

//NOTE success- and error-callback parameters are always optional

//open an activity / view of Gadgetbridge app.
//valid argument values:
// * "ControlCenterv2"
// * "SettingsActivity"
// * "MiBandPreferencesActivity"
// * "AppBlacklistActivity"
// * "DebugActivity"
// * "DbManagementActivity"
// * "DiscoveryActivity"
// * "MiBandPairingActivity"
// * "ChartsActivity"
// * "ConfigureAlarms"
// * "AlarmDetails"
GadgetbridgePlugin.openView(viewType, successCallback, errorCallback);


//get info of (first paired) wristband/tracker device
GadgetbridgePlugin.getDeviceInfo(successCallback({//or null
		name: String,
		address: String,
		model: String,
		type: String,
		firmware: String
	}, errorCallback
);

//Check, if paired device is (fully) connected
GadgetbridgePlugin.isConnected(successCallback(boolean), errorCallback);

//Connect to paired device
GadgetbridgePlugin.connect(successCallback, errorCallback);
GadgetbridgePlugin.connect(timeout, successCallback, errorCallback);

//Get batter level for the paired device
GadgetbridgePlugin.getBatteryLevel(successCallback(percent), errorCallback);
GadgetbridgePlugin.getBatteryLevel(timeout, successCallback(percent), errorCallback);

//////////////////// Notifications (on wristband/tracker device) ////////////////////////////

//Show notification on tracker/device
GadgetbridgePlugin.fireNotification(message, successCallback(completed), errorCallback);
GadgetbridgePlugin.fireNotification(message, repeat, successCallback(completed), errorCallback);
GadgetbridgePlugin.fireNotification(message, repeat, delay, successCallback(completed), errorCallback);

//Cancel notification (repeats) on tracker/device
GadgetbridgePlugin.cancelNotification(successCallback(prevented), errorCallback);


/////////////////////////////////// Activity Data /////////////////////////////////////////

//Start data synchronization with wristband/tracker device (pull data from device into application database)
GadgetbridgePlugin.synchronize(successCallback, errorCallback);
GadgetbridgePlugin.synchronize(timeout, successCallback, errorCallback);

//Get activity data from application database
GadgetbridgePlugin.retrieveData(successCallback([{
  activity: number,//double
  /**light sleep*/
  sleep1: number,//float
  /**deep sleep*/
  sleep2: number,//float
  notWorn: boolean,
  steps: number,//integer
  heartRate: number,// [0,255], NOTE value 255 indicates an invalid measurement
  raw: number,//integer
  timestamp: number//10-digit / UNIX timestamp, i.e. without milliseconds
}, ... ]), errorCallback);
//Get activity data from application database
GadgetbridgePlugin.retrieveData(start, successCallback(data), errorCallback);
GadgetbridgePlugin.retrieveData(start, end, successCallback(data), errorCallback);

//IMPORTANT: currently Gadgetbridge does not implement deleting data by time-ranges correctly,
//           so removeData() will always fail!
//           If appropriate, the method removeAllData() can be used instead.
 
//[DO NOT USE: use removeAllData() instead, if possible] Remove / delete activity data from application database
//GadgetbridgePlugin.removeData(successCallback, errorCallback);
//GadgetbridgePlugin.removeData(start, successCallback, errorCallback);
//GadgetbridgePlugin.removeData(start, end, successCallback, errorCallback);

//Remove / delete all Mi-Band 2 activity data from application database
GadgetbridgePlugin.removeAllData(successCallback, errorCallback);



/////////////////////// Configuration / Settings /////////////////////////////////


//Get configuration setting(s).
GadgetbridgePlugin.getConfig(settingsName/*string or Array<string>*/, successCallback({
	"the-settings-name": the_value,
	...
}), errorCallback);


//Set one or multiple configuration values
GadgetbridgePlugin.setConfig(name, value, successCallback(nameOfSuccessfullyChangedSetting), errorCallback);
GadgetbridgePlugin.setConfig({setting1: value1, ...}, successCallback([setting1, ...]), errorCallback);

// custom settings (i.e. in addition to standard Gadgetbridge settings):
// * "disableSyncToast" (boolean): hides/does not show Toast message when synchronizing data from device

////////////////////////// Events / Listeners //////////////////////////////////////

//listen to connection changes of device
GadgetbridgePlugin.onConnect(listener, errorCallback);
GadgetbridgePlugin.offConnect(listener, errorCallback);

//listen to button presses of (Mi-Band 2) device
//NOTE the button events depend on the corresponding configuration, i.e. settings for (can also be set via the "MiBandPreferencesActivity" activity/view)
//       "mi2_enable_button_action", "mi2_button_action_vibrate",  "mi_button_press_count",  "mi_button_press_count_max_delay", "mi_button_press_count_match_delay",  "mi_button_press_broadcast"
GadgetbridgePlugin.onButton(listener, errorCallback);
GadgetbridgePlugin.offButton(listener, errorCallback);

```



# Development

## Update Gadgetbridge

Compile Gadgetbridge as library and copy the `aar` file from `build/outputs/aar` (from inside the Gadgetbridge project)
into the plugin's directory (and rename file to):  
`res/android/libs/gadgetbridge.aar`

Ensure the Gadgetbridge's dependencies are up-to-date:
Open Gadgetbridge's `build.gradle` (for the app) file, and check the `dependencies {` section for the
entry `compile fileTree(dir: 'libs', include: ['*.jar'])`:  
copy&paste the `compile` entries of the `dependencies {` section into the plugin's gradle file at  
`res/android/res/gadgetbridgeBuild.gradle`

e.g. something like
```
...
dependencies {
    compile fileTree(dir: "libs", include: ["*.jar"])
    compile "com.android.support:appcompat-v7:27.1.1"
    compile "com.android.support:cardview-v7:27.1.1"
    compile "com.android.support:recyclerview-v7:27.1.1"
    compile "com.android.support:support-v4:27.1.1"
    compile "com.android.support:gridlayout-v7:27.1.1"
    compile "com.android.support:design:27.1.1"
    compile "com.android.support:palette-v7:27.1.1"
    compile("com.github.tony19:logback-android-classic:1.1.1-6") {
        exclude group: "com.google.android", module: "android"
    }
    compile "org.slf4j:slf4j-api:1.7.12"
    compile "com.github.Freeyourgadget:MPAndroidChart:5e5bd6c1d3e95c515d4853647ae554e48ee1d593"
    compile "com.github.pfichtner:durationformatter:0.1.1"
    compile "de.cketti.library.changelog:ckchangelog:1.2.2"
    compile "net.e175.klaus:solarpositioning:0.0.9"
    // use pristine greendao instead of our custom version, since our custom jitpack-packaged
    // version contains way too much and our custom patches are in the generator only.
    compile "org.greenrobot:greendao:2.2.1"
    compile "org.apache.commons:commons-lang3:3.5"
    compile "org.cyanogenmod:platform.sdk:6.0"

    implemenation(name:'gadgetbridge', ext:'aar')
}
...
```


[1]: https://github.com/Freeyourgadget/Gadgetbridge
[2]: https://github.com/mmig/Gadgetbridge
[3]: https://github.com/mmig/Gadgetbridge/tree/as-library
[4]: https://mmig.github.io/cordova-plugin-gadgetbridge/interfaces/_gadgetbridge_d_.gadgetbridgeplugin.html
[5]: https://support.google.com/googleplay/android-developer/answer/9047303
