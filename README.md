# LSPosed 101 Navbar Button

Adds a fourth button to the Android 3-button navigation bar:

`[B] [Back] [Home] [Recents]`

Tapping **B** displays `LSPosed button clicked`.

## Requirements

- LSPosed with libxposed API 101 support.
- Android Studio with a recent JDK/Android SDK.
- `com.android.systemui` selected as the module scope. The module declares this scope statically.

This project uses `io.github.libxposed:api:101.0.1` as `compileOnly`.

## Build

Open the project in Android Studio and run:

```text
./gradlew assembleDebug
```

The APK is:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install / enable

1. Install the APK.
2. Open LSPosed.
3. Enable the module for **SystemUI / com.android.systemui**.
4. Restart SystemUI or reboot.
5. Switch to Android's **3-button navigation** mode.
6. You should see:

`[B] [Back] [Home] [Recents]`

Tap B to show the toast.

## Important

SystemUI is OEM-specific. The hook tries both AOSP class names:

- `com.android.systemui.navigationbar.NavigationBarInflaterView`
- `com.android.systemui.statusbar.phone.NavigationBarInflaterView`

If HyperOS 3 uses a different class or has substantially modified the navbar inflater, the log will show which hook was found. The next step would be adapting the hook to the exact HyperOS class/method.
