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
    compile 'com.android.support:appcompat-v7:25.3.1'
    compile 'com.android.support:cardview-v7:25.3.1'
    compile 'com.android.support:recyclerview-v7:25.3.1'
    compile 'com.android.support:support-v4:25.3.1'
    compile 'com.android.support:gridlayout-v7:25.3.1'
    compile 'com.android.support:design:25.3.1'
    compile 'com.github.tony19:logback-android-classic:1.1.1-6'
    compile 'org.slf4j:slf4j-api:1.7.7'
    compile 'com.github.PhilJay:MPAndroidChart:v3.0.2'
    compile 'com.github.pfichtner:durationformatter:0.1.1'
    compile 'de.cketti.library.changelog:ckchangelog:1.2.2'
    compile 'net.e175.klaus:solarpositioning:0.0.9'
    // use pristine greendao instead of our custom version, since our custom jitpack-packaged
    // version contains way too much and our custom patches are in the generator only.
    compile 'org.greenrobot:greendao:2.2.1'
    compile 'org.apache.commons:commons-lang3:3.5'

    compile(name:'gadgetbridge', ext:'aar')
}
...
```


[1]: https://github.com/Freeyourgadget/Gadgetbridge
[2]: https://github.com/mmig/Gadgetbridge
[3]: https://github.com/mmig/Gadgetbridge/tree/as-library
