# My MQTT

The MQTT Android Service is an MQTT client library modified from https://github.com/eclipse/paho.mqtt.android
because old library don't support Android 12. It hasn't updated in a long time.

## How to use
1. Download ZIP and Extract All
2. Import library to your android project and select only mqtt folder
3. Edit the build.gradle (app level) file like below.
```
dependencies {
  ...
  implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
  implementation project(path: ':mqtt')
}
```
4. See example project at https://github.com/ElectApp/MyMQTT/tree/master/app


