# Google Play Store Upload Guide

## Bundle File
**Location:** `C:\code\github\infection\app\build\outputs\bundle\release\app-release.aab`  
**Size:** 4.47 MB  
**Status:** ✓ Signed & Ready

## What Was Set Up

### 1. Keystore Generated
- **File:** `app/keystore/funfection.jks`
- **Validity:** 10,000 days (27+ years)
- **Algorithm:** RSA 2048-bit
- **Store Password:** `funfection123`
- **Key Alias:** `funfection`
- **Key Password:** `funfection123`

### 2. Build Configuration Updated
The `app/build.gradle` now includes:
- `signingConfigs { release { ... } }` — points to the keystore
- `buildTypes { release { signingConfig signingConfigs.release } }` — signs all release builds

### 3. Release Bundle Built
Ran `./gradlew bundleRelease` which:
- Compiled all Java/Kotlin code in release mode
- Minified & processed resources
- Generated signed AAB (Android App Bundle)

---

## Upload Steps

1. **Go to [Google Play Console](https://play.google.com/console)**
2. **Select or create an app** with package name `com.example.funfection`
3. **Navigate:** Testing → Internal Testing (or Production)
4. **Click "Create Release"**
5. **Upload AAB:** Drag `app-release.aab` into the upload area
6. **Review store listing, pricing, privacy, etc.**
7. **Submit for review**

---

## Important Notes

⚠️ **Keystore Security:**
- The keystore file (`app/keystore/funfection.jks`) is currently in the repo with hardcoded passwords in `build.gradle`
- For production: Move the keystore outside the repo and use environment variables or `local.properties` for sensitive credentials
- Never commit keystores with plaintext passwords to public repos

✓ **Version Management:**
- Current: `versionCode 1`, `versionName '1.0'`
- Each Play Store update must increment `versionCode`
- Update in `app/build.gradle` before building future releases

✓ **Target & Min SDK:**
- `minSdk 24` (Android 7.0)
- `compileSdk 36` (Android 15)
- `targetSdk 36` (Android 15)

---

## Future Builds

To build another release bundle:
```bash
./gradlew bundleRelease
```

The bundle will be output to the same location and ready to upload.

