# cordova-plugin-gadgetbridge
----

Cordova plugin for using [Gadgetbridge][1] as a library,
for accessing Pebble, Mi Band etc. devices.

# Included Resources

Uses a [slightly modified][2] version of [Gadgetbridge][1] that is compiled
[as-library][3] instead of as an `application`, see  
`res/android/libs/gadgetbridge.aar`

The compiled `gadgetbridge.aar` is based on Gadgetbridge version 0.21.0
(versionCode 101, git hash 1efd73af5e9673a740e1954d3c678c2a65cf0718).


# Installation

```
cordova plugin add https://github.com/mmig/cordova-plugin-gadgetbridge
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

//Connect to paired device.
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
  heartRate: number,// [0,255]
  raw: number,//integer
  timestamp: number//10-digit / UNIX timestamp
}, ... ]), errorCallback);
//Get activity data from application database
GadgetbridgePlugin.retrieveData(start, successCallback(data), errorCallback);
GadgetbridgePlugin.retrieveData(start, end, successCallback(data), errorCallback);

//Remove / delete activity data from application database
GadgetbridgePlugin.removeData(successCallback, errorCallback);
GadgetbridgePlugin.removeData(start, successCallback, errorCallback);
GadgetbridgePlugin.removeData(start, end, successCallback, errorCallback);



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
GadgetbridgePlugin.offConnect(successCallback, errorCallback);

//listen to button presses of (Mi-Band 2) device
GadgetbridgePlugin.onButton(listener, errorCallback);
GadgetbridgePlugin.offButton(successCallback, errorCallback);

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
