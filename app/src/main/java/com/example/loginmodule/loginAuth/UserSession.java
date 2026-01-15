package com.example.loginmodule.loginAuth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseUser;

/**
 * UserSession - Manages local user session and preferences
 * Stores login state, user info for quick access
 */
public class UserSession {
    private static final String PREF_NAME = "LoginModuleSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_LOGIN_TYPE = "loginType";
    private static final String KEY_LAST_LOGIN = "lastLogin";

    public static final String LOGIN_TYPE_PHONE = "phone";
    public static final String LOGIN_TYPE_EMAIL = "email";
    public static final String LOGIN_TYPE_GOOGLE = "google";

    private static UserSession instance;
    private final SharedPreferences preferences;
    private final SharedPreferences.Editor editor;

    private UserSession(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public static synchronized UserSession getInstance(Context context) {
        if (instance == null) {
            instance = new UserSession(context);
        }
        return instance;
    }

    /**
     * Create session after successful login
     */
    public void createSession(FirebaseUser user, String loginType) {
        if (user == null)
            return;

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, user.getUid());
        editor.putString(KEY_USER_PHONE, user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        editor.putString(KEY_USER_EMAIL, user.getEmail() != null ? user.getEmail() : "");
        editor.putString(KEY_USER_NAME, user.getDisplayName() != null ? user.getDisplayName() : "");
        editor.putString(KEY_LOGIN_TYPE, loginType);
        editor.putLong(KEY_LAST_LOGIN, System.currentTimeMillis());
        editor.apply();
    }

    /**
     * Update user name in session
     */
    public void setUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    /**
     * Update user email in session
     */
    public void setUserEmail(String email) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }

    /**
     * Update user phone in session
     */
    public void setUserPhone(String phone) {
        editor.putString(KEY_USER_PHONE, phone);
        editor.apply();
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get user ID
     */
    public String getUserId() {
        return preferences.getString(KEY_USER_ID, "");
    }

    /**
     * Get user name
     */
    public String getUserName() {
        return preferences.getString(KEY_USER_NAME, "");
    }

    /**
     * Get user phone
     */
    public String getUserPhone() {
        return preferences.getString(KEY_USER_PHONE, "");
    }

    /**
     * Get user email
     */
    public String getUserEmail() {
        return preferences.getString(KEY_USER_EMAIL, "");
    }

    /**
     * Get login type
     */
    public String getLoginType() {
        return preferences.getString(KEY_LOGIN_TYPE, "");
    }

    /**
     * Get last login timestamp
     */
    public long getLastLogin() {
        return preferences.getLong(KEY_LAST_LOGIN, 0);
    }

    /**
     * Get display identifier (phone or email based on login type)
     */
    public String getDisplayIdentifier() {
        String loginType = getLoginType();
        if (LOGIN_TYPE_PHONE.equals(loginType)) {
            return getUserPhone();
        } else {
            return getUserEmail();
        }
    }

    /**
     * Clear session on logout
     */
    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    /**
     * Check if session is valid (has user ID)
     */
    public boolean isValidSession() {
        return isLoggedIn() && !getUserId().isEmpty();
    }
}
