package com.example.loginmodule.loginAuth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OtpHelper - Handles OTP auto-reading and countdown timer
 * Uses SMS Retriever API for automatic OTP detection
 */
public class OtpHelper {
    private static final String TAG = "OtpHelper";
    private static final long DEFAULT_COUNTDOWN_MS = 60000; // 60 seconds
    private static final long COUNTDOWN_INTERVAL_MS = 1000; // 1 second

    private final Context context;
    private CountDownTimer countDownTimer;
    private OtpListener otpListener;
    private boolean isTimerRunning = false;
    private BroadcastReceiver smsReceiver;

    public interface OtpListener {
        void onOtpReceived(String otp);

        void onTimerTick(long secondsRemaining);

        void onTimerFinished();
    }

    public OtpHelper(Context context) {
        this.context = context;
    }

    /**
     * Set OTP listener
     */
    public void setOtpListener(OtpListener listener) {
        this.otpListener = listener;
    }

    /**
     * Start SMS Retriever for auto OTP reading
     */
    public void startSmsRetriever() {
        SmsRetrieverClient client = SmsRetriever.getClient(context);
        Task<Void> task = client.startSmsRetriever();

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // SMS Retriever started successfully
                registerSmsReceiver();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Failed to start SMS Retriever
                e.printStackTrace();
            }
        });
    }

    /**
     * Register SMS broadcast receiver
     */
    private void registerSmsReceiver() {
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
                        if (status != null) {
                            switch (status.getStatusCode()) {
                                case CommonStatusCodes.SUCCESS:
                                    String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                                    if (message != null) {
                                        String otp = extractOtp(message);
                                        if (otpListener != null && otp != null) {
                                            otpListener.onOtpReceived(otp);
                                        }
                                    }
                                    break;
                                case CommonStatusCodes.TIMEOUT:
                                    // Timeout - OTP was not auto-read
                                    break;
                            }
                        }
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(smsReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(smsReceiver, intentFilter);
        }
    }

    /**
     * Extract OTP from SMS message
     */
    private String extractOtp(String message) {
        // Pattern to find 4-6 digit OTP
        Pattern pattern = Pattern.compile("\\b(\\d{4,6})\\b");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Start countdown timer for resend OTP
     */
    public void startResendTimer() {
        startResendTimer(DEFAULT_COUNTDOWN_MS);
    }

    /**
     * Start countdown timer with custom duration
     */
    public void startResendTimer(long durationMs) {
        stopTimer();

        isTimerRunning = true;
        countDownTimer = new CountDownTimer(durationMs, COUNTDOWN_INTERVAL_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                if (otpListener != null) {
                    otpListener.onTimerTick(secondsRemaining);
                }
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                if (otpListener != null) {
                    otpListener.onTimerFinished();
                }
            }
        }.start();
    }

    /**
     * Stop countdown timer
     */
    public void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
            isTimerRunning = false;
        }
    }

    /**
     * Check if timer is running
     */
    public boolean isTimerRunning() {
        return isTimerRunning;
    }

    /**
     * Format OTP for display with separators
     */
    public static String formatOtpDisplay(String otp) {
        if (otp == null || otp.length() < 4) {
            return otp;
        }

        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < otp.length(); i++) {
            formatted.append(otp.charAt(i));
            if (i < otp.length() - 1) {
                formatted.append(" ");
            }
        }
        return formatted.toString();
    }

    /**
     * Validate OTP format
     */
    public static boolean isValidOtp(String otp, int length) {
        if (otp == null)
            return false;
        return otp.matches("\\d{" + length + "}");
    }

    /**
     * Validate 6-digit OTP
     */
    public static boolean isValidOtp(String otp) {
        return isValidOtp(otp, 6);
    }

    /**
     * Convert seconds to mm:ss format
     */
    public static String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        stopTimer();
        if (smsReceiver != null) {
            try {
                context.unregisterReceiver(smsReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver was not registered
            }
            smsReceiver = null;
        }
    }
}
