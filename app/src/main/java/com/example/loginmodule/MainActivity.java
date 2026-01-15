package com.example.loginmodule;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.loginmodule.loginAuth.AuthManager;
import com.example.loginmodule.loginAuth.UserSession;
import com.example.loginmodule.loginUi.LoginActivity;

public class MainActivity extends AppCompatActivity {

    private AuthManager authManager;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = AuthManager.getInstance();
        userSession = UserSession.getInstance(this);

        // Security Check: If not logged in, go to LoginActivity
        if (!authManager.isLoggedIn() || !userSession.isValidSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        Button btnLogout = findViewById(R.id.btnLogout);

        // Get username from session
        String username = userSession.getUserName();
        if (username.isEmpty()) {
            // Fallback to phone or email
            username = userSession.getDisplayIdentifier();
        }
        tvWelcome.setText("Hello, " + username + "!");

        btnLogout.setOnClickListener(v -> {
            authManager.signOut();
            userSession.clearSession();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}