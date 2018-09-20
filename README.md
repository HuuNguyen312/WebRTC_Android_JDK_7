
## Source code directories

+ https://webrtc.googlesource.com/src/+/master/webrtc/sdk/android/api/org/webrtc
+ https://webrtc.googlesource.com/src/+/master/webrtc/sdk/android/src/java/org/webrtc
+ https://webrtc.googlesource.com/src/+/master/webrtc/rtc_base/java/src/org/webrtc
+ https://webrtc.googlesource.com/src/+/master/webrtc/modules/audio_device/android/java/src/org/webrtc/voiceengine
+ https://webrtc.googlesource.com/src/+/master/webrtc/examples/androidapp/

## Debug native code in Android Studio

Edit `gradle.properties`, set `compile_native_code=true` and other variables according to your WebRTC checkout location, then enjoy :)

Note:

+ use the same version of Android SDK and NDK;
+ recreate `protoc` after updating webrtc repo, build WebRTC with ninja would create it;
+ delete `webrtc_build_dir` after updating webrtc repo;
