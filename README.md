# Kang Tester

Test harness for ArcHeroNG: a small utility to check basic functionalities of Android.

## Building

You need to install Android SDK API 33 with corresponding NDK. Docker image `fabernovel/android:api-33-ndk-v1.7.0` is a good starting point.

Build with the following command:

```
./gradlew assemble
```

## Features
* Audio
* Touch
* Camera
* Sensor
* Notification
* Wi-Fi Scanner
* Video Playback
* Screen Capture
* External Storage
* Overlay Window
* RK3588 NPU
