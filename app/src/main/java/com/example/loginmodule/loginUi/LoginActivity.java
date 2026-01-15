package com.example.loginmodule.loginUi;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.example.loginmodule.MainActivity;
import com.example.loginmodule.R;
import com.example.loginmodule.loginAuth.AccountDetector;
import com.example.loginmodule.loginAuth.AuthManager;
import com.example.loginmodule.loginAuth.UserSession;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * LoginActivity - Main Login Screen with Modern UI
 * Features: Phone OTP, Email login, Google Sign-In, Auto-detect phone/email
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // UI Components
    private LottieAnimationView lottieAnimationView;
    private EditText etPhone;
    private MaterialButton btnContinue;
    private MaterialButton btnGoogle;
    private MaterialButton btnEmail;
    private LinearLayout phoneInputLayout;
    private TextView tvTitle;
    private LinearLayout suggestionsContainer;
    private ProgressBar progressBar;

    // Auth & Detection
    private AuthManager authManager;
    private AccountDetector accountDetector;
    private UserSession userSession;
    private AccountDetector.AccountInfo detectedAccounts;

    // Google Sign-In
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // Permission launcher
    private ActivityResultLauncher<String[]> permissionLauncher;

    // Selected country code
    private String selectedCountryCode = "+91";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeComponents();
        setupGoogleSignIn();
        setupPermissionLauncher();
        checkExistingSession();
        setupUI();
        setupListeners();
        requestPermissionsIfNeeded();
        startEntranceAnimations();
    }

    private void initializeComponents() {
        authManager = AuthManager.getInstance();
        accountDetector = new AccountDetector(this);
        userSession = UserSession.getInstance(this);

        // Find views
        lottieAnimationView = findViewById(R.id.lottieAnimationView);
        etPhone = findViewById(R.id.etPhone);
        btnContinue = findViewById(R.id.btnContinue);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnEmail = findViewById(R.id.btnEmail);
        phoneInputLayout = findViewById(R.id.phoneInputLayout);
        tvTitle = findViewById(R.id.tvTitle);
        suggestionsContainer = findViewById(R.id.suggestionsContainer);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupGoogleSignIn() {
        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Setup Google Sign-In launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        showLoading(false);
                        showError("Google Sign-In cancelled");
                    }
                });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google sign-in failed: " + e.getStatusCode());
            showLoading(false);
            showError("Google Sign-In failed. Please try again.");
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            userSession.createSession(user, UserSession.LOGIN_TYPE_GOOGLE);
                            if (user.getDisplayName() != null) {
                                userSession.setUserName(user.getDisplayName());
                            }
                            if (user.getEmail() != null) {
                                userSession.setUserEmail(user.getEmail());
                            }
                            Toast.makeText(this, "Welcome, " + user.getDisplayName() + "!", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        }
                    } else {
                        showError("Authentication failed. Please try again.");
                    }
                });
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        detectSystemAccounts();
                    }
                });
    }

    private void checkExistingSession() {
        if (authManager.isLoggedIn() && userSession.isValidSession()) {
            navigateToMain();
        }
    }

    private void setupUI() {
        // Set Lottie animation - Food delivery with people theme
        lottieAnimationView.setAnimationFromUrl(
                "https://assets3.lottiefiles.com/packages/lf20_jcikwtux.json");
        lottieAnimationView.playAnimation();
    }

    private void setupListeners() {
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateContinueButtonState();
                hideSuggestions();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && detectedAccounts != null && !detectedAccounts.phoneNumbers.isEmpty()) {
                showPhoneSuggestions();
            }
        });

        btnContinue.setOnClickListener(v -> onContinueClicked());
        btnGoogle.setOnClickListener(v -> onGoogleSignInClicked());
        btnEmail.setOnClickListener(v -> onEmailLoginClicked());
    }

    private void updateContinueButtonState() {
        String phone = etPhone.getText().toString().trim();
        boolean isValid = phone.length() >= 10;
        btnContinue.setEnabled(isValid);
        btnContinue.setAlpha(isValid ? 1.0f : 0.6f);
    }

    private void requestPermissionsIfNeeded() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.GET_ACCOUNTS);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (permissionsNeeded.isEmpty()) {
            detectSystemAccounts();
        } else {
            permissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
        }
    }

    private void detectSystemAccounts() {
        detectedAccounts = accountDetector.detectAccounts();

        if (detectedAccounts.hasPhone()) {
            String localNumber = accountDetector.getLocalNumber(detectedAccounts.primaryPhone);
            if (!localNumber.isEmpty()) {
                etPhone.setHint("Detected: " + formatPhoneForDisplay(localNumber));
            }
        }
    }

    private void showPhoneSuggestions() {
        if (detectedAccounts == null || detectedAccounts.phoneNumbers.isEmpty()) {
            return;
        }

        suggestionsContainer.removeAllViews();
        suggestionsContainer.setVisibility(View.VISIBLE);

        for (String phone : detectedAccounts.phoneNumbers) {
            TextView suggestion = createSuggestionView(phone);
            suggestion.setOnClickListener(v -> {
                String localNumber = accountDetector.getLocalNumber(phone);
                etPhone.setText(localNumber);
                etPhone.setSelection(localNumber.length());
                hideSuggestions();
            });
            suggestionsContainer.addView(suggestion);
        }

        suggestionsContainer.setAlpha(0f);
        suggestionsContainer.animate().alpha(1f).setDuration(200).start();
    }

    private TextView createSuggestionView(String phone) {
        TextView tv = new TextView(this);
        tv.setText("📱 " + formatPhoneForDisplay(accountDetector.getLocalNumber(phone)));
        tv.setPadding(32, 24, 32, 24);
        tv.setTextSize(16);
        tv.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        tv.setBackground(ContextCompat.getDrawable(this, R.drawable.suggestion_item_bg));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        tv.setLayoutParams(params);

        return tv;
    }

    private void hideSuggestions() {
        if (suggestionsContainer != null && suggestionsContainer.getVisibility() == View.VISIBLE) {
            suggestionsContainer.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> suggestionsContainer.setVisibility(View.GONE))
                    .start();
        }
    }

    private String formatPhoneForDisplay(String phone) {
        if (phone == null || phone.length() < 10)
            return phone;
        return phone.substring(0, 3) + " " + phone.substring(3, 6) + " " + phone.substring(6);
    }

    private void onContinueClicked() {
        String phone = etPhone.getText().toString().trim();

        if (phone.length() < 10) {
            showError("Please enter a valid phone number");
            shakeView(phoneInputLayout);
            return;
        }

        String fullPhone = selectedCountryCode + phone;
        showLoading(true);

        Intent intent = new Intent(this, OtpVerificationActivity.class);
        intent.putExtra("phone", fullPhone);
        intent.putExtra("phone_display", formatPhoneForDisplay(phone));
        startActivity(intent);

        showLoading(false);
    }

    private void onGoogleSignInClicked() {
        pulseView(btnGoogle);
        showLoading(true);

        // Sign out first to show account picker
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void onEmailLoginClicked() {
        pulseView(btnEmail);

        Intent intent = new Intent(this, EmailLoginActivity.class);
        if (detectedAccounts != null && !detectedAccounts.emails.isEmpty()) {
            intent.putStringArrayListExtra("detected_emails", new ArrayList<>(detectedAccounts.emails));
        }
        startActivity(intent);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ==================== Animations ====================

    private void startEntranceAnimations() {
        tvTitle.setAlpha(0f);
        tvTitle.setTranslationY(-50f);
        tvTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(300)
                .setInterpolator(new OvershootInterpolator())
                .start();

        phoneInputLayout.setAlpha(0f);
        phoneInputLayout.setTranslationY(30f);
        phoneInputLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(450)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        btnContinue.setAlpha(0f);
        btnContinue.setScaleX(0.8f);
        btnContinue.setScaleY(0.8f);
        btnContinue.animate()
                .alpha(0.6f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(600)
                .setInterpolator(new OvershootInterpolator())
                .start();

        animateSocialButtons();
    }

    private void animateSocialButtons() {
        View[] socialButtons = { btnGoogle, btnEmail };

        for (int i = 0; i < socialButtons.length; i++) {
            View button = socialButtons[i];
            button.setAlpha(0f);
            button.setTranslationY(30f);
            button.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(700 + (i * 100))
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    private void shakeView(View view) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX",
                0, 15, -15, 10, -10, 5, -5, 0);
        shake.setDuration(400);
        shake.start();
    }

    private void pulseView(View view) {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1.02f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1.02f, 1f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);
        set.playTogether(scaleX, scaleY);
        set.start();
    }

    // ==================== Utility ====================

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnContinue.setEnabled(!show);
        btnGoogle.setEnabled(!show);
        btnEmail.setEnabled(!show);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lottieAnimationView != null && !lottieAnimationView.isAnimating()) {
            lottieAnimationView.resumeAnimation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (lottieAnimationView != null) {
            lottieAnimationView.pauseAnimation();
        }
    }
}
