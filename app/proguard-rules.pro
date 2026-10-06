# R8 rules for the release build. Most libraries (Firebase, Hilt, Room, ML Kit, Coil,
# kotlinx.serialization) ship their own consumer rules; only what they don't cover is listed here.

# Credential Manager loads the Play services provider by reflection (androidx.credentials docs).
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# Keep line numbers in crash reports, with the source file name hidden.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
