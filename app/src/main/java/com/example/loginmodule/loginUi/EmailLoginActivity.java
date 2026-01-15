package com.example.loginmodule.loginUi;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
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
import com.example.loginmodule.loginAuth.UserSession;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class EmailLoginActivity extends AppCompatActivity {

    private LottieAnimationView lottieEmail;
    private ImageView btnBack;
    private TextView tvTitle;
    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnSubmit;
    private TextView tvToggleMode;
    private ProgressBar progressBar;
    private LinearLayout suggestionsContainer;

    private boolean isSignupMode = false;
    private ArrayList<String> detectedEmails;
    private AuthManager authManager;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_login);

        detectedEmails = getIntent().getStringArrayListExtra("detected_emails");
        initViews();
        setupListeners();
        startEntranceAnimations();
    }

    private void initViews() {
        authManager = AuthManager.getInstance();
        userSession = UserSession.getInstance(this);

        lottieEmail = findViewById(R.id.lottieEmail);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvToggleMode = findViewById(R.id.tvToggleMode);
        progressBar = findViewById(R.id.progressBar);
        suggestionsContainer = findViewById(R.id.suggestionsContainer);

        lottieEmail.setAnimationFromUrl("https://assets5.lottiefiles.com/packages/lf20_k86wxpgr.json");
        lottieEmail.playAnimation();
        updateUIForMode();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && detectedEmails != null && !detectedEmails.isEmpty() &&
                    (etEmail.getText() == null || etEmail.getText().toString().isEmpty())) {
                showEmailSuggestions();
            } else {
                hideSuggestions();
            }
        });

        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                tilEmail.setError(null);
                tilPassword.setError(null);
                tilName.setError(null);
                tilConfirmPassword.setError(null);
                hideSuggestions();
                updateSubmitButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        etEmail.addTextChangedListener(clearErrorWatcher);
        etPassword.addTextChangedListener(clearErrorWatcher);
        etName.addTextChangedListener(clearErrorWatcher);
        etConfirmPassword.addTextChangedListener(clearErrorWatcher);

        btnSubmit.setOnClickListener(v -> {
            if (isSignupMode)
                performSignup();
            else
                performLogin();
        });

        tvToggleMode.setOnClickListener(v -> toggleMode());
    }

    private void toggleMode() {
        isSignupMode = !isSignupMode;
        updateUIForMode();
    }

    private void updateUIForMode() {
        tvTitle.setText(isSignupMode ? "Create Account" : "Welcome Back");
        btnSubmit.setText(isSignupMode ? "Sign Up" : "Log In");
        tvToggleMode.setText(isSignupMode ? "Already have an account? Log In" : "Don't have an account? Sign Up");
        tilName.setVisibility(isSignupMode ? View.VISIBLE : View.GONE);
        tilConfirmPassword.setVisibility(isSignupMode ? View.VISIBLE : View.GONE);
        updateSubmitButtonState();
    }

    private void showEmailSuggestions() {
        if (detectedEmails == null || detectedEmails.isEmpty())
            return;
        suggestionsContainer.removeAllViews();
        suggestionsContainer.setVisibility(View.VISIBLE);

        for (String email : detectedEmails) {
            TextView tv = new TextView(this);
            tv.setText("✉️ " + email);
            tv.setPadding(32, 20, 32, 20);
            tv.setTextSize(15);
            tv.setOnClickListener(v -> {
                etEmail.setText(email);
                hideSuggestions();
                etPassword.requestFocus();
            });
            suggestionsContainer.addView(tv);
        }
        suggestionsContainer.animate().alpha(1f).setDuration(200).start();
    }

    private void hideSuggestions() {
        if (suggestionsContainer != null) {
            suggestionsContainer.animate().alpha(0f).setDuration(150)
                    .withEndAction(() -> suggestionsContainer.setVisibility(View.GONE)).start();
        }
    }

    private void performLogin() {
        String email = getText(etEmail), password = getText(etPassword);
        if (!validateEmail(email) || !validatePassword(password))
            return;
        showLoading(true);

        authManager.signInWithEmail(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                runOnUiThread(() -> {
                    showLoading(false);
                    userSession.createSession(user, UserSession.LOGIN_TYPE_EMAIL);
                    navigateToMain();
                });
            }

            @Override
            public void onError(String msg) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(msg);
                });
            }
        });
    }

    private void performSignup() {
        String name = getText(etName), email = getText(etEmail);
        String password = getText(etPassword), confirm = getText(etConfirmPassword);
        if (!validateName(name) || !validateEmail(email) || !validatePassword(password))
            return;
        if (!password.equals(confirm)) {
            tilConfirmPassword.setError("Passwords don't match");
            return;
        }
        showLoading(true);

        authManager.createAccountWithEmail(email, password, name, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                runOnUiThread(() -> {
                    showLoading(false);
                    userSession.createSession(user, UserSession.LOGIN_TYPE_EMAIL);
                    userSession.setUserName(name);
                    navigateToMain();
                });
            }

            @Override
            public void onError(String msg) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError(msg);
                });
            }
        });
    }

    private boolean validateName(String n) {
        if (n.isEmpty()) {
            tilName.setError("Required");
            return false;
        }
        return true;
    }

    private boolean validateEmail(String e) {
        if (e.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
            tilEmail.setError("Valid email required");
            return false;
        }
        return true;
    }

    private boolean validatePassword(String p) {
        if (p.length() < 6) {
            tilPassword.setError("Min 6 characters");
            return false;
        }
        return true;
    }

    private String getText(EditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void updateSubmitButtonState() {
        boolean valid = !getText(etEmail).isEmpty() && !getText(etPassword).isEmpty();
        if (isSignupMode)
            valid = valid && !getText(etName).isEmpty() && !getText(etConfirmPassword).isEmpty();
        btnSubmit.setEnabled(valid);
        btnSubmit.setAlpha(valid ? 1f : 0.6f);
    }

    private void startEntranceAnimations() {
        tvTitle.setAlpha(0f);
        tvTitle.animate().alpha(1f).setDuration(500).setStartDelay(200).start();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!show);
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lottieEmail != null)
            lottieEmail.resumeAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (lottieEmail != null)
            lottieEmail.pauseAnimation();
    }
}
