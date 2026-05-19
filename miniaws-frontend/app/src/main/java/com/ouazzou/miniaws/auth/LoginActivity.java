package com.ouazzou.miniaws.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ouazzou.miniaws.R;
import com.ouazzou.miniaws.ui.main.MainActivity;
import com.ouazzou.miniaws.utils.Constants;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvToggleMode;

    private FirebaseAuth mAuth;

    // La variable magique qui sait si on est en mode Inscription ou Connexion
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvToggleMode = findViewById(R.id.tvToggleMode); // Notre nouveau texte !

        // Action principale du gros bouton
        btnLogin.setOnClickListener(v -> handleAuthAction());

        // Action du petit texte pour basculer de mode
        tvToggleMode.setOnClickListener(v -> toggleMode());
    }

    // 🔄 Méthode pour changer l'apparence de l'écran
    private void toggleMode() {
        isLoginMode = !isLoginMode; // On inverse le mode
        if (isLoginMode) {
            btnLogin.setText("S'IDENTIFIER");
            tvToggleMode.setText("Pas encore de compte ? S'inscrire");
        } else {
            btnLogin.setText("CRÉER UN COMPTE");
            tvToggleMode.setText("Déjà un compte ? Se connecter");
        }
    }

    // La méthode qui décide d'inscrire ou de connecter
    private void handleAuthAction() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Le mot de passe doit faire au moins 6 caractères", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        if (isLoginMode) {
            // MODE CONNEXION
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> processResult(task));
        } else {
            // MODE INSCRIPTION
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> processResult(task));
        }
    }

    // Gère le succès ou l'échec pour les deux modes
    private void processResult(com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult> task) {
        if (task.isSuccessful()) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) getSecureToken(user);
        } else {
            showLoading(false);
            String error = task.getException() != null ? task.getException().getMessage() : "Erreur inconnue";
            Toast.makeText(LoginActivity.this, "Échec : " + error, Toast.LENGTH_LONG).show();
        }
    }

    private void getSecureToken(FirebaseUser user) {
        user.getIdToken(true).addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                Constants.CURRENT_TOKEN = task.getResult().getToken();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            btnLogin.setText("");
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnLogin.setText(isLoginMode ? "S'IDENTIFIER" : "CRÉER UN COMPTE");
        }
    }
}