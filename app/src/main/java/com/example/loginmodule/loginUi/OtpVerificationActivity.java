package com.example.loginmodule.loginUi;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.example.loginmodule.MainActivity;
import com.example.loginmodule.R;
import com.example.loginmodule.loginAuth.AuthManager;
import com.example.loginmodule.loginAuth.OtpHelper;
import com.example.loginmodule.loginAuth.UserSession;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;

/**
 * OtpVerificationActivity - OTP Entry Screen with auto-read and animations
 * Features: 6-digit OTP input, auto-read SMS, resend timer, verification
 */
public class OtpVerificationActivity extends AppCompatActivity {

    // UI Components
    private LottieAnimationView lottieVerification;
    private ImageView btnBack;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private EditText[] otpFields;
    private LinearLayout otpContainer;
    private MaterialButton btnVerify;
    private TextView tvResendInfo;
    private TextView tvResend;
    private ProgressBar progressBar;

    // Data
    private String phoneNumber;
    private String phoneDisplay;

    // Auth & Helpers
    private AuthManager authManager;
    private OtpHelper otpHelper;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        getIntentData();
        initializeComponents();
        setupUI();
        setupListeners();
        sendOtp();
        startEntranceAnimations();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        phoneNumber = intent.getStringExtra("phone");
        phoneDisplay = intent.getStringExtra("phone_display");

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeComponents() {
        authManager = AuthManager.getInstance();
        userSession = UserSession.getInstance(this);
        otpHelper = new OtpHelper(this);

        // Find views
        lottieVerification = findViewById(R.id.lottieVerification);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        otpContainer = findViewById(R.id.otpContainer);
        btnVerify = findViewById(R.id.btnVerify);
        tvResendInfo = findViewById(R.id.tvResendInfo);
        tvResend = findViewById(R.id.tvResend);
        progressBar = findViewById(R.id.progressBar);

        // Initialize OTP fields
        otpFields = new EditText[6];
        otpFields[0] = findViewById(R.id.etOtp1);
        otpFields[1] = findViewById(R.id.etOtp2);
        otpFields[2] = findViewById(R.id.etOtp3);
        otpFields[3] = findViewById(R.id.etOtp4);
        otpFields[4] = findViewById(R.id.etOtp5);
        otpFields[5] = findViewById(R.id.etOtp6);
    }

