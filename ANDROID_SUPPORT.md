# Android support and verification

## Platform and build requirements

| Setting | Value |
| --- | --- |
| SDK/demo minimum Android version | Android 7.0 / API 24 |
| SDK/demo compile SDK | Android 17 / API 37 |
| Demo target SDK | 37 |
| Android Gradle Plugin | 9.2.1 (built-in Kotlin) |
| Gradle wrapper | 9.8.2, distribution SHA-256 verified |
| Kotlin / Compose compiler | 2.3.10 |
| Repository build/test JDK | Oracle JDK 27 |
| Java/Kotlin output compatibility | Java 8 |
| Robolectric | 4.17 |

Oracle JDK 27 is required for repository builds and tests. Set `JAVA_HOME`
and Android Studio's Gradle JDK to an Oracle JDK 27 installation.
`gradle/gradle-daemon-jvm.properties` pins the Gradle daemon to that version
and vendor. CI uses `actions/setup-java` with `distribution: oracle`.
No minimum Android version increase is introduced. Tool versions are pinned
to an API-37-compatible combination, rather than every latest dependency.

```sh
sdkmanager "platforms;android-37.0" "build-tools;36.0.0"
./gradlew testDebugUnitTest lintDebug assembleRelease assembleAndroidTest --console=plain
```

An app's `targetSdk` determines target-specific behavior. Updating this library
does not opt an existing host into new target behavior. The API-37-targeted demo
applies system-bar, cutout and keyboard insets centrally in `BaseActivity`.

## Automated coverage

| Check | Coverage |
| --- | --- |
| Card fragment lifecycle (Robolectric) | Every API 24–37; config retrieval, bridge success, queued/late callback cancellation, destruction and view recreation |
| Fingerprint and Google Pay unit tests | API 24, 28, 35, 36, 37; SDK behavior and request construction |
| Demo/backend unit tests | Existing checkout/session and mocked backend checks |
| Lint | SDK and demo, including API-use checks; warnings remain visible |
| Release build | SDK AAR and R8-minified demo APK |
| Device WebView test | Local HTML calls the real JavaScript bridge and deserializes card details, repeated after view recreation |
| GitHub Actions device matrix | Configured for every API 24–37 (SDK package `37.0`); inspect the actual workflow results |

```sh
./gradlew :app:connectedReleaseAndroidTest -PandroidTestBuildType=release --console=plain
```

The demo release is debug-key-signed for local/CI installation. The property
above adds `app/proguard-instrumentation.pro`: it retains shared runner
dependencies, resource IDs and test entry points R8 could otherwise inline out
of the app. SDK model fields and JavaScript methods still rely on the shipped
consumer rules. A normal `assembleRelease` does not load these test-only rules.

## Host integration

- Use a compile SDK compatible with the AndroidX dependencies; API 37 is the
  verified repository configuration. Keep `minSdk >= 24`.
- Keep Android System WebView/Chrome updated and enabled. OS API level alone
  does not determine JavaScript, TLS or hosted-tokenizer compatibility.
- Host tokenizer HTML over HTTPS and handle network/JavaScript errors.
  WebView debugging is controlled by the host, not globally enabled by the SDK.
- Google Pay needs supported device/account/region, compatible Play services
  and merchant configuration. Check readiness before launching.
- Minified hosts must include the AAR's consumer rules for Gson field names,
  card deserialization and JavaScript methods.
- Preserve PayPal return-URL/manifest setup when raising target SDK. PayPal is
  a demo integration of the separate PayPal SDK.
