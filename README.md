
## Source code directories

+ https://webrtc.googlesource.com/src/+/master/

## Use Gralde

```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
  implementation 'com.github.ngochuu90:WebRTC_Android_JDK_7:0.1'
}
```
