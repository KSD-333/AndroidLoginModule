package com.example.loginmodule.loginAuth;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Patterns;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AccountDetector - Detects system phone numbers and email accounts
 * Helps users by auto-suggesting their contact information
 */
public class AccountDetector {
    private static final String TAG = "AccountDetector";

    private final Context context;

    public AccountDetector(Context context) {
        this.context = context;
    }

    /**
     * Data class to hold detected account info
     */
    public static class AccountInfo {
        public List<String> phoneNumbers;
        public List<String> emails;
        public String primaryPhone;
        public String primaryEmail;

        public AccountInfo() {
            phoneNumbers = new ArrayList<>();
            emails = new ArrayList<>();
            primaryPhone = "";
            primaryEmail = "";
        }

        public boolean hasPhone() {
            return primaryPhone != null && !primaryPhone.isEmpty();
        }

        public boolean hasEmail() {
            return primaryEmail != null && !primaryEmail.isEmpty();
        }
    }

    /**
     * Check if required permission is granted
     */
    public boolean hasAccountPermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if phone permission is granted
     */
    public boolean hasPhonePermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if contacts permission is granted
     */
    public boolean hasContactsPermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Get all detected account information
     */
    public AccountInfo detectAccounts() {
        AccountInfo accountInfo = new AccountInfo();

        // Detect emails from accounts
        accountInfo.emails = detectEmails();
        if (!accountInfo.emails.isEmpty()) {
            accountInfo.primaryEmail = accountInfo.emails.get(0);
        }

        // Detect phone numbers
        accountInfo.phoneNumbers = detectPhoneNumbers();
        if (!accountInfo.phoneNumbers.isEmpty()) {
            accountInfo.primaryPhone = accountInfo.phoneNumbers.get(0);
        }

        return accountInfo;
    }

    /**
     * Detect email accounts on the device
     */
    @SuppressLint("HardwareIds")
    public List<String> detectEmails() {
        Set<String> emailSet = new HashSet<>();

        try {
            if (hasAccountPermission()) {
                AccountManager accountManager = AccountManager.get(context);
                Account[] accounts = accountManager.getAccounts();

                for (Account account : accounts) {
                    String email = account.name;
                    if (isValidEmail(email)) {
                        emailSet.add(email);
                    }
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(emailSet);
    }

    /**
     * Detect phone numbers from device
     */
    @SuppressLint("HardwareIds")
    public List<String> detectPhoneNumbers() {
        Set<String> phoneSet = new HashSet<>();

        try {
            // Try to get from TelephonyManager
            if (hasPhonePermission()) {
                TelephonyManager telephonyManager = (TelephonyManager) context
                        .getSystemService(Context.TELEPHONY_SERVICE);

                if (telephonyManager != null) {
                    String line1Number = telephonyManager.getLine1Number();
                    if (line1Number != null && !line1Number.isEmpty()) {
                        phoneSet.add(formatPhoneNumber(line1Number));
                    }
                }
            }

            // Try to get from SIM subscription (Android 5.1+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && hasPhonePermission()) {
                // Additional phone detection for dual SIM
                detectFromSubscriptions(phoneSet);
            }

        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(phoneSet);
    }

    /**
     * Detect phone numbers from SIM subscriptions
     */
    @SuppressLint("MissingPermission")
    private void detectFromSubscriptions(Set<String> phoneSet) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                android.telephony.SubscriptionManager subscriptionManager = (android.telephony.SubscriptionManager) context
                        .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

                if (subscriptionManager != null && hasPhonePermission()) {
                    List<android.telephony.SubscriptionInfo> subscriptionInfoList = subscriptionManager
                            .getActiveSubscriptionInfoList();

                    if (subscriptionInfoList != null) {
                        for (android.telephony.SubscriptionInfo info : subscriptionInfoList) {
                            String number = info.getNumber();
                            if (number != null && !number.isEmpty()) {
                                phoneSet.add(formatPhoneNumber(number));
                            }
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get user's profile phone number from contacts
     */
    @SuppressLint("Range")
    public String getProfilePhoneNumber() {
        if (!hasContactsPermission()) {
            return "";
        }

        try {
            Uri uri = ContactsContract.Profile.CONTENT_URI;
            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[] { ContactsContract.Profile.DISPLAY_NAME },
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                // User has a profile, try to get associated phone
                cursor.close();

                // Query profile phones
                Uri phonesUri = Uri.withAppendedPath(
                        ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY);

                Cursor phoneCursor = context.getContentResolver().query(
                        phonesUri,
                        new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER },
                        ContactsContract.Data.MIMETYPE + " = ?",
                        new String[] { ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE },
                        null);

                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    String phone = phoneCursor.getString(
                            phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phoneCursor.close();
                    return formatPhoneNumber(phone);
                }

                if (phoneCursor != null) {
                    phoneCursor.close();
                }
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Format phone number (remove special chars, ensure country code)
     */
    public String formatPhoneNumber(String phone) {
        if (phone == null)
            return "";

        // Remove all non-digit characters except +
        String cleaned = phone.replaceAll("[^\\d+]", "");

        // If number doesn't start with +, assume Indian number
        if (!cleaned.startsWith("+")) {
            // Remove leading 0 if present
            if (cleaned.startsWith("0")) {
                cleaned = cleaned.substring(1);
            }
            // Add India country code
            if (cleaned.length() == 10) {
                cleaned = "+91" + cleaned;
            }
        }

        return cleaned;
    }

    /**
     * Get only the local phone number (without country code)
     */
    public String getLocalNumber(String phone) {
        if (phone == null)
            return "";

        String cleaned = phone.replaceAll("[^\\d]", "");

        // Remove country code if present
        if (cleaned.startsWith("91") && cleaned.length() > 10) {
            cleaned = cleaned.substring(2);
        }

        // Return last 10 digits
        if (cleaned.length() > 10) {
            cleaned = cleaned.substring(cleaned.length() - 10);
        }

        return cleaned;
    }

    /**
     * Required permissions for account detection
     */
    public static String[] getRequiredPermissions() {
        return new String[] {
                Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS
        };
    }
}
