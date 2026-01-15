# Universal Login Module

A reusable, modern login module for Android applications with beautiful Zomato-like UI, animations, and Firebase authentication.

## 📂 Folder Structure

```
loginAuth/                    # Authentication Logic
├── AuthManager.java          # Firebase Auth handler (Phone OTP, Email)
├── AccountDetector.java      # Auto-detect system phone/email
├── UserSession.java          # Local session management
└── OtpHelper.java            # OTP auto-read & timer

loginUi/                      # User Interface
├── LoginActivity.java        # Main login screen
├── OtpVerificationActivity.java  # OTP verification
└── EmailLoginActivity.java   # Email login/signup

res/layout/
├── activity_login.xml
├── activity_otp_verification.xml
└── activity_email_login.xml

res/drawable/                 # Required drawables
├── header_gradient.xml
├── top_rounded_sheet.xml
├── input_field_bg.xml
├── otp_field_bg.xml
├── social_button_bg.xml
├── suggestions_bg.xml
├── suggestion_item_bg.xml
├── fade_overlay.xml
├── ic_back_arrow.xml
├── ic_dropdown.xml
├── ic_person.xml
├── ic_lock.xml
├── ic_google_placeholder.xml
├── ic_email_placeholder.xml
└── ic_flag_india_placeholder.xml
```

## 🚀 How to Reuse in Any Project

### Step 1: Copy Folders
Copy the following folders to your project:
- `java/com/example/loginmodule/loginAuth/` → Your package path
- `java/com/example/loginmodule/loginUi/` → Your package path
- `res/layout/activity_login.xml`, `activity_otp_verification.xml`, `activity_email_login.xml`
- `res/drawable/` (all listed drawables above)

### Step 2: Update Package Names
In all Java files, update the package declaration:
```java
package com.yourpackage.loginAuth;  // or loginUi
```

Update imports accordingly.

### Step 3: Add Dependencies
Add to your `app/build.gradle`:
```gradle
dependencies {
    // Firebase
    implementation platform('com.google.firebase:firebase-bom:34.7.0')
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-firestore'
    
    // Lottie Animations
    implementation 'com.airbnb.android:lottie:6.6.0'
    
    // Material Components
    implementation 'com.google.android.material:material:1.10.0'
    
    // SMS Retriever API
    implementation 'com.google.android.gms:play-services-auth:21.0.0'
    implementation 'com.google.android.gms:play-services-auth-api-phone:18.0.2'
}
```

### Step 4: Add Permissions
Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.GET_ACCOUNTS" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
```

### Step 5: Register Activities
Add to `AndroidManifest.xml`:
```xml
<activity
    android:name=".loginUi.LoginActivity"
    android:exported="true"
    android:screenOrientation="portrait"
    android:windowSoftInputMode="adjustResize">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity
    android:name=".loginUi.OtpVerificationActivity"
    android:exported="false"
    android:screenOrientation="portrait" />

<activity
    android:name=".loginUi.EmailLoginActivity"
    android:exported="false"
    android:screenOrientation="portrait" />
```

### Step 6: Add Styles
Add to `values/themes.xml`:
```xml
<style name="OtpEditText">
    <item name="android:layout_width">48dp</item>
    <item name="android:layout_height">56dp</item>
    <item name="android:background">@drawable/otp_field_bg</item>
    <item name="android:gravity">center</item>
    <item name="android:inputType">number</item>
    <item name="android:maxLength">1</item>
    <item name="android:textColor">#1A1A2E</item>
    <item name="android:textSize">20sp</item>
    <item name="android:textStyle">bold</item>
</style>
```

### Step 7: Add Colors
Add to `values/colors.xml`:
```xml
<color name="primary_color">#EF5350</color>
<color name="primary_dark">#D32F2F</color>
<color name="text_primary">#1A1A2E</color>
<color name="text_secondary">#666666</color>
<color name="border_color">#E0E0E0</color>
```

### Step 8: Firebase Setup
1. Create a Firebase project
2. Add `google-services.json` to your app folder
3. Enable Phone Authentication in Firebase Console
4. Enable Email/Password Authentication

## ✨ Features

- **Phone OTP Login** - Firebase Phone Authentication
- **Email Login/Signup** - Full email authentication flow
- **Auto-detect Phone/Email** - Reads system accounts for quick login
- **SMS Auto-read** - Automatically reads OTP from SMS
- **Beautiful Animations** - Lottie animations throughout
- **Modern UI** - Zomato-inspired clean design
- **Reusable** - Copy & paste into any project

## 🎨 Customization

### Change Primary Color
Update `header_gradient.xml` and `colors.xml` with your brand colors.

### Change Animations
Replace Lottie URLs in activities:
```java
lottieAnimationView.setAnimationFromUrl("YOUR_LOTTIE_URL");
```

### Navigate After Login
Update `navigateToMain()` in activities to go to your desired screen:
```java
private void navigateToMain() {
    startActivity(new Intent(this, YourMainActivity.class));
    finish();
}
```

## 📱 Supported

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34+
- **Languages**: Java

## 📄 License

Free to use in any project. Customize as needed!