    private void setupUI() {
        // Set Lottie animation
        lottieVerification.setAnimationFromUrl(
                "https://assets2.lottiefiles.com/packages/lf20_xlmz9xwm.json");
        lottieVerification.playAnimation();

        // Set subtitle with phone number
        tvSubtitle.setText("Enter the 6-digit code sent to\n" +
                (phoneDisplay != null ? phoneDisplay : phoneNumber));
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // OTP field listeners
        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;

            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpFields.length - 1) {
                        otpFields[index + 1].requestFocus();
                        animateOtpField(otpFields[index], true);
                    }
                    updateVerifyButtonState();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                        event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (otpFields[index].getText().length() == 0 && index > 0) {
                        otpFields[index - 1].requestFocus();
                        otpFields[index - 1].setText("");
                        animateOtpField(otpFields[index], false);
                    }
                }
                return false;
            });

            // Focus animation
            otpFields[i].setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    highlightOtpField(otpFields[index], true);
                } else {
                    highlightOtpField(otpFields[index], false);
                }
            });
        }

        // Verify button
        btnVerify.setOnClickListener(v -> verifyOtp());

        // Resend button
        tvResend.setOnClickListener(v -> {
            if (!otpHelper.isTimerRunning()) {
                resendOtp();
            }
        });

        // OTP Helper listener for auto-read
        otpHelper.setOtpListener(new OtpHelper.OtpListener() {
            @Override
            public void onOtpReceived(String otp) {
                runOnUiThread(() -> autoFillOtp(otp));
            }

            @Override
            public void onTimerTick(long secondsRemaining) {
                runOnUiThread(() -> {
                    tvResendInfo.setText("Resend OTP in ");
                    tvResend.setText(OtpHelper.formatTime(secondsRemaining));
                    tvResend.setEnabled(false);
                    tvResend.setAlpha(0.5f);
                });
            }

            @Override
            public void onTimerFinished() {
                runOnUiThread(() -> {
                    tvResendInfo.setText("Didn't receive OTP? ");
                    tvResend.setText("Resend");
                    tvResend.setEnabled(true);
                    tvResend.setAlpha(1f);
                });
            }
        });
    }

    private void sendOtp() {
        showLoading(true);

        authManager.sendOtp(this, phoneNumber, new AuthManager.OtpCallback() {
            @Override
            public void onCodeSent(String verificationId) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(OtpVerificationActivity.this,
                            "OTP sent successfully!", Toast.LENGTH_SHORT).show();
                    otpHelper.startSmsRetriever();
                    otpHelper.startResendTimer();
                    otpFields[0].requestFocus();
                });
            }

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                runOnUiThread(() -> {
                    showLoading(false);
                    // Auto verification - will be handled by AuthManager
                });
            }

            @Override
            public void onVerificationFailed(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(message);
                    shakeOtpContainer();
                });
            }
        });
    }

    private void resendOtp() {
        clearOtpFields();
        showLoading(true);

        authManager.resendOtp(this, phoneNumber, new AuthManager.OtpCallback() {
            @Override
            public void onCodeSent(String verificationId) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(OtpVerificationActivity.this,
                            "OTP resent!", Toast.LENGTH_SHORT).show();
                    otpHelper.startResendTimer();
                    otpFields[0].requestFocus();
                });
            }

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                runOnUiThread(() -> showLoading(false));
            }

            @Override
            public void onVerificationFailed(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(message);
                });
            }
        });
    }

    private void verifyOtp() {
        String otp = getOtpFromFields();

        if (!OtpHelper.isValidOtp(otp)) {
            showError("Please enter valid 6-digit OTP");
            shakeOtpContainer();
            return;
        }

        showLoading(true);

        authManager.verifyOtp(otp, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                runOnUiThread(() -> {
                    showLoading(false);

                    // Create session
                    userSession.createSession(user, UserSession.LOGIN_TYPE_PHONE);

                    // Show success animation
                    showSuccessAnimation(() -> navigateToMain());
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(message);
                    shakeOtpContainer();
                    clearOtpFields();
                });
            }
        });
    }

    private String getOtpFromFields() {
        StringBuilder otp = new StringBuilder();
        for (EditText field : otpFields) {
            otp.append(field.getText().toString());
        }
        return otp.toString();
    }

    private void autoFillOtp(String otp) {
        if (otp == null || otp.length() != 6)
            return;

        for (int i = 0; i < 6 && i < otpFields.length; i++) {
            final int index = i;
            final String digit = String.valueOf(otp.charAt(i));

            otpFields[index].postDelayed(() -> {
                otpFields[index].setText(digit);
                animateOtpField(otpFields[index], true);
            }, i * 80);
        }

        // Auto verify after filling
        otpFields[5].postDelayed(this::verifyOtp, 600);
    }

    private void clearOtpFields() {
        for (EditText field : otpFields) {
            field.setText("");
        }
        otpFields[0].requestFocus();
    }

    private void updateVerifyButtonState() {
        String otp = getOtpFromFields();
        boolean isValid = otp.length() == 6;

        btnVerify.setEnabled(isValid);
        btnVerify.setAlpha(isValid ? 1f : 0.6f);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ==================== Animations ====================

    private void startEntranceAnimations() {
        // Title animation
        tvTitle.setAlpha(0f);
        tvTitle.setTranslationY(-30f);
        tvTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .start();

        // Subtitle animation
        tvSubtitle.setAlpha(0f);
        tvSubtitle.setTranslationY(-20f);
        tvSubtitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(350)
                .start();

        // OTP fields animation
        for (int i = 0; i < otpFields.length; i++) {
            EditText field = otpFields[i];
            field.setAlpha(0f);
            field.setScaleX(0.5f);
            field.setScaleY(0.5f);
            field.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setStartDelay(450 + (i * 50))
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        }

        // Verify button animation
        btnVerify.setAlpha(0f);
        btnVerify.setTranslationY(30f);
        btnVerify.animate()
                .alpha(0.6f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(700)
                .start();
    }

    private void animateOtpField(EditText field, boolean filled) {
        field.animate()
                .scaleX(filled ? 1.1f : 1f)
                .scaleY(filled ? 1.1f : 1f)
                .setDuration(100)
                .withEndAction(() -> {
                    field.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void highlightOtpField(EditText field, boolean focused) {
        int color = focused ? getResources().getColor(R.color.primary_color, null)
                : getResources().getColor(R.color.border_color, null);
        field.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void shakeOtpContainer() {
        ObjectAnimator shake = ObjectAnimator.ofFloat(otpContainer, "translationX",
                0, 20, -20, 15, -15, 10, -10, 5, -5, 0);
        shake.setDuration(500);
        shake.start();
    }

    private void showSuccessAnimation(Runnable onComplete) {
        // Change Lottie to success animation
        lottieVerification.setAnimationFromUrl(
                "https://assets2.lottiefiles.com/packages/lf20_jbrw3hcz.json");
        lottieVerification.setRepeatCount(0);
        lottieVerification.playAnimation();

        // Scale animation for container
        View container = findViewById(R.id.mainContainer);
        container.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .alpha(0.8f)
                .setDuration(300)
                .withEndAction(() -> {
                    container.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(200)
                            .withEndAction(onComplete)
                            .start();
                })
                .start();
    }

    // ==================== Utility ====================

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnVerify.setEnabled(!show);
        btnVerify.setText(show ? "Verifying..." : "Verify");

        for (EditText field : otpFields) {
            field.setEnabled(!show);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lottieVerification != null && !lottieVerification.isAnimating()) {
            lottieVerification.resumeAnimation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (lottieVerification != null) {
            lottieVerification.pauseAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (otpHelper != null) {
            otpHelper.cleanup();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
