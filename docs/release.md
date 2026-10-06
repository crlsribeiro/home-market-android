# Release

How to build a signed release of the Android app and what the Play Store needs. No key, password or
`google-services.json` is ever committed.

## Release build

The `release` build type runs R8 (code shrinking, obfuscation and resource shrinking). Rules that the
libraries don't ship themselves are in [`app/proguard-rules.pro`](../app/proguard-rules.pro). CI runs
`assembleRelease` on every pull request, so an R8 failure shows up before merge.

R8 only fails at build time for missing classes. Before each release, install the release build on a
device and walk through sign-in (email and Google), the list, shopping mode, a receipt upload and the
history, because a missing keep rule only shows up as a crash at runtime.

## Signing

1. Create the upload key once, and keep it and its passwords outside the repository (a password
   manager, for example):

   ```
   keytool -genkeypair -v -keystore ~/keys/homemarket-upload.jks -alias upload \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Create `keystore.properties` in the project root. The file is git-ignored.

   ```
   storeFile=/Users/you/keys/homemarket-upload.jks
   storePassword=...
   keyAlias=upload
   keyPassword=...
   ```

   CI can use the environment variables `HOMEMARKET_STORE_FILE`, `HOMEMARKET_STORE_PASSWORD`,
   `HOMEMARKET_KEY_ALIAS` and `HOMEMARKET_KEY_PASSWORD` instead.

3. Build the bundle for the Play Store:

   ```
   ./gradlew bundleRelease
   ```

   The file is `app/build/outputs/bundle/release/app-release.aab`. Without a signing configuration the
   build still works but the bundle is unsigned.

4. Enrol in Play App Signing when you create the app in the Play Console. Google keeps the app signing
   key; the key above is only the upload key.

5. Add the SHA-1 of the **app signing key** (Play Console → Setup → App signing) to the Android app in
   the Firebase console, otherwise Google sign-in fails in the Play Store build.

## Version

`versionCode` and `versionName` are in [`app/build.gradle.kts`](../app/build.gradle.kts). Increase
`versionCode` by one for every upload to the Play Store.

## Play Store checklist

- [ ] App icon and feature graphic (512 × 512 and 1024 × 500).
- [ ] Phone screenshots in English and Portuguese (Brazil).
- [ ] Privacy policy URL: `https://crlsribeiro.github.io/home-market-privacy/` (the same link as in the
      registration screen).
- [ ] Account deletion: the app deletes the account in Account → Delete account. The Play Console also
      asks for a web link where users can request deletion without the app.
- [ ] Data safety form: name, email and phone (account), profile and item photos and receipt photos
      (user content), FCM token once notifications ship. Data is sent to Firebase over HTTPS; users can
      delete their account.
- [ ] Content rating questionnaire and target audience (not for children).
- [ ] Internal testing track first, with the release build installed from the Play Store.
