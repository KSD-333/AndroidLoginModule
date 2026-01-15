package com.example.loginmodule.loginAuth;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AuthManager - Central Firebase Authentication Handler
 * Handles phone auth, email auth, and user session management
 */
public class AuthManager {
    private static final String TAG = "AuthManager";
    private static AuthManager instance;

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    // Callbacks
    private AuthCallback authCallback;
    private OtpCallback otpCallback;

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);

        void onError(String message);
    }

    public interface OtpCallback {
        void onCodeSent(String verificationId);

        void onVerificationCompleted(PhoneAuthCredential credential);

        void onVerificationFailed(String message);
    }

    private AuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    /**
     * Check if user is already logged in
     */
    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Get current logged in user
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Send OTP to phone number
     */
    public void sendOtp(Activity activity, String phoneNumber, OtpCallback callback) {
        this.otpCallback = callback;

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        Log.d(TAG, "Verification completed automatically");
                        if (otpCallback != null) {
                            otpCallback.onVerificationCompleted(credential);
                        }
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Log.e(TAG, "Verification failed: " + e.getMessage());
                        if (otpCallback != null) {
                            otpCallback.onVerificationFailed(e.getMessage());
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String verId,
                            @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        Log.d(TAG, "Code sent to " + phoneNumber);
                        verificationId = verId;
                        resendToken = token;
                        if (otpCallback != null) {
                            otpCallback.onCodeSent(verId);
                        }
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    /**
     * Resend OTP
     */
    public void resendOtp(Activity activity, String phoneNumber, OtpCallback callback) {
        this.otpCallback = callback;

        if (resendToken == null) {
            sendOtp(activity, phoneNumber, callback);
            return;
        }

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setForceResendingToken(resendToken)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        if (otpCallback != null) {
                            otpCallback.onVerificationCompleted(credential);
                        }
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        if (otpCallback != null) {
                            otpCallback.onVerificationFailed(e.getMessage());
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String verId,
                            @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = verId;
                        resendToken = token;
                        if (otpCallback != null) {
                            otpCallback.onCodeSent(verId);
                        }
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    /**
     * Verify OTP entered by user
     */
    public void verifyOtp(String otp, AuthCallback callback) {
        this.authCallback = callback;

        if (verificationId == null) {
            if (callback != null) {
                callback.onError("Verification ID is null. Please request OTP again.");
            }
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        signInWithCredential(credential);
    }

    /**
     * Sign in with phone credential
     */
    private void signInWithCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        saveUserToFirestore(user);
                        if (authCallback != null) {
                            authCallback.onSuccess(user);
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Authentication failed";
                        if (authCallback != null) {
                            authCallback.onError(error);
                        }
                    }
                });
    }

    /**
     * Sign in with email and password
     */
    public void signInWithEmail(String email, String password, AuthCallback callback) {
        this.authCallback = callback;

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Sign in failed";
                        if (callback != null) {
                            callback.onError(error);
                        }
                    }
                });
    }

    /**
     * Create account with email and password
     */
    public void createAccountWithEmail(String email, String password, String name, AuthCallback callback) {
        this.authCallback = callback;

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        saveUserToFirestoreWithName(user, name, email);
                        if (callback != null) {
                            callback.onSuccess(user);
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Account creation failed";
                        if (callback != null) {
                            callback.onError(error);
                        }
                    }
                });
    }

    /**
     * Save user data to Firestore
     */
    private void saveUserToFirestore(FirebaseUser user) {
        if (user == null)
            return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("phone", user.getPhoneNumber());
        userData.put("lastLogin", System.currentTimeMillis());
        userData.put("createdAt", System.currentTimeMillis());

        firestore.collection("users")
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save user: " + e.getMessage()));
    }

    /**
     * Save user data to Firestore with name and email
     */
    private void saveUserToFirestoreWithName(FirebaseUser user, String name, String email) {
        if (user == null)
            return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("name", name);
        userData.put("email", email);
        userData.put("lastLogin", System.currentTimeMillis());
        userData.put("createdAt", System.currentTimeMillis());

        firestore.collection("users")
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save user: " + e.getMessage()));
    }

    /**
     * Get user data from Firestore
     */
    public void getUserData(String uid, OnCompleteListener<DocumentSnapshot> listener) {
        firestore.collection("users")
                .document(uid)
                .get()
                .addOnCompleteListener(listener);
    }

    /**
     * Sign out user
     */
    public void signOut() {
        firebaseAuth.signOut();
    }

    /**
     * Get verification ID for OTP
     */
    public String getVerificationId() {
        return verificationId;
    }

    /**
     * Set auth callback
     */
    public void setAuthCallback(AuthCallback callback) {
        this.authCallback = callback;
    }
}
